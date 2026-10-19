// 后台用户管理接口；所有路由由 /api/admin/** 管理员拦截器保护。
package com.campushelp.admin;

import com.campushelp.audit.AdminAuditService;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/admin/users")
// 类/接口职责：用户分页查询、详情和启用/停用操作。
public class AdminUserController {
    private final AdminUserService users;
    private final AdminUserImportService imports;
    private final AdminAuditService audit;

    public AdminUserController(AdminUserService users, AdminUserImportService imports,
        AdminAuditService audit) {
        this.users = users;
        this.imports = imports;
        this.audit = audit;
    }

    public record StatusRequest(Boolean active) {}

    @GetMapping
    public AdminUserService.UserPage list(
        @RequestParam(defaultValue = "1") int page,
        @RequestParam(defaultValue = "10") int size,
        @RequestParam(defaultValue = "all") String status,
        @RequestParam(defaultValue = "") String keyword
    ) {
        return users.list(keyword, status, page, size);
    }

    @PostMapping("/import")
    public AdminUserImportService.ImportResult importUsers(HttpServletRequest request,
        @RequestParam("file") MultipartFile file) {
        AdminUserImportService.ImportResult result = imports.importFile(file);
        audit.record(request, "USER_IMPORT", "user", null,
            "批量导入用户：成功 " + result.success() + " 条，失败 " + result.failed() + " 条");
        return result;
    }

    @GetMapping("/{id}")
    public AdminUserService.UserDetail detail(@PathVariable Long id) {
        return users.detail(id);
    }

    @PatchMapping("/{id}/status")
    public Map<String, String> status(HttpServletRequest request, @PathVariable Long id,
        @RequestBody StatusRequest body) {
        users.updateStatus(id, body.active());
        audit.record(request, Boolean.TRUE.equals(body.active()) ? "USER_ENABLE" : "USER_DISABLE",
            "user", id, Boolean.TRUE.equals(body.active()) ? "启用用户" : "停用用户");
        return Map.of("message", Boolean.TRUE.equals(body.active()) ? "用户已启用" : "用户已停用");
    }
}
