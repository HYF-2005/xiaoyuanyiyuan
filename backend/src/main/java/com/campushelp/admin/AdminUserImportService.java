// 后台批量导入用户：读取学生模板、逐行校验、生成 BCrypt 密码并返回可展示的失败明细。
package com.campushelp.admin;

import com.campushelp.admin.XlsxSheetReader.SheetRow;
import com.campushelp.mapper.UserMapper;
import com.campushelp.model.User;
import com.campushelp.web.ApiException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
// 类/接口职责：学生用户 Excel 批量导入；身份证号只用于生成初始密码，不写入数据库。
public class AdminUserImportService {
    private static final Logger log = LoggerFactory.getLogger(AdminUserImportService.class);
    private static final long MAX_FILE_BYTES = 10L * 1024 * 1024;
    private static final int MAX_ROWS = 1000;
    private static final int DEFAULT_CREDIT = 61;
    private static final List<String> REQUIRED_COLUMNS = List.of("学号", "姓名", "身份证号码");
    private static final Map<String, Integer> MAX_LENGTHS = Map.of(
        "学院", 64, "专业", 64, "年级", 32, "班级", 64, "宿舍", 64
    );

    private final UserMapper users;
    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    public AdminUserImportService(UserMapper users) {
        this.users = users;
    }

    public record RowError(int row, String studentNo, String message) {}
    public record ImportResult(int total, int success, int failed, List<RowError> errors) {}

    // importFile：只接受 xlsx；先限制大小，再交给无依赖读取器解析第一张工作表。
    public ImportResult importFile(MultipartFile file) {
        if (file == null || file.isEmpty()) throw bad("请选择要导入的 Excel 文件");
        if (file.getSize() > MAX_FILE_BYTES) throw bad("Excel 文件不能超过 10MB");
        String filename = file.getOriginalFilename() == null ? "" : file.getOriginalFilename().toLowerCase();
        if (!filename.endsWith(".xlsx")) throw bad("仅支持 .xlsx 格式，请下载模板填写后上传");
        try (InputStream input = file.getInputStream()) {
            return importRows(XlsxSheetReader.read(input));
        } catch (IllegalArgumentException error) {
            throw bad(error.getMessage() == null ? "Excel 内容不符合模板" : error.getMessage());
        } catch (IOException error) {
            throw bad("Excel 文件无法读取，请确认文件未损坏");
        }
    }

    // importRows：包级可见，便于不依赖真实文件测试校验和计数逻辑。
    ImportResult importRows(List<SheetRow> sheetRows) {
        if (sheetRows == null || sheetRows.isEmpty()) throw bad("Excel 中没有可导入的数据");
        Map<String, Integer> headers = headerMap(sheetRows.get(0));
        List<String> missing = REQUIRED_COLUMNS.stream().filter(name -> !headers.containsKey(name)).toList();
        if (!missing.isEmpty()) throw bad("模板缺少必填列：" + String.join("、", missing));

        List<SheetRow> dataRows = sheetRows.stream().skip(1)
            .filter(row -> row.cells().values().stream().anyMatch(value -> !value.isBlank()))
            .toList();
        if (dataRows.isEmpty()) throw bad("Excel 中没有可导入的数据");
        if (dataRows.size() > MAX_ROWS) throw bad("单次最多导入 " + MAX_ROWS + " 条数据");

        Set<String> seenStudents = new HashSet<>();
        Set<String> seenPhones = new HashSet<>();
        List<RowError> errors = new ArrayList<>();
        int success = 0;
        for (SheetRow row : dataRows) {
            String studentNo = value(row, headers, "学号");
            try {
                ImportedUser imported = validate(row, headers);
                if (!seenStudents.add(imported.username())) {
                    throw new IllegalArgumentException("文件内学号重复");
                }
                if (imported.phone() != null && !seenPhones.add(imported.phone())) {
                    throw new IllegalArgumentException("文件内手机号重复");
                }
                if (users.findAnyByUsername(imported.username()) != null) {
                    throw new IllegalArgumentException("学号已存在");
                }
                if (imported.phone() != null && users.findAnyByPhone(imported.phone()) != null) {
                    throw new IllegalArgumentException("手机号已被其他用户使用");
                }
                User user = imported.toUser();
                user.setPasswordHash(encoder.encode(imported.password()));
                user.setActive(true);
                users.insertImported(user);
                success++;
            } catch (IllegalArgumentException error) {
                errors.add(new RowError(row.rowNumber(), studentNo, error.getMessage()));
            } catch (DataIntegrityViolationException error) {
                log.warn("批量导入用户写入失败，学号={}", studentNo, error);
                errors.add(new RowError(row.rowNumber(), studentNo, "账号或手机号已存在"));
            }
        }
        return new ImportResult(dataRows.size(), success, errors.size(), List.copyOf(errors));
    }

    private ImportedUser validate(SheetRow row, Map<String, Integer> headers) {
        String username = required(row, headers, "学号");
        if (!username.matches("[A-Za-z0-9_-]{4,64}")) {
            throw new IllegalArgumentException("学号需为 4–64 位字母、数字、下划线或横线");
        }
        String displayName = required(row, headers, "姓名");
        if (displayName.length() > 64) throw new IllegalArgumentException("姓名不能超过 64 字");

        String idCard = required(row, headers, "身份证号码").replaceAll("\\s+", "");
        if (!idCard.matches("\\d{17}[\\dXx]")) {
            throw new IllegalArgumentException("身份证号码需为 18 位");
        }
        String password = idCard.substring(idCard.length() - 6);

        String phone = value(row, headers, "手机号");
        if (!phone.isBlank() && !phone.matches("1[3-9]\\d{9}")) {
            throw new IllegalArgumentException("手机号格式错误");
        }

        Map<String, String> profile = new HashMap<>();
        for (Map.Entry<String, Integer> entry : MAX_LENGTHS.entrySet()) {
            String value = value(row, headers, entry.getKey());
            if (value.length() > entry.getValue()) {
                throw new IllegalArgumentException(entry.getKey() + "不能超过 " + entry.getValue() + " 字");
            }
            profile.put(entry.getKey(), value.isBlank() ? null : value);
        }

        int credit = DEFAULT_CREDIT;
        String creditText = value(row, headers, "信用分");
        if (!creditText.isBlank()) {
            try {
                credit = Integer.parseInt(creditText);
            } catch (NumberFormatException error) {
                throw new IllegalArgumentException("信用分需为整数");
            }
            if (credit < 0 || credit > 100) throw new IllegalArgumentException("信用分需在 0–100 之间");
        }

        return new ImportedUser(
            username,
            displayName,
            phone.isBlank() ? null : phone,
            profile.get("学院"),
            profile.get("专业"),
            profile.get("年级"),
            profile.get("班级"),
            profile.get("宿舍"),
            credit,
            password
        );
    }

    private Map<String, Integer> headerMap(SheetRow headerRow) {
        Map<String, Integer> headers = new HashMap<>();
        for (Map.Entry<Integer, String> entry : headerRow.cells().entrySet()) {
            String name = entry.getValue().replace("*", "").replace(" ", "").trim();
            if (!name.isEmpty()) headers.putIfAbsent(name, entry.getKey());
        }
        return headers;
    }

    private String required(SheetRow row, Map<String, Integer> headers, String name) {
        String value = value(row, headers, name);
        if (value.isBlank()) throw new IllegalArgumentException(name + "不能为空");
        return value;
    }

    private String value(SheetRow row, Map<String, Integer> headers, String name) {
        Integer column = headers.get(name);
        return column == null ? "" : row.value(column).trim();
    }

    private ApiException bad(String message) {
        return new ApiException(HttpStatus.BAD_REQUEST, message);
    }

    private record ImportedUser(
        String username,
        String displayName,
        String phone,
        String college,
        String major,
        String grade,
        String className,
        String dormitory,
        int credit,
        String password
    ) {
        User toUser() {
            User user = new User();
            user.setUsername(username);
            user.setDisplayName(displayName);
            user.setPhone(phone);
            user.setCollege(college);
            user.setMajor(major);
            user.setGrade(grade);
            user.setClassName(className);
            user.setDormitory(dormitory);
            user.setCreditBase(credit);
            return user;
        }
    }
}
