// 操作日志查询接口；仅超级管理员可见。
package com.campushelp.audit;

import com.campushelp.admin.AdminAccess;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/audit-logs")
public class AdminAuditController {
    private final AdminAuditService audit;

    public AdminAuditController(AdminAuditService audit) {
        this.audit = audit;
    }

    @GetMapping
    public AdminAuditService.LogPage list(HttpServletRequest request,
        @RequestParam(defaultValue = "1") int page,
        @RequestParam(defaultValue = "20") int size,
        @RequestParam(defaultValue = "all") String action,
        @RequestParam(defaultValue = "") String keyword) {
        AdminAccess.requireSuper(request);
        return audit.list(page, size, action, keyword);
    }
}
