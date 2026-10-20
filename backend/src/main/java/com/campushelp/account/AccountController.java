// 账户接口入口；返回本人信息、信用分和演示账单，接收反馈/注销请求。
package com.campushelp.account;

import com.campushelp.auth.AuthInterceptor;
import com.campushelp.mapper.FeedbackMapper;
import com.campushelp.mapper.OrderMapper;
import com.campushelp.model.HelpOrder;
import com.campushelp.web.ApiException;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

// 注册 HTTP 控制器（Advice 则为全局异常处理器），返回数据而不是 HTML 页面。
@RestController
@RequestMapping("/api")
// 类/接口职责：账户接口入口；返回本人信息、信用分和演示账单，接收反馈/注销请求。
public class AccountController {
    // accounts：账户统计业务依赖，提供信用分/收入视图；final 引用在构造后不再换成别的对象。
    private final AccountService accounts;
    // feedback：反馈数据库写入接口；final 引用在构造后不再换成别的对象。
    private final FeedbackMapper feedback;
    // orders：订单查询接口，用于校验反馈关联的订单是否属于本人参与。
    private final OrderMapper orders;
    // closure：账号注销业务依赖；final 引用在构造后不再换成别的对象。
    private final AccountClosureService closure;
    // AccountController：构造器由 Spring 注入所需依赖（测试中可手动创建）；不在每次请求中重复 new 数据库工具。
    public AccountController(AccountService accounts, FeedbackMapper feedback, OrderMapper orders,
        AccountClosureService closure) {
        this.accounts = accounts;
        this.feedback = feedback;
        this.orders = orders;
        this.closure = closure;
    }

    // FeedbackRequest：content 为反馈正文，orderId 可选，非空表示针对该订单的反馈。
    // record 是 Java 17 的数据载体，自动提供构造器和字段访问方法；此处承载请求/响应 JSON，不是数据库表。
    public record FeedbackRequest(String content, Long orderId) {}
    // DeactivateRequest：接收路由/JSON 参数并委托 AccountService 的对应业务；Controller 不直接执行 SQL。
    public record DeactivateRequest(String password) {}

    // HTTP 路由：GET 用于读取，POST 用于提交；路径与类上的 @RequestMapping 前缀组合，返回对象自动转 JSON。
    @GetMapping("/me")
    // me：接收路由/JSON 参数并委托 AccountService 的对应业务；Controller 不直接执行 SQL。
    public AccountService.AccountSummary me(HttpServletRequest request) {
        return accounts.summary(userId(request));
    }

    @GetMapping("/me/credit")
    // credit：接收路由/JSON 参数并委托 AccountService 的对应业务；Controller 不直接执行 SQL。
    public AccountService.CreditSummary credit(HttpServletRequest request) {
        return accounts.credit(userId(request));
    }

    @GetMapping("/me/transactions")
    // transactions：接收路由/JSON 参数并委托 AccountService 的对应业务；Controller 不直接执行 SQL。
    public List<AccountService.Transaction> transactions(HttpServletRequest request) {
        return accounts.transactions(userId(request));
    }

    @PostMapping("/feedback")
    public Map<String,
    String> feedback(HttpServletRequest request, @RequestBody FeedbackRequest body) {
        String content = body.content() == null ? "" : body.content().trim();
        if (content.length() < 10 || content.length() > 2000) throw new ApiException(HttpStatus.BAD_REQUEST, "反馈需为 10–2000 字");
        Long userId = userId(request);
        Long orderId = body.orderId();
        // 只允许关联本人参与（发布或接单）的订单，避免给任意订单挂反馈。
        if (orderId != null && !participates(userId, orderId)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "只能对本人参与的订单提交反馈");
        }
        feedback.insert(userId, orderId, content);
        return Map.of("message", "反馈已提交");
    }

    // participates：判断用户是否为该订单的发布者或接单者。
    private boolean participates(Long userId, Long orderId) {
        HelpOrder order = orders.findById(orderId);
        return order != null
            && (userId.equals(order.getPublisherId()) || userId.equals(order.getRunnerId()));
    }

    @PostMapping("/me/deactivate")
    public Map<String,
    String> deactivate(HttpServletRequest request, @RequestBody DeactivateRequest body) {
        closure.deactivate(userId(request), body.password());
        return Map.of("message", "账号已注销");
    }

    // userId：只读取登录拦截器认证过的用户 id，不能让前端自己指定操作者。
    private Long userId(HttpServletRequest request) {
        return AuthInterceptor.current(request).getId();
    }
}
