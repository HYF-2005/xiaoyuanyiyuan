// 登录拦截器；业务接口执行前验证 Authorization，将真实用户存入本次 request。
package com.campushelp.auth;

import com.campushelp.model.User;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
// 类/接口职责：登录拦截器；业务接口执行前验证 Authorization，将真实用户存入本次 request。
public class AuthInterceptor implements HandlerInterceptor {
    public static final String USER_ATTRIBUTE = "currentUser";
    private final AuthService auth;

    // AuthInterceptor：构造器由 Spring 注入所需依赖（测试中可手动创建）；不在每次请求中重复 new 数据库工具。
    public AuthInterceptor(AuthService auth) {
        this.auth = auth;
    }

    @Override
    // preHandle：控制器执行前验证令牌，把有效 User 放入 request 属性，true 才放行。
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        User user = auth.currentUser(request.getHeader("Authorization"));
        request.setAttribute(USER_ATTRIBUTE, user);
        return true;
    }

    // current：后续控制器从本次请求取已验证的用户，而不是读取前端传的 userId。
    public static User current(HttpServletRequest request) {
        return (User) request.getAttribute(USER_ATTRIBUTE);
    }
}
