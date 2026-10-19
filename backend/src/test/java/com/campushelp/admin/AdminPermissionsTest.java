// 后台权限工具测试；验证角色默认值、权限归一化和接口路径映射。
package com.campushelp.admin;

import static org.junit.jupiter.api.Assertions.*;

import com.campushelp.model.Admin;
import com.campushelp.web.ApiException;
import java.util.List;
import org.junit.jupiter.api.Test;

class AdminPermissionsTest {
    @Test
    void superAdminAlwaysHasEveryPermission() {
        var admin = admin("super_admin", "[\"dashboard\"]");

        assertEquals(AdminPermissions.ALL, AdminPermissions.resolve(admin));
    }

    @Test
    void customPermissionsOverrideRoleDefaults() {
        var admin = admin("operator", "[\"dashboard\",\"users\"]");

        assertEquals(List.of("dashboard", "users"), AdminPermissions.resolve(admin));
    }

    @Test
    void financeFallsBackToRoleDefaults() {
        var admin = admin("finance", null);

        assertEquals(List.of("dashboard", "finance", "wallet"), AdminPermissions.resolve(admin));
    }

    @Test
    void normalizeRejectsUnknownPermissionAndEmptySelection() {
        assertEquals(List.of("dashboard", "users"),
            AdminPermissions.normalize(List.of("users", "dashboard", "users")));
        assertThrows(ApiException.class, () -> AdminPermissions.normalize(List.of("admins")));
        assertThrows(ApiException.class, () -> AdminPermissions.normalize(List.of()));
    }

    @Test
    void mapsAdminApiPathsToMenuPermissions() {
        assertEquals("dashboard", AdminPermissions.permissionForPath("/api/admin/dashboard"));
        assertEquals("orders", AdminPermissions.permissionForPath("/api/admin/risk-orders"));
        assertEquals("locations", AdminPermissions.permissionForPath("/api/admin/geography/overview"));
        assertEquals("system", AdminPermissions.permissionForPath("/api/admin/audit-logs"));
        assertNull(AdminPermissions.permissionForPath("/api/admin/auth/me"));
        assertNull(AdminPermissions.permissionForPath("/api/admin/admins"));
    }

    private Admin admin(String role, String permissionCodes) {
        var admin = new Admin();
        admin.setRole(role);
        admin.setPermissionCodes(permissionCodes);
        return admin;
    }
}
