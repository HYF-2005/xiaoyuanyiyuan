// 认证业务层；用 BCrypt 校验密码/验证码，随机 token 建立数据库会话；演示验证码没有接短信平台。
package com.campushelp.auth;

import com.campushelp.mapper.CodeMapper;
import com.campushelp.mapper.SessionMapper;
import com.campushelp.mapper.UserMapper;
import com.campushelp.model.User;
import com.campushelp.model.VerificationCode;
import com.campushelp.web.ApiException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

@Service
// 类/接口职责：认证业务层；用 BCrypt 校验密码/验证码，随机 token 建立数据库会话；演示验证码没有接短信平台。
public class AuthService {
    private final UserMapper users;
    private final SessionMapper sessions;
    // codes：验证码数据库接口；final 引用在构造后不再换成别的对象。
    private final CodeMapper codes;
    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
    // random：安全随机数发生器，生成验证码/token；final 引用在构造后不再换成别的对象。
    private final SecureRandom random = new SecureRandom();
    // demoCodesEnabled：配置中的演示码开关，上线短信未配置时不能继续发演示码；final 引用在构造后不再换成别的对象。
    private final boolean demoCodesEnabled;

    // AuthService：构造器由 Spring 注入所需依赖（测试中可手动创建）；不在每次请求中重复 new 数据库工具。
    public AuthService(UserMapper users, SessionMapper sessions, CodeMapper codes,
    @Value("${app.demo-codes-enabled}") boolean demoCodesEnabled) {
        this.users = users;
        this.sessions = sessions;
        this.codes = codes;
        this.demoCodesEnabled = demoCodesEnabled;
    }

    public record LoginResult(String token, String expiresAt, Long userId, String displayName) {}

    // register：清理输入并校验账号/密码/姓名；可选手机号需 bind 验证码；存 BCrypt 密码哈希并签发会话。
    public LoginResult register(String username, String password, String displayName, String phone, String code) {
        username = username == null ? "" : username.trim();
        displayName = displayName == null ? "" : displayName.trim();
        if (!username.matches("[A-Za-z0-9_-]{4,64}")) throw bad("账号需为 4–64 位字母、数字、下划线或横线");
        validatePassword(password);
        if (displayName.isBlank() || displayName.length() > 64) throw bad("姓名不能为空且不能超过 64 字");
        if (phone != null && !phone.isBlank() && !phone.matches("1[3-9]\\d{9}")) throw bad("手机号格式错误");
        if (phone != null && !phone.isBlank()) verifyCode(phone, "bind", code);
        User user = new User();
        user.setUsername(username);
        user.setPasswordHash(encoder.encode(password));
        user.setDisplayName(displayName);
        user.setPhone(phone == null || phone.isBlank() ? null : phone);
        users.insert(user);
        return issue(user);
    }

    // login：按账号查有效用户，用 BCrypt.matches 校验密码；失败返回 401，成功签发 token。
    public LoginResult login(String username, String password) {
        User user = users.findByUsername(username == null ? "" : username.trim());
        if (user == null || password == null || !encoder.matches(password, user.getPasswordHash())) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "账号或密码错误");
        }
        return issue(user);
    }

    // loginPhone：只允许已经绑定的手机号登录；验证 login 用途单次码后签发 token。
    public LoginResult loginPhone(String phone, String code) {
        User user = users.findByPhone(phone);
        if (user == null) throw new ApiException(HttpStatus.UNAUTHORIZED, "手机号未绑定账号");
        verifyCode(phone, "login", code);
        return issue(user);
    }

    public Map<String,
    String> sendCode(String phone, String purpose) {
        if (!demoCodesEnabled) throw new ApiException(HttpStatus.NOT_IMPLEMENTED, "尚未配置短信服务");
        if (phone == null || !phone.matches("1[3-9]\\d{9}")) throw bad("手机号格式错误");
        if (purpose == null || !purpose.matches("login|reset|bind")) throw bad("验证码用途错误");
        if (!"bind".equals(purpose) && users.findByPhone(phone) == null) throw bad("手机号未绑定账号");
        if ("bind".equals(purpose) && users.findByPhone(phone) != null) throw bad("手机号已被使用");
        VerificationCode latest = codes.latest(phone, purpose);
        if (latest != null && latest.getCreatedAt().isAfter(LocalDateTime.now().minusSeconds(60))) {
            throw new ApiException(HttpStatus.TOO_MANY_REQUESTS, "请 60 秒后再获取验证码");
        }
        String code = String.format("%06d", random.nextInt(1_000_000));
        VerificationCode item = new VerificationCode();
        item.setPhone(phone);
        item.setPurpose(purpose);
        item.setCodeHash(encoder.encode(code));
        item.setExpiresAt(LocalDateTime.now().plusMinutes(5));
        codes.insert(item);
        return Map.of("message", "本地演示验证码，未发送短信", "demoCode", code);
    }

    // resetPassword：验证账号和绑定号码一致，验证 reset 码；修改密码哈希后删除该用户全部会话。
    public void resetPassword(String username, String phone, String code, String newPassword) {
        User user = users.findByUsername(username == null ? "" : username.trim());
        if (user == null || phone == null || !phone.equals(user.getPhone())) throw bad("账号与手机号不匹配");
        validatePassword(newPassword);
        verifyCode(phone, "reset", code);
        users.updatePassword(user.getId(), encoder.encode(newPassword));
        sessions.deleteByUserId(user.getId());
    }

    // changePassword：验证旧密码后保存新密码哈希，并使该用户旧 token 全部失效。
    public void changePassword(Long userId, String oldPassword, String newPassword) {
        User user = users.findById(userId);
        if (user == null || oldPassword == null || !encoder.matches(oldPassword, user.getPasswordHash())) throw bad("旧密码错误");
        validatePassword(newPassword);
        users.updatePassword(userId, encoder.encode(newPassword));
        sessions.deleteByUserId(userId);
    }

    // bindPhone：验证号码可用和 bind 用途验证码，再修改 users.phone。
    public void bindPhone(Long userId, String phone, String code) {
        if (phone == null || !phone.matches("1[3-9]\\d{9}")) throw bad("手机号格式错误");
        if (users.findByPhone(phone) != null) throw bad("手机号已被使用");
        verifyCode(phone, "bind", code);
        users.updatePhone(userId, phone);
    }

    // currentUser：检查 Bearer 格式与 64 位十六进制令牌；对 token 做 SHA-256 后查询有效会话及未注销用户。
    public User currentUser(String authorization) {
        if (authorization == null || !authorization.startsWith("Bearer ")) throw new ApiException(HttpStatus.UNAUTHORIZED, "请先登录");
        String token = authorization.substring(7).trim();
        if (!token.matches("[0-9a-f]{64}")) throw new ApiException(HttpStatus.UNAUTHORIZED, "登录凭证无效");
        User user = sessions.findUserByHash(sha256(token));
        if (user == null) throw new ApiException(HttpStatus.UNAUTHORIZED, "登录已过期，请重新登录");
        return user;
    }

    // logout：删除本次 Authorization 对应的数据库会话；其他设备会话不在此处删除。
    public void logout(String authorization) {
        if (authorization != null && authorization.startsWith("Bearer ")) sessions.deleteByHash(sha256(authorization.substring(7).trim()));
    }

    // verifyCode：检查最新验证码用途、格式、期限、已用与次数；先条件记录尝试，再 BCrypt 校验，条件标记已使用确保单次消费。
    private void verifyCode(String phone, String purpose, String code) {
        if (phone == null || code == null || !code.matches("\\d{6}")) throw bad("验证码格式错误");
        VerificationCode latest = codes.latest(phone, purpose);
        if (latest == null || latest.getConsumedAt() != null || latest.getAttempts() >= 5
        || !latest.getExpiresAt().isAfter(LocalDateTime.now())) throw bad("验证码已过期，请重新获取");
        if (codes.recordAttempt(latest.getId()) != 1) throw bad("验证码尝试次数过多，请重新获取");
        if (!encoder.matches(code, latest.getCodeHash())) throw bad("验证码错误");
        if (codes.consume(latest.getId(), LocalDateTime.now()) != 1) throw bad("验证码已使用，请重新获取");
    }

    // issue：生成 32 随机字节并编码成 64 位 token；客户端收到原 token，数据库保存 SHA-256 哈希，期限 7 天。
    private LoginResult issue(User user) {
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        String token = HexFormat.of().formatHex(bytes);
        LocalDateTime expiry = LocalDateTime.now().plusDays(7);
        sessions.insert(user.getId(), sha256(token), expiry);
        return new LoginResult(token, expiry.toString(), user.getId(), user.getDisplayName());
    }

    // sha256：对随机 token 做 SHA-256；密码/验证码使用 BCrypt，两种哈希用途不同。
    private static String sha256(String value) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception error) {
            throw new IllegalStateException("SHA-256 不可用", error);
        }
    }

    // validatePassword：后端要求 8–72 位且有字母和数字；部分前端密码页更严格为 8–10 位，不能只依赖前端检查。
    private static void validatePassword(String password) {
        if (password == null || !password.matches("(?=.*[A-Za-z])(?=.*\\d).{8,72}")) throw bad("密码至少 8 位，且包含字母和数字");
    }

    // bad：创建 400 业务异常，交给 ApiErrorHandler 转成前端能读的 message。
    private static ApiException bad(String message) {
        return new ApiException(HttpStatus.BAD_REQUEST, message);
    }
}
