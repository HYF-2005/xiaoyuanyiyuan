// 后台用户管理数据库行对象；包含账号状态与订单计数，不返回密码哈希。
package com.campushelp.admin;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;

// 类/接口职责：用户列表/详情的数据库投影；由 Service 转成前端专用记录。
public final class AdminUserRow {
    private AdminUserRow() {}

    @Data
    public static class User {
        private Long id;
        private String username;
        private String displayName;
        private String phone;
        private String college;
        private String major;
        private String grade;
        private String className;
        private String dormitory;
        private Integer creditBase;
        private Boolean active;
        private LocalDateTime createdAt;
        private Long publishedOrders;
        private Long acceptedOrders;
        private Long openOrders;
    }

    @Data
    public static class Order {
        private Long id;
        private String type;
        private String title;
        private String status;
        private BigDecimal reward;
        private LocalDateTime createdAt;
        private String role;
    }
}
