// 后台登录拦截器；只保护 /api/admin/**，与会小程序用户拦截器完全分开。
package com.campushelp.admin;

import com.campushelp.model.Admin;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
// 类/接口职责：后台管理接口执行前校验管理员会话，并把当前管理员放入 request。
public class AdminAuthInterceptor implements HandlerInterceptor {
    public static final String ADMIN_ATTRIBUTE = "currentAdmin";
    private final AdminAuthService auth;

    public AdminAuthInterceptor(AdminAuthService auth) {
        this.auth = auth;
    }

    @Override
    // preHandle：校验 Authorization；失败抛出 401，由全局异常处理器统一返回 message。
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        Admin admin = auth.currentAdmin(request.getHeader("Authorization"));
        request.setAttribute(ADMIN_ATTRIBUTE, admin);
        AdminPermissions.require(admin, AdminPermissions.permissionForPath(request.getRequestURI()));
        return true;
    }

    public static Admin current(HttpServletRequest request) {
        return (Admin) request.getAttribute(ADMIN_ATTRIBUTE);
    }
}
