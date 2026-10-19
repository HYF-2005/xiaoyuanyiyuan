// 后台操作审计：只记录写操作、登录和退出，便于运营追溯。
package com.campushelp.audit;

import com.campushelp.admin.AdminAuthInterceptor;
import com.campushelp.mapper.AdminAuditMapper;
import com.campushelp.model.Admin;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
// 类/接口职责：写入审计日志和分页读取操作日志。
public class AdminAuditService {
    private final AdminAuditMapper logs;

    public AdminAuditService(AdminAuditMapper logs) {
        this.logs = logs;
    }

    public record LogItem(Long id, Long adminId, String adminName, String action, String targetType,
        String targetId, String detail, String ip, java.time.LocalDateTime createdAt) {}

    public record LogPage(int page, int size, long total, List<LogItem> items) {}

    // record：从管理员拦截器读取当前操作者；只应由后台写接口调用。
    public void record(HttpServletRequest request, String action, String targetType, Object targetId, String detail) {
        Admin admin = AdminAuthInterceptor.current(request);
        record(admin.getId(), admin.getDisplayName(), action, targetType, targetId, detail, ip(request));
    }

    // record：登录成功时还没有 request 属性中的管理员，允许显式传入身份。
    public void record(Admin admin, String action, String targetType, Object targetId, String detail, String ip) {
        if (admin == null) return;
        record(admin.getId(), admin.getDisplayName(), action, targetType, targetId, detail, ip);
    }

    public void record(Long adminId, String adminName, String action, String targetType,
        Object targetId, String detail, String ip) {
        if (adminId == null) return;
        logs.insert(adminId, adminName == null ? "管理员" : adminName, action, targetType,
            targetId == null ? null : String.valueOf(targetId),
            detail == null ? null : detail.substring(0, Math.min(500, detail.length())),
            ip == null ? null : ip.substring(0, Math.min(64, ip.length())));
    }

    public String clientIp(HttpServletRequest request) {
        return ip(request);
    }

    public LogPage list(int page, int size, String action, String keyword) {
        int safePage = Math.max(1, page);
        int safeSize = Math.max(1, Math.min(50, size));
        String normalizedAction = action == null || action.isBlank() || "all".equals(action) ? null : action;
        String normalizedKeyword = keyword == null ? "" : keyword.trim();
        long total = logs.count(normalizedAction, normalizedKeyword);
        List<LogItem> items = logs.page(normalizedAction, normalizedKeyword,
                (safePage - 1) * safeSize, safeSize).stream()
            .map(row -> new LogItem(row.getId(), row.getAdminId(), row.getAdminName(), row.getAction(),
                row.getTargetType(), row.getTargetId(), row.getDetail(), row.getIp(), row.getCreatedAt()))
            .toList();
        return new LogPage(safePage, safeSize, total, items);
    }

    private String ip(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) return forwarded.split(",")[0].trim();
        return request.getRemoteAddr();
    }
}
