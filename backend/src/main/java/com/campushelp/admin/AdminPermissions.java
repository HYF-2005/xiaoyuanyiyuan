// 后台菜单权限：角色提供默认值，单个管理员可以保存自定义覆盖。
package com.campushelp.admin;

import com.campushelp.model.Admin;
import com.campushelp.web.ApiException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.http.HttpStatus;

// 类/接口职责：定义可授权菜单、解析管理员权限，并按接口路径做后端二次校验。
public final class AdminPermissions {
    // 顺序与后台侧边栏一致，前端权限弹窗也按这个顺序展示。
    public static final List<String> ALL = List.of(
        "dashboard", "users", "orders", "finance", "wallet",
        "content", "locations", "system", "agreements", "feedback"
    );
    private static final ObjectMapper JSON = new ObjectMapper();
    private static final Map<String, List<String>> ROLE_DEFAULTS = Map.of(
        "super_admin", ALL,
        "operator", List.of("dashboard", "users", "orders", "locations", "feedback"),
        "finance", List.of("dashboard", "finance", "wallet"),
        "content", List.of("dashboard", "content", "agreements", "feedback")
    );

    private AdminPermissions() {}

    public static List<String> defaults(String role) {
        return ROLE_DEFAULTS.getOrDefault(role == null ? "" : role, List.of());
    }

    // resolve：超级管理员固定拥有全部权限；其他管理员有自定义值时使用自定义值，否则走角色默认值。
    public static List<String> resolve(Admin admin) {
        if (admin == null) return List.of();
        if ("super_admin".equals(admin.getRole())) return ALL;
        List<String> stored = parse(admin.getPermissionCodes());
        return stored == null || stored.isEmpty() ? defaults(admin.getRole()) : stored;
    }

    // normalize：去重、过滤超管专属之外的无效项，并按固定菜单顺序输出。
    public static List<String> normalize(List<String> requested) {
        if (requested == null || requested.isEmpty()) {
            throw bad("请至少选择一个权限");
        }
        Set<String> chosen = new LinkedHashSet<>();
        for (String raw : requested) {
            String value = raw == null ? "" : raw.trim();
            if (!ALL.contains(value)) throw bad("权限项无效：" + value);
            chosen.add(value);
        }
        if (chosen.isEmpty()) throw bad("请至少选择一个权限");
        return ALL.stream().filter(chosen::contains).toList();
    }

    public static String serialize(List<String> permissions) {
        try {
            return JSON.writeValueAsString(permissions);
        } catch (Exception error) {
            throw new IllegalStateException("无法保存管理员权限", error);
        }
    }

    // require：前端隐藏菜单只是体验，后端仍按当前登录管理员的实时权限拦截。
    public static void require(Admin admin, String permission) {
        if (permission != null && !resolve(admin).contains(permission)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "当前管理员没有访问该模块的权限");
        }
    }

    // permissionForPath：把后台接口归到侧边栏对应模块；未列出的接口继续由各 Controller 自己校验。
    public static String permissionForPath(String uri) {
        if (uri == null) return null;
        if (uri.startsWith("/api/admin/dashboard")) return "dashboard";
        if (uri.startsWith("/api/admin/users")) return "users";
        if (uri.startsWith("/api/admin/orders") || uri.startsWith("/api/admin/risk-orders")) return "orders";
        if (uri.startsWith("/api/admin/finance")) return "finance";
        if (uri.startsWith("/api/admin/content")) return "content";
        if (uri.startsWith("/api/admin/locations") || uri.startsWith("/api/admin/geography")) return "locations";
        if (uri.startsWith("/api/admin/system") || uri.startsWith("/api/admin/audit-logs")) return "system";
        if (uri.startsWith("/api/admin/agreements")) return "agreements";
        if (uri.startsWith("/api/admin/feedback")) return "feedback";
        return null;
    }

    private static List<String> parse(String raw) {
        if (raw == null || raw.isBlank()) return null;
        try {
            List<String> values = JSON.readValue(raw, new TypeReference<List<String>>() {});
            return values == null ? null : values.stream().filter(ALL::contains).distinct().toList();
        } catch (Exception ignored) {
            // 历史脏数据不应把管理员锁死；回退到角色默认权限。
            return null;
        }
    }

    private static ApiException bad(String message) {
        return new ApiException(HttpStatus.BAD_REQUEST, message);
    }
}
