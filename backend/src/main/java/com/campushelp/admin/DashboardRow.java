// 数据看板的数据库行对象；由 MyBatis 自动映射下划线字段，Service 再组装成接口返回值。
package com.campushelp.admin;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.Data;

// 类/接口职责：只承载看板聚合查询结果，不直接暴露给前端；避免把数据库行和 API 结构混在一起。
public final class DashboardRow {
    private DashboardRow() {}

    @Data
    public static class Status {
        private String status;
        private Long total;
    }

    @Data
    public static class OrderDay {
        private LocalDate day;
        private Long total;
        private Long completed;
        private BigDecimal amount;
    }

    @Data
    public static class UserDay {
        private LocalDate day;
        private Long total;
    }

    @Data
    public static class RecentOrder {
        private Long id;
        private String type;
        private String title;
        private String status;
        private BigDecimal reward;
        private Long publisherId;
        private String publisherName;
        private Long runnerId;
        private String runnerName;
        private LocalDateTime createdAt;
    }

    @Data
    public static class RecentFeedback {
        private Long id;
        private String content;
        private String userName;
        private LocalDateTime createdAt;
    }
}
