// 首页轮播路由；轮播属于登录后内容，沿用 /api/** 的统一登录拦截。
package com.campushelp.content;

import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/banners")
// 类/接口职责：首页轮播路由；轮播属于登录后内容，沿用 /api/** 的统一登录拦截。
public class BannerController {
    private final BannerService banners;

    // BannerController：构造器由 Spring 注入业务层；Controller 不直接执行 SQL。
    public BannerController(BannerService banners) {
        this.banners = banners;
    }

    @GetMapping
    // list：返回当前有效轮播，前端用 swiper 渲染并绑定点击跳转。
    public List<BannerService.BannerView> list() {
        return banners.list();
    }
}
