// 管理员管理边界测试；重点防止把自己或最后一名超级管理员锁在门外。
package com.campushelp.admin;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.campushelp.mapper.AdminMapper;
import com.campushelp.model.Admin;
import com.campushelp.web.ApiException;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;

class AdminManagementServiceTest {
    @Test
    void cannotDisableCurrentAdmin() {
        var mapper = mock(AdminMapper.class);
        var service = new AdminManagementService(mapper);
        var actor = admin(1L, "admin", "super_admin", true);
        when(mapper.findById(1L)).thenReturn(actor);

        var error = assertThrows(ApiException.class, () -> service.updateStatus(1L, false, actor));

        assertEquals(400, error.status().value());
        verify(mapper, never()).updateStatus(anyLong(), anyBoolean());
    }

    @Test
    void cannotDisableLastActiveSuperAdmin() {
        var mapper = mock(AdminMapper.class);
        var service = new AdminManagementService(mapper);
        var actor = admin(2L, "operator", "operator", true);
        var target = admin(1L, "root", "super_admin", true);
        when(mapper.findById(1L)).thenReturn(target);
        when(mapper.countOtherActiveSuperAdmins(1L)).thenReturn(0);

        var error = assertThrows(ApiException.class, () -> service.updateStatus(1L, false, actor));

        assertEquals(400, error.status().value());
        verify(mapper, never()).updateStatus(1L, false);
    }

    @Test
    void createsAdminWithBcryptPassword() {
        var mapper = mock(AdminMapper.class);
        var service = new AdminManagementService(mapper);
        var saved = new AtomicReference<Admin>();
        when(mapper.findByUsername("new.operator")).thenReturn(null);
        when(mapper.insert(any())).thenAnswer(invocation -> {
            Admin admin = invocation.getArgument(0);
            admin.setId(3L);
            saved.set(admin);
            return 1;
        });
        when(mapper.findById(3L)).thenAnswer(invocation -> saved.get());

        var created = service.create(new AdminManagementService.CreateRequest(
            "new.operator", "新运营", "13800138000", "operator@example.com",
            "operator", "Test@12345", false, "夜班交接"));

        assertEquals("new.operator", created.username());
        assertEquals("13800138000", created.phone());
        assertEquals("operator@example.com", created.email());
        assertEquals("夜班交接", created.remark());
        assertEquals(List.of("dashboard", "users", "orders", "locations", "feedback"), created.permissions());
        assertFalse(created.active());
        assertNotEquals("Test@12345", saved.get().getPasswordHash());
        assertTrue(saved.get().getPasswordHash().startsWith("$2"));
        assertFalse(saved.get().getActive());
    }

    @Test
    void updatesCustomPermissionsForNonSuperAdmin() {
        var mapper = mock(AdminMapper.class);
        var service = new AdminManagementService(mapper);
        var target = admin(5L, "operator", "operator", true);
        when(mapper.findById(5L)).thenReturn(target);
        when(mapper.updatePermissions(eq(5L), anyString())).thenAnswer(invocation -> {
            target.setPermissionCodes(invocation.getArgument(1));
            return 1;
        });

        var updated = service.updatePermissions(5L,
            new AdminManagementService.PermissionRequest(List.of("feedback", "dashboard", "dashboard")));

        assertEquals(List.of("dashboard", "feedback"), updated.permissions());
        verify(mapper).updatePermissions(eq(5L), contains("feedback"));
    }

    @Test
    void cannotConfigurePermissionsForSuperAdmin() {
        var mapper = mock(AdminMapper.class);
        var service = new AdminManagementService(mapper);
        when(mapper.findById(1L)).thenReturn(admin(1L, "root", "super_admin", true));

        var error = assertThrows(ApiException.class, () -> service.updatePermissions(1L,
            new AdminManagementService.PermissionRequest(List.of("dashboard"))));

        assertEquals(400, error.status().value());
        verify(mapper, never()).updatePermissions(anyLong(), anyString());
    }

    @Test
    void rejectsMalformedAdminContact() {
        var mapper = mock(AdminMapper.class);
        var service = new AdminManagementService(mapper);

        var error = assertThrows(ApiException.class, () -> service.create(
            new AdminManagementService.CreateRequest("new.operator", "新运营", "12345",
                "not-an-email", "operator", "Test@12345", true, null)));

        assertEquals(400, error.status().value());
        verify(mapper, never()).insert(any());
    }

    @Test
    void resetPasswordRevokesSessions() {
        var mapper = mock(AdminMapper.class);
        var service = new AdminManagementService(mapper);
        when(mapper.findById(2L)).thenReturn(admin(2L, "operator", "operator", true));

        service.resetPassword(2L, "NewTest@12345");

        verify(mapper).updatePassword(eq(2L), startsWith("$2"));
        verify(mapper).deleteSessionsByAdminId(2L);
    }

    private Admin admin(Long id, String username, String role, boolean active) {
        var admin = new Admin();
        admin.setId(id);
        admin.setUsername(username);
        admin.setDisplayName(username);
        admin.setRole(role);
        admin.setActive(active);
        return admin;
    }
}
