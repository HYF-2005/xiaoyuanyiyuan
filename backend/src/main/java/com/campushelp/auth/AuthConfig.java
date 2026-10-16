// 拦截规则配置；保护 /api 下业务接口，公开登录/注册/验证码/重置和健康检查。
package com.campushelp.auth;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
// 类/接口职责：拦截规则配置；保护 /api 下业务接口，公开登录/注册/验证码/重置和健康检查。
public class AuthConfig implements WebMvcConfigurer {
    // interceptor：登录拦截器对象；final 引用在构造后不再换成别的对象。
    private final AuthInterceptor interceptor;
    // AuthConfig：构造器由 Spring 注入所需依赖（测试中可手动创建）；不在每次请求中重复 new 数据库工具。
    public AuthConfig(AuthInterceptor interceptor) {
        this.interceptor = interceptor;
    }

    @Override
    // addInterceptors：为 /api/** 安装登录拦截器，并排除用户登录前需要调用的接口；排除鉴权不等于业务无需校验。
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(interceptor).addPathPatterns("/api/**")
        .excludePathPatterns("/api/auth/register", "/api/auth/login", "/api/auth/phone-login",
        "/api/auth/code", "/api/auth/reset-password", "/api/health");
    }
}
