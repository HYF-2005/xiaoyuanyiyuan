// 后台角色简单守卫；当前阶段管理员管理/审计只允许 super_admin。
package com.campushelp.admin;

import com.campushelp.model.Admin;
import com.campushelp.web.ApiException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;

public final class AdminAccess {
    private AdminAccess() {}

    public static Admin current(HttpServletRequest request) {
        return AdminAuthInterceptor.current(request);
    }

    public static void requireSuper(HttpServletRequest request) {
        if (!"super_admin".equals(current(request).getRole())) {
            throw new ApiException(HttpStatus.FORBIDDEN, "只有超级管理员可以执行此操作");
        }
    }
}
