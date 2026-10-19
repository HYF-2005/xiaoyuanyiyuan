// 数据看板单元测试；用 Mockito 固定聚合结果，不连接 MySQL。
package com.campushelp.admin;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.campushelp.account.AccountService;
import com.campushelp.mapper.AdminDashboardMapper;
import com.campushelp.web.ApiException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;

// 类/接口职责：验证完成率、金额、趋势补零和非法时间范围。
class AdminDashboardServiceTest {
    @Test
    void summarizesCountsAndFillsTrendDays() {
        var mapper = mock(AdminDashboardMapper.class);
        when(mapper.countUsers()).thenReturn(5L);
        when(mapper.countActiveUsers()).thenReturn(4L);
        when(mapper.countUsersToday()).thenReturn(2L);
        when(mapper.countOrders()).thenReturn(4L);
        when(mapper.countOrdersToday()).thenReturn(0L);
        when(mapper.countOpenOrders()).thenReturn(0L);
        when(mapper.countCompletedOrders()).thenReturn(1L);
        when(mapper.countFeedback()).thenReturn(0L);
        when(mapper.completedAmount()).thenReturn(new BigDecimal("6.20"));
        when(mapper.countOrdersByStatus()).thenReturn(List.of());
        when(mapper.countUsersByDay(any())).thenReturn(List.of());
        when(mapper.countOrdersByDay(any())).thenReturn(List.of());
        when(mapper.recentOrders()).thenReturn(List.of());
        when(mapper.recentFeedback()).thenReturn(List.of());

        var service = new AdminDashboardService(mapper, mock(AccountService.class));
        var result = service.load(7);

        assertEquals(5L, result.summary().totalUsers());
        assertEquals(4L, result.summary().activeUsers());
        assertEquals(25.0, result.summary().completionRate());
        assertEquals(new BigDecimal("6.20"), result.summary().completedAmount());
        assertEquals(7, result.trend().size());
        assertEquals(5, result.status().size());
        assertEquals(0L, result.trend().get(6).orders());
        assertThrows(ApiException.class, () -> service.load(10));
    }

    @Test
    void includesParticipantCreditAndIncomeInRecentOrders() {
        var mapper = mock(AdminDashboardMapper.class);
        var accounts = mock(AccountService.class);
        when(mapper.completedAmount()).thenReturn(BigDecimal.ZERO);
        when(mapper.countOrdersByStatus()).thenReturn(List.of());
        when(mapper.countUsersByDay(any())).thenReturn(List.of());
        when(mapper.countOrdersByDay(any())).thenReturn(List.of());
        when(mapper.recentFeedback()).thenReturn(List.of());
        when(accounts.creditScore(10L)).thenReturn(72);
        when(accounts.creditScore(11L)).thenReturn(84);
        var row = new DashboardRow.RecentOrder();
        row.setId(9206L);
        row.setType("parcel");
        row.setTitle("高报酬代取");
        row.setStatus("已完成");
        row.setReward(new BigDecimal("60.00"));
        row.setPublisherId(10L);
        row.setPublisherName("发布者");
        row.setRunnerId(11L);
        row.setRunnerName("接单者");
        row.setCreatedAt(LocalDateTime.now());
        when(mapper.recentOrders()).thenReturn(List.of(row));

        var service = new AdminDashboardService(mapper, accounts);
        var order = service.load(7).recentOrders().get(0);

        assertEquals(72, order.publisherCredit());
        assertEquals(84, order.runnerCredit());
        assertEquals(new BigDecimal("60.00"), order.actualIncome());
        assertEquals(BigDecimal.ZERO, order.platformFee());
    }
}
