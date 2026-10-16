// 订单单元测试；检查游客隐私、条件更新冲突和发布期限/包裹校验。
package com.campushelp.order;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.campushelp.account.AccountService;
import com.campushelp.mapper.OrderMapper;
import com.campushelp.mapper.UserMapper;
import com.campushelp.model.HelpOrder;
import com.campushelp.model.User;
import com.campushelp.web.ApiException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

// 类/接口职责：订单单元测试；检查游客隐私、条件更新冲突和发布期限/包裹校验。
class OrderServiceTest {
    private OrderMapper mapper;
    private OrderService service;

    // 每条测试前执行初始化，避免不同测试共用状态。
    @BeforeEach
    // setup：每个测试前创建 Mock Mapper 和业务对象，准备有效发布者信息。
    void setup() {
        mapper = mock(OrderMapper.class);
        UserMapper users = mock(UserMapper.class);
        AccountService accounts = mock(AccountService.class);
        User publisher = new User();
        publisher.setId(1L);
        publisher.setDisplayName("发布者");
        when(users.findById(1L)).thenReturn(publisher);
        when(accounts.creditScore(1L)).thenReturn(61);
        service = new OrderService(mapper, users, new ObjectMapper(), accounts);
    }

    @Test
    // visitorCannotReadPickupCodesOrDetailedAddress：断言游客拿不到取件码和详细地址，发布者能看到。
    void visitorCannotReadPickupCodesOrDetailedAddress() {
        HelpOrder order = new HelpOrder();
        order.setId(10L);
        order.setType("parcel");
        order.setTitle("小件1个");
        order.setPackagesJson("[{\"code\":\"123456\",\"size\":\"小件\"}]");
        order.setDeliveryAddress("宿舍 101");
        order.setDeliveryBuilding("一号学生宿舍");
        order.setReward(new BigDecimal("1.00"));
        order.setPublisherId(1L);
        order.setStatus("待接单");
        when(mapper.findById(10L)).thenReturn(order);
        var view = service.detail(10L, 2L);
        assertEquals(List.of(), view.packages());
        assertEquals(null, view.deliveryAddress());
    }

    @Test
    // failedConditionalAcceptIsConflict：模拟 UPDATE 影响 0 行，断言业务返回 409。
    void failedConditionalAcceptIsConflict() {
        when(mapper.accept(10L, 2L)).thenReturn(0);
        assertEquals(409, assertThrows(ApiException.class, () -> service.accept(10L, 2L)).status().value());
    }

    @Test
    // parcelCreateRequiresFutureDeadlineAndValidPackage：断言过期截止日期和无效包裹不能发布。
    void parcelCreateRequiresFutureDeadlineAndValidPackage() {
        var invalid = new OrderService.CreateOrder("parcel", "北苑菜鸟驿站", null,
        List.of(new OrderService.PackageItem("A123", "小件")), null, "一号学生宿舍",
        "101", new BigDecimal("3.10"), LocalDate.now().minusDays(1), LocalTime.NOON);
        assertEquals(400, assertThrows(ApiException.class, () -> service.create(1L, invalid)).status().value());
        var valid = new OrderService.CreateOrder("parcel", "北苑菜鸟驿站", null,
        List.of(new OrderService.PackageItem("A123", "小件")), null, "一号学生宿舍",
        "101", new BigDecimal("3.10"), LocalDate.now().plusDays(1), LocalTime.NOON);
        when(mapper.insert(any(HelpOrder.class))).thenAnswer(invocation -> {
            HelpOrder order = invocation.getArgument(0);
            assertEquals("小件1个", order.getTitle());
            order.setId(11L);
            when(mapper.findById(11L)).thenReturn(order);
            return 1;
        });
        assertEquals(11L, service.create(1L, valid).id());
        verify(mapper).insert(any(HelpOrder.class));
    }
}
