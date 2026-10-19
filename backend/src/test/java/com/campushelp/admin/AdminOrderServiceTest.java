// 后台订单管理单元测试；验证筛选、取消边界和强制完成凭证限制。
package com.campushelp.admin;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.campushelp.mapper.AdminOrderMapper;
import com.campushelp.web.ApiException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;

// 类/接口职责：后台订单管理业务规则，不连接数据库。
class AdminOrderServiceTest {
    @Test
    void listsOrdersWithFilters() {
        var mapper = mock(AdminOrderMapper.class);
        var service = new AdminOrderService(mapper);
        when(mapper.count("小件", "待确认", "parcel")).thenReturn(1L);
        when(mapper.page("小件", "待确认", "parcel", 0, 10)).thenReturn(List.of(row("待确认", true)));

        var page = service.list(" 小件 ", "待确认", "parcel", 1, 10);

        assertEquals(1L, page.total());
        assertEquals("待确认", page.items().get(0).status());
        verify(mapper).page("小件", "待确认", "parcel", 0, 10);
    }

    @Test
    void rejectsInvalidStatusFilter() {
        var service = new AdminOrderService(mock(AdminOrderMapper.class));

        var error = assertThrows(ApiException.class, () -> service.list("", "已发货", "all", 1, 10));
        assertEquals(400, error.status().value());
    }

    @Test
    void cannotCancelCompletedOrder() {
        var mapper = mock(AdminOrderMapper.class);
        var service = new AdminOrderService(mapper);
        when(mapper.findById(1L)).thenReturn(row("已完成", true));

        var error = assertThrows(ApiException.class, () -> service.cancel(1L));
        assertEquals(409, error.status().value());
        verify(mapper, never()).cancel(1L);
    }

    @Test
    void forceCompleteRequiresPendingOrderWithProof() {
        var mapper = mock(AdminOrderMapper.class);
        var service = new AdminOrderService(mapper);
        when(mapper.findById(2L)).thenReturn(row("待确认", false));

        var error = assertThrows(ApiException.class, () -> service.forceComplete(2L));
        assertEquals(409, error.status().value());
        verify(mapper, never()).forceComplete(2L);
    }

    @Test
    void forceCompleteUpdatesThenReturnsCompletedOrder() {
        var mapper = mock(AdminOrderMapper.class);
        var service = new AdminOrderService(mapper);
        var pending = row("待确认", true);
        var completed = row("已完成", true);
        when(mapper.findById(3L)).thenReturn(pending, completed);
        when(mapper.forceComplete(3L)).thenReturn(1);

        var detail = service.forceComplete(3L);

        assertEquals("已完成", detail.status());
        assertFalse(detail.canForceComplete());
        verify(mapper).forceComplete(3L);
    }

    private AdminOrderRow.Order row(String status, boolean hasProof) {
        var row = new AdminOrderRow.Order();
        row.setId(1L);
        row.setType("parcel");
        row.setTitle("小件1个");
        row.setStation("菜鸟驿站");
        row.setDeliveryBuilding("1号楼");
        row.setDeliveryAddress("101");
        row.setReward(new BigDecimal("5.00"));
        row.setStatus(status);
        row.setPublisherId(2L);
        row.setPublisherName("发布者");
        row.setPublisherPhone("13800000000");
        row.setRunnerId(3L);
        row.setRunnerName("接单者");
        row.setRunnerPhone("13900000000");
        row.setPackageCount(1);
        row.setCodeCount(0);
        row.setHasProof(hasProof);
        row.setCreatedAt(LocalDateTime.now());
        return row;
    }
}
