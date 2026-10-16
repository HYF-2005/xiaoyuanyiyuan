// 认证单元测试；用 Mockito 替代数据库，不需要连接 MySQL。
package com.campushelp.auth;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.campushelp.mapper.CodeMapper;
import com.campushelp.mapper.SessionMapper;
import com.campushelp.mapper.UserMapper;
import com.campushelp.model.User;
import com.campushelp.model.VerificationCode;
import com.campushelp.web.ApiException;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

// 类/接口职责：认证单元测试；用 Mockito 替代数据库，不需要连接 MySQL。
class AuthServiceTest {
    @Test
    // loginRejectsWrongPasswordAndIssuesRandomTokenForValidPassword：断言错误密码 401；正确密码获得随机 token，数据库收到的是 SHA-256 哈希。
    void loginRejectsWrongPasswordAndIssuesRandomTokenForValidPassword() {
        var users = mock(UserMapper.class);
        var sessions = mock(SessionMapper.class);
        var codes = mock(CodeMapper.class);
        var auth = new AuthService(users, sessions, codes, true);
        User user = new User();
        user.setId(1L);
        user.setDisplayName("测试同学");
        user.setPasswordHash(new BCryptPasswordEncoder().encode("Campus123"));
        when(users.findByUsername("student01")).thenReturn(user);
        assertEquals(401, assertThrows(ApiException.class, () -> auth.login("student01", "wrong")).status().value());
        var result = auth.login("student01", "Campus123");
        assertTrue(result.token().matches("[0-9a-f]{64}"));
        verify(sessions).insert(eq(1L), argThat(hash -> !hash.equals(result.token()) && hash.length() == 64), any());
    }

    @Test
    // phoneCodeIsConsumedOnlyOnce：断言验证码首次消费可登录，再次消费被拒绝。
    void phoneCodeIsConsumedOnlyOnce() {
        var users = mock(UserMapper.class);
        var sessions = mock(SessionMapper.class);
        var codes = mock(CodeMapper.class);
        var auth = new AuthService(users, sessions, codes, true);
        User user = new User();
        user.setId(1L);
        user.setDisplayName("测试同学");
        when(users.findByPhone("13800000000")).thenReturn(user);
        VerificationCode code = new VerificationCode();
        code.setId(1L);
        code.setCodeHash(new BCryptPasswordEncoder().encode("123456"));
        code.setExpiresAt(LocalDateTime.now().plusMinutes(5));
        code.setAttempts(0);
        when(codes.latest("13800000000", "login")).thenReturn(code);
        when(codes.recordAttempt(1L)).thenReturn(1);
        when(codes.consume(eq(1L), any())).thenReturn(1).thenReturn(0);
        assertNotNull(auth.loginPhone("13800000000", "123456").token());
        assertThrows(ApiException.class, () -> auth.loginPhone("13800000000", "123456"));
        verify(sessions, times(1)).insert(eq(1L), anyString(), any());
    }
}
