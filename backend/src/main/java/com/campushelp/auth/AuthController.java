// 登录/注册/验证码/密码/手机号接口入口；接收 JSON 后交给 AuthService，不直接写 SQL。
package com.campushelp.auth;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
// 类/接口职责：登录/注册/验证码/密码/手机号接口入口；接收 JSON 后交给 AuthService，不直接写 SQL。
public class AuthController {
    // auth：认证业务依赖，验证用户身份与密码；final 引用在构造后不再换成别的对象。
    private final AuthService auth;
    // AuthController：构造器由 Spring 注入所需依赖（测试中可手动创建）；不在每次请求中重复 new 数据库工具。
    public AuthController(AuthService auth) {
        this.auth = auth;
    }

    // Register：接收路由/JSON 参数并委托 AuthService 的对应业务；Controller 不直接执行 SQL。
    public record Register(String username, String password, String displayName, String phone, String code) {}
    // Login：接收路由/JSON 参数并委托 AuthService 的对应业务；Controller 不直接执行 SQL。
    public record Login(String username, String password) {}
    // PhoneLogin：接收路由/JSON 参数并委托 AuthService 的对应业务；Controller 不直接执行 SQL。
    public record PhoneLogin(String phone, String code) {}
    // CodeRequest：接收路由/JSON 参数并委托 AuthService 的对应业务；Controller 不直接执行 SQL。
    public record CodeRequest(String phone, String purpose) {}
    // ResetPassword：接收路由/JSON 参数并委托 AuthService 的对应业务；Controller 不直接执行 SQL。
    public record ResetPassword(String username, String phone, String code, String newPassword) {}
    // ChangePassword：接收路由/JSON 参数并委托 AuthService 的对应业务；Controller 不直接执行 SQL。
    public record ChangePassword(String oldPassword, String newPassword) {}
    // BindPhone：接收路由/JSON 参数并委托 AuthService 的对应业务；Controller 不直接执行 SQL。
    public record BindPhone(String phone, String code) {}

    @GetMapping("/health")
    public Map<String,
    String> health() {
        return Map.of("status", "ok");
    }

    @PostMapping("/auth/register")
    // register：接收路由/JSON 参数并委托 AuthService 的对应业务；Controller 不直接执行 SQL。
    public AuthService.LoginResult register(@RequestBody Register body) {
        return auth.register(body.username(), body.password(), body.displayName(), body.phone(), body.code());
    }

    @PostMapping("/auth/login")
    // login：接收路由/JSON 参数并委托 AuthService 的对应业务；Controller 不直接执行 SQL。
    public AuthService.LoginResult login(@RequestBody Login body) {
        return auth.login(body.username(), body.password());
    }

    @PostMapping("/auth/phone-login")
    // phoneLogin：接收路由/JSON 参数并委托 AuthService 的对应业务；Controller 不直接执行 SQL。
    public AuthService.LoginResult phoneLogin(@RequestBody PhoneLogin body) {
        return auth.loginPhone(body.phone(), body.code());
    }

    @PostMapping("/auth/code")
    public Map<String,
    String> code(@RequestBody CodeRequest body) {
        return auth.sendCode(body.phone(), body.purpose());
    }

    @PostMapping("/auth/reset-password")
    public Map<String,
    String> reset(@RequestBody ResetPassword body) {
        auth.resetPassword(body.username(), body.phone(), body.code(), body.newPassword());
        return Map.of("message", "密码已重置");
    }

    @PostMapping("/auth/logout")
    public Map<String,
    String> logout(HttpServletRequest request) {
        auth.logout(request.getHeader("Authorization"));
        return Map.of("message", "已退出登录");
    }

    @PostMapping("/me/password")
    public Map<String,
    String> changePassword(HttpServletRequest request, @RequestBody ChangePassword body) {
        auth.changePassword(AuthInterceptor.current(request).getId(), body.oldPassword(), body.newPassword());
        return Map.of("message", "密码已修改");
    }

    @PostMapping("/me/phone")
    public Map<String,
    String> bindPhone(HttpServletRequest request, @RequestBody BindPhone body) {
        auth.bindPhone(AuthInterceptor.current(request).getId(), body.phone(), body.code());
        return Map.of("message", "手机号已绑定");
    }
}
