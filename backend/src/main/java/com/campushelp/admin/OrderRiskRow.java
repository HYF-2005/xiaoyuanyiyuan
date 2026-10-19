// 风险订单数据库行对象；扫描信号、风险快照和汇总数字都由 Mapper 投影到这些内部记录。
package com.campushelp.admin;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;

// 类/接口职责：后台风险订单扫描/队列的数据库投影，不直接暴露给前端。
public final class OrderRiskRow {
    private OrderRiskRow() {}

    // 规则扫描所需的最小订单字段；包含发布者和接单者注册时间，用于新账号判断。
    @Data
    public static class Signal {
        private Long id;
        private String type;
        private String title;
        private String status;
        private BigDecimal reward;
        private Long publisherId;
        private Long runnerId;
        private String proofFile;
        private Long cancelledBy;
        private LocalDateTime createdAt;
        private LocalDateTime acceptedAt;
        private LocalDateTime submittedAt;
        private LocalDateTime completedAt;
        private LocalDateTime cancelledAt;
        private LocalDateTime publisherCreatedAt;
        private LocalDateTime runnerCreatedAt;
    }

    // 风险队列行；risk_flags 是 JSON 数组文本，由 Service 转成字符串列表。
    @Data
    public static class Risk {
        private Long orderId;
        private Integer riskScore;
        private String riskLevel;
        private String riskFlags;
        private String reviewStatus;
        private String reviewNote;
        private LocalDateTime reviewedAt;
        private String reviewerName;
        private LocalDateTime scannedAt;
        private String type;
        private String title;
        private String status;
        private BigDecimal reward;
        private LocalDateTime createdAt;
        private Long publisherId;
        private String publisherName;
        private Long runnerId;
        private String runnerName;
    }

    // 风险队列汇总；COUNT(CASE WHEN ...) 避免 SUM 返回 DECIMAL。
    @Data
    public static class Summary {
        private Long total;
        private Long high;
        private Long medium;
        private Long low;
        private Long pending;
        private Long cleared;
        private Long confirmed;
    }
}
