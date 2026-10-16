// 订单路由入口；当前操作用户来自登录拦截器，不接受前端伪造的用户 id。
package com.campushelp.order;

import com.campushelp.auth.AuthInterceptor;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/orders")
// 类/接口职责：订单路由入口；当前操作用户来自登录拦截器，不接受前端伪造的用户 id。
public class OrderController {
    private final OrderService orders;
    // OrderController：构造器由 Spring 注入所需依赖（测试中可手动创建）；不在每次请求中重复 new 数据库工具。
    public OrderController(OrderService orders) {
        this.orders = orders;
    }

    @PostMapping
    // create：接收路由/JSON 参数并委托 OrderService 的对应业务；Controller 不直接执行 SQL。
    public OrderService.OrderView create(HttpServletRequest request, @RequestBody OrderService.CreateOrder body) {
        return orders.create(userId(request), body);
    }

    @GetMapping("/market")
    // market：接收路由/JSON 参数并委托 OrderService 的对应业务；Controller 不直接执行 SQL。
    public List<OrderService.OrderView> market(HttpServletRequest request, @RequestParam(defaultValue = "parcel") String type) {
        return orders.market(type, userId(request));
    }

    @GetMapping("/mine")
    // mine：接收路由/JSON 参数并委托 OrderService 的对应业务；Controller 不直接执行 SQL。
    public List<OrderService.OrderView> mine(HttpServletRequest request, @RequestParam(required = false) String type) {
        return orders.mine(userId(request), type);
    }

    @GetMapping("/{id}")
    // detail：接收路由/JSON 参数并委托 OrderService 的对应业务；Controller 不直接执行 SQL。
    public OrderService.OrderView detail(HttpServletRequest request, @PathVariable Long id) {
        return orders.detail(id, userId(request));
    }

    @PostMapping("/{id}/accept")
    // accept：接收路由/JSON 参数并委托 OrderService 的对应业务；Controller 不直接执行 SQL。
    public OrderService.OrderView accept(HttpServletRequest request, @PathVariable Long id) {
        return orders.accept(id, userId(request));
    }

    @PostMapping("/{id}/confirm")
    // confirm：接收路由/JSON 参数并委托 OrderService 的对应业务；Controller 不直接执行 SQL。
    public OrderService.OrderView confirm(HttpServletRequest request, @PathVariable Long id) {
        return orders.confirm(id, userId(request));
    }

    @PostMapping("/{id}/cancel")
    // cancel：接收路由/JSON 参数并委托 OrderService 的对应业务；Controller 不直接执行 SQL。
    public OrderService.OrderView cancel(HttpServletRequest request, @PathVariable Long id) {
        return orders.cancel(id, userId(request));
    }

    private Long userId(HttpServletRequest request) {
        return AuthInterceptor.current(request).getId();
    }
}
