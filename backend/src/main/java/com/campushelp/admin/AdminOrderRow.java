// 后台订单管理数据库行对象；列表只返回参与者摘要，详情不含用户取件码原文。
package com.campushelp.admin;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;

// 类/接口职责：后台订单列表/详情的数据库投影；由 Service 转成前端专用记录。
public final class AdminOrderRow {
    private AdminOrderRow() {}

    @Data
    public static class Order {
        private Long id;
        private String type;
        private String title;
        private String station;
        private String restaurant;
        private String deliveryBuilding;
        private String deliveryAddress;
        private BigDecimal reward;
        private String status;
        private Long publisherId;
        private String publisherName;
        private String publisherPhone;
        private Long runnerId;
        private String runnerName;
        private String runnerPhone;
        private Integer packageCount;
        private Integer codeCount;
        private Boolean hasProof;
        private LocalDateTime createdAt;
        private LocalDateTime deadlineAt;
        private LocalDateTime acceptedAt;
        private LocalDateTime submittedAt;
        private LocalDateTime completedAt;
        private LocalDateTime cancelledAt;
        private String cancelledByName;
    }
}
