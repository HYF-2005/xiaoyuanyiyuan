// 后台认证业务；管理员账号独立于小程序 users，token 只保存 SHA-256 哈希。
package com.campushelp.admin;

import com.campushelp.mapper.AdminMapper;
import com.campushelp.model.Admin;
import com.campushelp.web.ApiException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

@Service
// 类/接口职责：管理员登录、当前会话校验和退出；与小程序用户认证完全分离。
public class AdminAuthService {
    private static final int SESSION_HOURS = 12;
    private final AdminMapper admins;
    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
    private final SecureRandom random = new SecureRandom();

    public AdminAuthService(AdminMapper admins) {
        this.admins = admins;
    }

    public record AdminView(Long id, String username, String displayName, String role,
        List<String> permissions) {}
    public record LoginResult(String token, String expiresAt, AdminView admin) {}

    // login：账号不存在、密码错误或账号停用都返回同一句提示，避免泄露账号状态。
    public LoginResult login(String username, String password) {
        String normalized = username == null ? "" : username.trim();
        Admin admin = admins.findByUsername(normalized);
        if (admin == null || password == null || !Boolean.TRUE.equals(admin.getActive())
            || !encoder.matches(password, admin.getPasswordHash())) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "账号或密码错误");
        }

        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        String token = HexFormat.of().formatHex(bytes);
        LocalDateTime expiresAt = LocalDateTime.now().plusHours(SESSION_HOURS);
        admins.insertSession(admin.getId(), sha256(token), expiresAt);
        admins.touchLastLogin(admin.getId());
        return new LoginResult(token, expiresAt.toString(), view(admin));
    }

    // currentAdmin：校验 Bearer 格式、64 位十六进制 token、数据库会话和管理员启用状态。
    public Admin currentAdmin(String authorization) {
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "请先登录后台");
        }
        String token = authorization.substring(7).trim();
        if (!token.matches("[0-9a-f]{64}")) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "后台登录凭证无效");
        }
        Admin admin = admins.findByTokenHash(sha256(token));
        if (admin == null) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "后台登录已过期，请重新登录");
        }
        return admin;
    }

    public AdminView view(Admin admin) {
        return new AdminView(admin.getId(), admin.getUsername(), admin.getDisplayName(), admin.getRole(),
            AdminPermissions.resolve(admin));
    }

    // logout：删除当前 token 对应的后台会话；其他设备的会话不受影响。
    public void logout(String authorization) {
        if (authorization != null && authorization.startsWith("Bearer ")) {
            String token = authorization.substring(7).trim();
            if (token.matches("[0-9a-f]{64}")) {
                admins.deleteSession(sha256(token));
            }
        }
    }

    // sha256：数据库只保存哈希，即使库泄露也不能直接拿它当 Bearer token 使用。
    private String sha256(String value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                .digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (Exception error) {
            throw new IllegalStateException("无法计算 token 摘要", error);
        }
    }
}
