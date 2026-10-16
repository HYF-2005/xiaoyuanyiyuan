// 账户统计单元测试；验证完成/取消记录只推导一次信用分和收入。
package com.campushelp.account;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.campushelp.mapper.OrderMapper;
import com.campushelp.mapper.UserMapper;
import com.campushelp.model.HelpOrder;
import com.campushelp.model.User;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;

// 类/接口职责：账户统计单元测试；验证完成/取消记录只推导一次信用分和收入。
class AccountServiceTest {
    @Test
    // completedAndCancelledOrdersUpdateCreditAndIncomeOnce：用完成与取消订单验证信用分及完成收入重算，不因重复读取重复累计。
    void completedAndCancelledOrdersUpdateCreditAndIncomeOnce() {
        OrderMapper orders = mock(OrderMapper.class);
        UserMapper users = mock(UserMapper.class);
        AccountService service = new AccountService(orders, users);
        User user = new User();
        user.setId(2L);
        user.setCreditBase(61);
        user.setUsername("runner");
        user.setDisplayName("接单者");
        when(users.findById(2L)).thenReturn(user);

        HelpOrder complete = new HelpOrder();
        complete.setId(1L);
        complete.setType("parcel");
        complete.setTitle("小件1个");
        complete.setReward(new BigDecimal("3.10"));
        complete.setStatus("已完成");
        complete.setDeadlineAt(LocalDateTime.now().plusHours(1));
        complete.setSubmittedAt(LocalDateTime.now());
        complete.setCompletedAt(LocalDateTime.now());

        HelpOrder cancelled = new HelpOrder();
        cancelled.setId(2L);
        cancelled.setType("meal");
        cancelled.setTitle("1份餐");
        cancelled.setStatus("已取消");
        cancelled.setCancelledBy(2L);
        cancelled.setAcceptedAt(LocalDateTime.now().minusMinutes(10));
        cancelled.setCancelledAt(LocalDateTime.now());

        when(orders.byRunner(2L)).thenReturn(List.of(complete, cancelled));
        when(orders.byPublisher(2L)).thenReturn(List.of());
        assertEquals(58, service.creditScore(2L));
        assertEquals(new BigDecimal("3.10"), service.summary(2L).incomeTotal());
    }
}
