// 后台认证接口；登录公开，/me 与 /logout 由管理员拦截器保护。
package com.campushelp.admin;

import com.campushelp.audit.AdminAuditService;
import com.campushelp.model.Admin;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/auth")
// 类/接口职责：后台管理员登录、读取当前身份和退出；不处理小程序用户 token。
public class AdminAuthController {
    private final AdminAuthService auth;
    private final AdminAuditService audit;

    public AdminAuthController(AdminAuthService auth, AdminAuditService audit) {
        this.auth = auth;
        this.audit = audit;
    }

    public record Login(String username, String password) {}

    @PostMapping("/login")
    // login：接收账号密码，成功返回一次性明文 token 和管理员展示信息。
    public AdminAuthService.LoginResult login(HttpServletRequest request, @RequestBody Login body) {
        AdminAuthService.LoginResult result = auth.login(body.username(), body.password());
        audit.record(result.admin().id(), result.admin().displayName(), "LOGIN", "admin",
            result.admin().id(), "管理员登录后台", audit.clientIp(request));
        return result;
    }

    @GetMapping("/me")
    // me：登录后刷新页面时恢复顶部栏身份；管理员来自 request 属性，不信任前端传 id。
    public AdminAuthService.AdminView me(HttpServletRequest request) {
        Admin admin = AdminAuthInterceptor.current(request);
        return auth.view(admin);
    }

    @PostMapping("/logout")
    public Map<String, String> logout(HttpServletRequest request) {
        Admin admin = AdminAuthInterceptor.current(request);
        audit.record(request, "LOGOUT", "admin", admin.getId(), "管理员退出后台");
        auth.logout(request.getHeader("Authorization"));
        return Map.of("message", "已退出后台");
    }
}
