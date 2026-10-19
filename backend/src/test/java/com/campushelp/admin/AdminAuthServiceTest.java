// 后台认证单元测试；用 Mockito 替代数据库，不连接 MySQL。
package com.campushelp.admin;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.campushelp.mapper.AdminMapper;
import com.campushelp.model.Admin;
import com.campushelp.web.ApiException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

// 类/接口职责：验证后台登录失败提示、token 哈希存储和会话校验。
class AdminAuthServiceTest {
    @Test
    // loginRejectsWrongPasswordAndStoresHashedSession：错误密码 401；正确密码拿到随机 token，数据库只收到哈希。
    void loginRejectsWrongPasswordAndStoresHashedSession() {
        var admins = mock(AdminMapper.class);
        var auth = new AdminAuthService(admins);
        Admin admin = new Admin();
        admin.setId(7L);
        admin.setUsername("admin");
        admin.setDisplayName("超级管理员");
        admin.setRole("super_admin");
        admin.setActive(true);
        admin.setPasswordHash(new BCryptPasswordEncoder().encode("Admin@123456"));
        when(admins.findByUsername("admin")).thenReturn(admin);

        assertEquals(401, assertThrows(ApiException.class,
            () -> auth.login("admin", "wrong-password")).status().value());

        var result = auth.login("admin", "Admin@123456");
        assertTrue(result.token().matches("[0-9a-f]{64}"));
        assertEquals("admin", result.admin().username());
        assertEquals(AdminPermissions.ALL, result.admin().permissions());
        verify(admins).insertSession(eq(7L), argThat(hash -> !hash.equals(result.token()) && hash.length() == 64), any());
        verify(admins).touchLastLogin(7L);
    }

    @Test
    // validTokenIsResolvedByHashAndDisabledAccountsCannotUseSession：当前会话按哈希查管理员；停用账号不能登录。
    void validTokenIsResolvedByHashAndDisabledAccountsCannotUseSession() throws Exception {
        var admins = mock(AdminMapper.class);
        var auth = new AdminAuthService(admins);
        Admin admin = new Admin();
        admin.setId(8L);
        admin.setUsername("operator");
        admin.setActive(true);
        String token = "a".repeat(64);
        String hash = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
            .digest(token.getBytes(StandardCharsets.UTF_8)));
        when(admins.findByTokenHash(hash)).thenReturn(admin);
        assertEquals(8L, auth.currentAdmin("Bearer " + token).getId());

        Admin disabled = new Admin();
        disabled.setUsername("disabled");
        disabled.setActive(false);
        disabled.setPasswordHash(new BCryptPasswordEncoder().encode("Admin@123456"));
        when(admins.findByUsername("disabled")).thenReturn(disabled);
        assertThrows(ApiException.class, () -> auth.login("disabled", "Admin@123456"));
        assertThrows(ApiException.class, () -> auth.currentAdmin("not-a-token"));
    }
}
