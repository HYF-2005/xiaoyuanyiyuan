// 注销单元测试；验证未完成订单/错误密码会阻止停用与清会话。
package com.campushelp.account;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.campushelp.mapper.OrderMapper;
import com.campushelp.mapper.SessionMapper;
import com.campushelp.mapper.UserMapper;
import com.campushelp.model.User;
import com.campushelp.web.ApiException;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

// 类/接口职责：注销单元测试；验证未完成订单/错误密码会阻止停用与清会话。
class AccountClosureServiceTest {
    // JUnit 测试用例，不是对外 API；assert 检查结果，Mockito mock 模拟依赖。
    @Test
    // refusesClosureWhileOrdersAreOpen：存在未完成订单时拒绝注销，并检查没有停用用户或删除会话。
    void refusesClosureWhileOrdersAreOpen() {
        var users = mock(UserMapper.class);
        var orders = mock(OrderMapper.class);
        var sessions = mock(SessionMapper.class);
        User user = new User();
        user.setPasswordHash(new BCryptPasswordEncoder().encode("Campus123"));
        when(users.findById(1L)).thenReturn(user);
        when(orders.countOpenForUser(1L)).thenReturn(1);
        var service = new AccountClosureService(users, orders, sessions);
        assertEquals(409, assertThrows(ApiException.class, () -> service.deactivate(1L, "Campus123")).status().value());
        verify(users, never()).deactivate(1L);
        verify(sessions, never()).deleteByUserId(1L);
    }

    @Test
    // deactivatesOnlyAfterPasswordCheck：错误密码不写数据库，正确密码才停用账号并清除会话。
    void deactivatesOnlyAfterPasswordCheck() {
        var users = mock(UserMapper.class);
        var orders = mock(OrderMapper.class);
        var sessions = mock(SessionMapper.class);
        User user = new User();
        user.setPasswordHash(new BCryptPasswordEncoder().encode("Campus123"));
        when(users.findById(2L)).thenReturn(user);
        when(users.deactivate(2L)).thenReturn(1);
        var service = new AccountClosureService(users, orders, sessions);
        assertEquals(400, assertThrows(ApiException.class, () -> service.deactivate(2L, "wrong")).status().value());
        verify(users, never()).deactivate(2L);
        service.deactivate(2L, "Campus123");
        verify(users).deactivate(2L);
        verify(sessions).deleteByUserId(2L);
    }
}
