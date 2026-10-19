// 管理员管理接口；全部要求超级管理员身份。
package com.campushelp.admin;

import com.campushelp.audit.AdminAuditService;
import com.campushelp.model.Admin;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/admins")
public class AdminManagementController {
    private final AdminManagementService admins;
    private final AdminAuditService audit;

    public AdminManagementController(AdminManagementService admins, AdminAuditService audit) {
        this.admins = admins;
        this.audit = audit;
    }

    public record StatusRequest(boolean active) {}

    @GetMapping
    public List<AdminManagementService.AdminItem> list(HttpServletRequest request) {
        AdminAccess.requireSuper(request);
        return admins.list();
    }

    @PostMapping
    public AdminManagementService.AdminItem create(HttpServletRequest request,
        @RequestBody AdminManagementService.CreateRequest body) {
        AdminAccess.requireSuper(request);
        AdminManagementService.AdminItem saved = admins.create(body);
        audit.record(request, "ADMIN_CREATE", "admin", saved.id(), "创建管理员：" + saved.username());
        return saved;
    }

    @PutMapping("/{id}")
    public AdminManagementService.AdminItem updateProfile(HttpServletRequest request, @PathVariable Long id,
        @RequestBody AdminManagementService.ProfileRequest body) {
        AdminAccess.requireSuper(request);
        Admin actor = AdminAccess.current(request);
        AdminManagementService.AdminItem saved = admins.updateProfile(id, body, actor);
        audit.record(request, "ADMIN_UPDATE", "admin", id, "更新管理员：" + saved.username());
        return saved;
    }

    @PatchMapping("/{id}/status")
    public AdminManagementService.AdminItem status(HttpServletRequest request, @PathVariable Long id,
        @RequestBody StatusRequest body) {
        AdminAccess.requireSuper(request);
        Admin actor = AdminAccess.current(request);
        AdminManagementService.AdminItem saved = admins.updateStatus(id, body.active(), actor);
        audit.record(request, body.active() ? "ADMIN_ENABLE" : "ADMIN_DISABLE", "admin", id,
            (body.active() ? "启用管理员：" : "停用管理员：") + saved.username());
        return saved;
    }

    @PutMapping("/{id}/permissions")
    public AdminManagementService.AdminItem permissions(HttpServletRequest request, @PathVariable Long id,
        @RequestBody AdminManagementService.PermissionRequest body) {
        AdminAccess.requireSuper(request);
        AdminManagementService.AdminItem saved = admins.updatePermissions(id, body);
        audit.record(request, "ADMIN_PERMISSIONS", "admin", id,
            "更新管理员权限：" + String.join(",", saved.permissions()));
        return saved;
    }

    @PatchMapping("/{id}/password")
    public Map<String, String> resetPassword(HttpServletRequest request, @PathVariable Long id,
        @RequestBody AdminManagementService.PasswordRequest body) {
        AdminAccess.requireSuper(request);
        admins.resetPassword(id, body.password());
        audit.record(request, "ADMIN_RESET_PASSWORD", "admin", id, "重置管理员密码 #" + id);
        return Map.of("message", "密码已重置，该管理员现有会话已失效");
    }
}
