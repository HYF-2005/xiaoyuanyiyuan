// 后台拦截规则；/api/admin/auth/login 公开，其余 /api/admin/** 必须带管理员 token。
package com.campushelp.admin;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
// 类/接口职责：为后台管理接口安装独立拦截器，避免误用小程序普通用户的登录态。
public class AdminAuthConfig implements WebMvcConfigurer {
    private final AdminAuthInterceptor interceptor;

    public AdminAuthConfig(AdminAuthInterceptor interceptor) {
        this.interceptor = interceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(interceptor)
            .addPathPatterns("/api/admin/**")
            .excludePathPatterns("/api/admin/auth/login");
    }
}
