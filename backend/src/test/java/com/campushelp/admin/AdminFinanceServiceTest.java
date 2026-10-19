// 财务只读业务单元测试；验证净额计算和类型边界。
package com.campushelp.admin;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.campushelp.mapper.AdminFinanceMapper;
import com.campushelp.web.ApiException;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;

class AdminFinanceServiceTest {
    @Test
    void calculatesSummaryNetAmount() {
        var mapper = mock(AdminFinanceMapper.class);
        var service = new AdminFinanceService(mapper);
        var row = new AdminFinanceRow.Summary();
        row.setTotalOrders(4L);
        row.setCompletedOrders(1L);
        row.setCancelledOrders(3L);
        row.setPublishedAmount(new BigDecimal("19.50"));
        row.setCompletedAmount(new BigDecimal("6.20"));
        row.setRefundedAmount(new BigDecimal("13.30"));
        when(mapper.summary()).thenReturn(row);
        when(mapper.count(null, "")).thenReturn(0L);
        when(mapper.ledger(null, "", 0, 20)).thenReturn(List.of());

        var summary = service.summary();
        var page = service.ledger("all", "", 1, 20);

        assertEquals(new BigDecimal("-7.10"), summary.netAmount());
        assertEquals(4, page.summary().totalOrders());
        assertEquals(0, page.items().size());
    }

    @Test
    void rejectsUnknownLedgerKind() {
        var service = new AdminFinanceService(mock(AdminFinanceMapper.class));

        var error = assertThrows(ApiException.class, () -> service.ledger("profit", "", 1, 20));
        assertEquals(400, error.status().value());
    }

    @Test
    void rejectsOversizedPage() {
        var service = new AdminFinanceService(mock(AdminFinanceMapper.class));

        var error = assertThrows(ApiException.class, () -> service.ledger("all", "", 1, 100));
        assertEquals(400, error.status().value());
    }
}
