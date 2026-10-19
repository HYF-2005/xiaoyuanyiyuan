// 财务只读视图的数据库行对象；金额来自订单，不代表真实支付。
package com.campushelp.admin;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;

public final class AdminFinanceRow {
    private AdminFinanceRow() {}

    @Data
    public static class Summary {
        private Long totalOrders;
        private Long completedOrders;
        private Long cancelledOrders;
        private BigDecimal publishedAmount;
        private BigDecimal completedAmount;
        private BigDecimal refundedAmount;
    }

    @Data
    public static class Entry {
        private String id;
        private Long orderId;
        private String kind;
        private String title;
        private BigDecimal amount;
        private LocalDateTime occurredAt;
        private String status;
    }
}
