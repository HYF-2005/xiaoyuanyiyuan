// 后台管理员管理业务；仅超级管理员可调用，避免后台被普通运营账号接管。
package com.campushelp.admin;

import com.campushelp.mapper.AdminMapper;
import com.campushelp.model.Admin;
import com.campushelp.web.ApiException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdminManagementService {
    private static final Set<String> ROLES = Set.of("super_admin", "operator", "finance", "content");
    private final AdminMapper admins;
    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    public AdminManagementService(AdminMapper admins) {
        this.admins = admins;
    }

    public record AdminItem(Long id, String username, String displayName, String phone, String email,
        String role, Boolean active, String remark, List<String> permissions,
        LocalDateTime lastLoginAt, LocalDateTime createdAt) {}

    public record CreateRequest(String username, String displayName, String phone, String email,
        String role, String password, Boolean active, String remark) {}

    public record ProfileRequest(String displayName, String phone, String email, String role, String remark) {}

    public record PasswordRequest(String password) {}

    public record PermissionRequest(List<String> permissions) {}

    public List<AdminItem> list() {
        return admins.findAll().stream().map(this::item).toList();
    }

    @Transactional
    public AdminItem create(CreateRequest body) {
        String username = trim(body.username());
        String displayName = trim(body.displayName());
        String role = trim(body.role());
        if (username == null || !username.matches("[A-Za-z0-9_.-]{3,64}")) {
            throw bad("账号只能包含字母、数字、下划线、点或短横线，长度 3–64");
        }
        String phone = contact(body.phone(), "手机号", 20);
        String email = contact(body.email(), "邮箱", 128);
        String remark = contact(body.remark(), "备注", 500);
        validateDisplayName(displayName);
        validateContact(phone, email);
        validateRole(role);
        validatePassword(body.password());
        if (admins.findByUsername(username) != null) throw new ApiException(HttpStatus.CONFLICT, "管理员账号已存在");
        Admin admin = new Admin();
        admin.setUsername(username);
        admin.setDisplayName(displayName);
        admin.setPhone(phone);
        admin.setEmail(email);
        admin.setRole(role);
        admin.setPasswordHash(encoder.encode(body.password()));
        admin.setActive(body.active() == null || body.active());
        admin.setRemark(remark);
        admins.insert(admin);
        return item(admins.findById(admin.getId()));
    }

    @Transactional
    public AdminItem updateProfile(Long id, ProfileRequest body, Admin actor) {
        Admin target = require(id);
        String displayName = trim(body.displayName());
        String phone = contact(body.phone(), "手机号", 20);
        String email = contact(body.email(), "邮箱", 128);
        String remark = contact(body.remark(), "备注", 500);
        String role = trim(body.role());
        validateDisplayName(displayName);
        validateContact(phone, email);
        validateRole(role);
        if (actor.getId().equals(id) && !"super_admin".equals(role)) {
            throw bad("不能降级当前登录的超级管理员");
        }
        if ("super_admin".equals(target.getRole()) && !"super_admin".equals(role)
            && Boolean.TRUE.equals(target.getActive()) && admins.countOtherActiveSuperAdmins(id) == 0) {
            throw bad("必须至少保留一名启用中的超级管理员");
        }
        admins.updateProfile(id, displayName, phone, email, role, remark);
        return item(require(id));
    }

    @Transactional
    public AdminItem updateStatus(Long id, boolean active, Admin actor) {
        Admin target = require(id);
        if (actor.getId().equals(id) && !active) throw bad("不能停用当前登录的管理员");
        if (!active && "super_admin".equals(target.getRole()) && Boolean.TRUE.equals(target.getActive())
            && admins.countOtherActiveSuperAdmins(id) == 0) {
            throw bad("必须至少保留一名启用中的超级管理员");
        }
        admins.updateStatus(id, active);
        if (!active) admins.deleteSessionsByAdminId(id);
        return item(require(id));
    }

    @Transactional
    public AdminItem updatePermissions(Long id, PermissionRequest body) {
        Admin target = require(id);
        if ("super_admin".equals(target.getRole())) {
            throw bad("超级管理员默认拥有全部权限，无需单独配置");
        }
        List<String> permissions = AdminPermissions.normalize(body == null ? null : body.permissions());
        admins.updatePermissions(id, AdminPermissions.serialize(permissions));
        return item(require(id));
    }

    @Transactional
    public void resetPassword(Long id, String password) {
        require(id);
        validatePassword(password);
        admins.updatePassword(id, encoder.encode(password));
        admins.deleteSessionsByAdminId(id);
    }

    private Admin require(Long id) {
        Admin admin = admins.findById(id);
        if (admin == null) throw new ApiException(HttpStatus.NOT_FOUND, "管理员不存在");
        return admin;
    }

    private AdminItem item(Admin admin) {
        return new AdminItem(admin.getId(), admin.getUsername(), admin.getDisplayName(), admin.getPhone(),
            admin.getEmail(), admin.getRole(), admin.getActive(), admin.getRemark(),
            AdminPermissions.resolve(admin), admin.getLastLoginAt(), admin.getCreatedAt());
    }

    private void validateDisplayName(String displayName) {
        if (displayName == null || displayName.isBlank() || displayName.length() > 64) {
            throw bad("真实姓名不能为空且不能超过 64 字");
        }
    }

    private void validateContact(String phone, String email) {
        if (phone != null && !phone.matches("1[3-9]\\d{9}")) {
            throw bad("手机号需为 11 位号码");
        }
        if (email != null && !email.matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]{2,}$")) {
            throw bad("邮箱格式不正确");
        }
    }

    private static String contact(String value, String label, int maxLength) {
        String normalized = trim(value);
        if (normalized == null || normalized.isEmpty()) return null;
        if (normalized.length() > maxLength) throw bad(label + "不能超过 " + maxLength + " 字");
        return normalized;
    }

    private void validateRole(String role) {
        if (role == null || !ROLES.contains(role)) throw bad("管理员角色无效");
    }

    private void validatePassword(String password) {
        if (password == null || password.length() < 8 || password.length() > 72) {
            throw bad("密码长度需为 8–72 位");
        }
    }

    private static String trim(String value) {
        return value == null ? null : value.trim();
    }

    private static ApiException bad(String message) {
        return new ApiException(HttpStatus.BAD_REQUEST, message);
    }
}
