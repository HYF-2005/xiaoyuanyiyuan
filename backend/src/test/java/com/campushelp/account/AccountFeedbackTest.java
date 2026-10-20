// 订单问题反馈单元测试；验证只能关联本人参与的订单，且普通反馈不写 order_id。
package com.campushelp.account;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.campushelp.auth.AuthInterceptor;
import com.campushelp.mapper.FeedbackMapper;
import com.campushelp.mapper.OrderMapper;
import com.campushelp.model.HelpOrder;
import com.campushelp.model.User;
import com.campushelp.web.ApiException;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

// 类/接口职责：验证反馈接口对 orderId 的鉴权与写入行为。
class AccountFeedbackTest {
    // 构造一个已通过登录拦截器的请求，用户 id 由拦截器写入属性，前端无法伪造。
    private MockHttpServletRequest requestFor(long userId) {
        var request = new MockHttpServletRequest();
        var user = new User();
        user.setId(userId);
        request.setAttribute(AuthInterceptor.USER_ATTRIBUTE, user);
        return request;
    }

    private HelpOrder orderOf(long publisherId, Long runnerId) {
        var order = new HelpOrder();
        order.setPublisherId(publisherId);
        order.setRunnerId(runnerId);
        return order;
    }

    private AccountController controller(FeedbackMapper feedback, OrderMapper orders) {
        return new AccountController(mock(AccountService.class), feedback, orders,
            mock(AccountClosureService.class));
    }

    // JUnit 测试用例，不是对外 API；assert 检查结果，Mockito mock 模拟依赖。
    @Test
    // rejectsFeedbackForOrderTheUserDidNotJoin：既不发布也不接单的用户反馈他人订单，应被拒绝且不写库。
    void rejectsFeedbackForOrderTheUserDidNotJoin() {
        var feedback = mock(FeedbackMapper.class);
        var orders = mock(OrderMapper.class);
        when(orders.findById(9L)).thenReturn(orderOf(2L, 3L));

        var ex = assertThrows(ApiException.class, () -> controller(feedback, orders).feedback(
            requestFor(1L), new AccountController.FeedbackRequest("这单一直没有送到，需要处理", 9L)));

        assertEquals(400, ex.status().value());
        verify(feedback, never()).insert(anyLong(), nullable(Long.class), anyString());
    }

    @Test
    // publisherCanAttachOrderId：发布者提交反馈时，orderId 作为独立字段写入。
    void publisherCanAttachOrderId() {
        var feedback = mock(FeedbackMapper.class);
        var orders = mock(OrderMapper.class);
        when(orders.findById(9L)).thenReturn(orderOf(1L, 3L));

        controller(feedback, orders).feedback(
            requestFor(1L), new AccountController.FeedbackRequest("这单一直没有送到，需要处理", 9L));

        verify(feedback).insert(eq(1L), eq(9L), eq("这单一直没有送到，需要处理"));
    }

    @Test
    // runnerCanAttachOrderId：接单者同样可以针对该订单提交反馈。
    void runnerCanAttachOrderId() {
        var feedback = mock(FeedbackMapper.class);
        var orders = mock(OrderMapper.class);
        when(orders.findById(9L)).thenReturn(orderOf(3L, 1L));

        controller(feedback, orders).feedback(
            requestFor(1L), new AccountController.FeedbackRequest("取件码有误，联系不到发布者", 9L));

        verify(feedback).insert(eq(1L), eq(9L), eq("取件码有误，联系不到发布者"));
    }

    @Test
    // generalFeedbackSkipsOrderId：不带 orderId 的普通反馈按 null 写入，不做订单校验。
    void generalFeedbackSkipsOrderId() {
        var feedback = mock(FeedbackMapper.class);
        var orders = mock(OrderMapper.class);

        controller(feedback, orders).feedback(
            requestFor(1L), new AccountController.FeedbackRequest("希望增加夜间时段的配送", null));

        verify(feedback).insert(eq(1L), isNull(), eq("希望增加夜间时段的配送"));
        verify(orders, never()).findById(anyLong());
    }
}
