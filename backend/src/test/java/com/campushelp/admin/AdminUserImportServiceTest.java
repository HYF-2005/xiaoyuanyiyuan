// 批量导入用户测试：解析模板、字段落库和逐行失败明细。
package com.campushelp.admin;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.campushelp.admin.XlsxSheetReader.SheetRow;
import com.campushelp.mapper.UserMapper;
import com.campushelp.model.User;
import com.campushelp.web.ApiException;
import java.io.InputStream;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

// 类/接口职责：验证 xlsx 读取、必填校验、文件内去重、默认密码和成功/失败计数。
class AdminUserImportServiceTest {
    @Test
    void readsFirstWorksheetFromXlsx() throws Exception {
        try (InputStream input = getClass().getResourceAsStream("/user-import-test.xlsx")) {
            assertNotNull(input);
            List<SheetRow> rows = XlsxSheetReader.read(input);
            assertEquals(4, rows.size());
            assertEquals("学号", rows.get(0).value(0).replace("*", ""));
            assertEquals("20260001", rows.get(1).value(0));
            assertEquals("110101200001011234", rows.get(1).value(3));
            assertEquals("70", rows.get(1).value(9));
        }
    }

    @Test
    void importsValidRowsAndReturnsRowErrors() {
        UserMapper users = mock(UserMapper.class);
        AdminUserImportService service = new AdminUserImportService(users);
        List<SheetRow> rows = List.of(
            row(1, "学号*", "姓名*", "手机号", "身份证号码*", "学院", "专业", "年级", "班级", "宿舍", "信用分"),
            row(2, "20260001", "张三", "13800000001", "110101200001011234", "计算机学院", "软件工程", "2026", "1班", "A101", "70"),
            row(3, "20260002", "", "13800000002", "110101200001011235", "计算机学院", "软件工程", "2026", "1班", "A102", "61"),
            row(4, "20260003", "王五", "123", "110101200001011236", "计算机学院", "软件工程", "2026", "1班", "A103", "61")
        );

        var result = service.importRows(rows);

        assertEquals(3, result.total());
        assertEquals(1, result.success());
        assertEquals(2, result.failed());
        assertEquals(3, result.errors().get(0).row());
        assertEquals("姓名不能为空", result.errors().get(0).message());
        assertEquals(4, result.errors().get(1).row());
        assertEquals("手机号格式错误", result.errors().get(1).message());

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(users).insertImported(captor.capture());
        User saved = captor.getValue();
        assertEquals("20260001", saved.getUsername());
        assertEquals("张三", saved.getDisplayName());
        assertEquals("计算机学院", saved.getCollege());
        assertEquals("软件工程", saved.getMajor());
        assertEquals("2026", saved.getGrade());
        assertEquals("1班", saved.getClassName());
        assertEquals("A101", saved.getDormitory());
        assertEquals(70, saved.getCreditBase());
        assertTrue(new BCryptPasswordEncoder().matches("011234", saved.getPasswordHash()));
    }

    @Test
    void rejectsDuplicateStudentInsideFileAndExistingAccount() {
        UserMapper users = mock(UserMapper.class);
        AdminUserImportService service = new AdminUserImportService(users);
        User existing = new User();
        existing.setUsername("20260002");
        when(users.findAnyByUsername("20260002")).thenReturn(existing);
        List<SheetRow> rows = List.of(
            row(1, "学号*", "姓名*", "手机号", "身份证号码*", "学院", "专业", "年级", "班级", "宿舍", "信用分"),
            row(2, "20260001", "张三", "13800000001", "110101200001011234", "", "", "", "", "", ""),
            row(3, "20260001", "重复", "13800000003", "110101200001011235", "", "", "", "", "", ""),
            row(4, "20260002", "已存在", "13800000004", "110101200001011236", "", "", "", "", "", "")
        );

        var result = service.importRows(rows);

        assertEquals(2, result.failed());
        assertEquals("文件内学号重复", result.errors().get(0).message());
        assertEquals("学号已存在", result.errors().get(1).message());
        verify(users, times(1)).insertImported(any(User.class));
    }

    @Test
    void rejectsFileWithMissingRequiredColumn() {
        UserMapper users = mock(UserMapper.class);
        AdminUserImportService service = new AdminUserImportService(users);
        List<SheetRow> rows = List.of(row(1, "学号*", "姓名*"));

        ApiException error = assertThrows(ApiException.class, () -> service.importRows(rows));

        assertEquals(400, error.status().value());
        assertTrue(error.getMessage().contains("身份证号码"));
        verifyNoInteractions(users);
    }

    private SheetRow row(int number, String... values) {
        Map<Integer, String> cells = new LinkedHashMap<>();
        for (int i = 0; i < values.length; i++) {
            if (!values[i].isEmpty()) cells.put(i, values[i]);
        }
        return new SheetRow(number, cells);
    }
}
