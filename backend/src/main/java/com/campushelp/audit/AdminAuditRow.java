// 操作日志数据库行对象。
package com.campushelp.audit;

import java.time.LocalDateTime;
import lombok.Data;

@Data
public class AdminAuditRow {
    private Long id;
    private Long adminId;
    private String adminName;
    private String action;
    private String targetType;
    private String targetId;
    private String detail;
    private String ip;
    private LocalDateTime createdAt;
}
