// 后台用户管理单元测试；用 Mockito 替代数据库和账户统计。
package com.campushelp.admin;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.campushelp.account.AccountService;
import com.campushelp.mapper.AdminUserMapper;
import com.campushelp.mapper.SessionMapper;
import com.campushelp.model.User;
import com.campushelp.web.ApiException;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;

// 类/接口职责：验证分页筛选、停用撤销会话、自主注销账号不能直接启用。
class AdminUserServiceTest {
    @Test
    void listsUsersWithFiltersAndCounts() {
        var users = mock(AdminUserMapper.class);
        var sessions = mock(SessionMapper.class);
        var accounts = mock(AccountService.class);
        var service = new AdminUserService(users, sessions, accounts);
        var row = row(1L, "何雨峰", true);
        when(users.count("何", null)).thenReturn(1L);
        when(users.page("何", null, 0, 10)).thenReturn(List.of(row));

        var page = service.list(" 何 ", "all", 1, 10);

        assertEquals(1L, page.total());
        assertEquals(1, page.items().size());
        assertEquals("何雨峰", page.items().get(0).displayName());
        assertEquals(61, page.items().get(0).creditBase());
        assertTrue(page.items().get(0).active());
        verify(users).page("何", null, 0, 10);
    }

    @Test
    void disablingUserRevokesAllSessions() {
        var users = mock(AdminUserMapper.class);
        var sessions = mock(SessionMapper.class);
        var accounts = mock(AccountService.class);
        var service = new AdminUserService(users, sessions, accounts);
        var row = row(2L, "测试用户", true);
        when(users.findById(2L)).thenReturn(row);

        service.updateStatus(2L, false);

        verify(users).updateActive(2L, false);
        verify(sessions).deleteByUserId(2L);
    }

    @Test
    void selfClosedUserCannotBeEnabled() {
        var users = mock(AdminUserMapper.class);
        var sessions = mock(SessionMapper.class);
        var accounts = mock(AccountService.class);
        var service = new AdminUserService(users, sessions, accounts);
        var row = row(3L, "已注销用户", false);
        when(users.findById(3L)).thenReturn(row);

        var error = assertThrows(ApiException.class, () -> service.updateStatus(3L, true));
        assertEquals(409, error.status().value());
        verify(users, never()).updateActive(anyLong(), anyBoolean());
    }

    @Test
    void detailIncludesComputedCredit() {
        var users = mock(AdminUserMapper.class);
        var sessions = mock(SessionMapper.class);
        var accounts = mock(AccountService.class);
        var service = new AdminUserService(users, sessions, accounts);
        var row = row(4L, "信用用户", true);
        when(users.findById(4L)).thenReturn(row);
        when(users.recentOrders(4L)).thenReturn(List.of());
        when(accounts.credit(any(User.class)))
            .thenReturn(new AccountService.CreditSummary(63, List.of()));

        var detail = service.detail(4L);

        assertEquals(63, detail.creditScore());
        assertEquals(1L, detail.publishedOrders());
        assertEquals(0L, detail.openOrders());
    }

    private AdminUserRow.User row(Long id, String displayName, boolean active) {
        var row = new AdminUserRow.User();
        row.setId(id);
        row.setUsername("user" + id);
        row.setDisplayName(displayName);
        row.setPhone("13800000000");
        row.setCreditBase(61);
        row.setActive(active);
        row.setCreatedAt(LocalDateTime.now());
        row.setPublishedOrders(1L);
        row.setAcceptedOrders(0L);
        row.setOpenOrders(0L);
        return row;
    }
}
