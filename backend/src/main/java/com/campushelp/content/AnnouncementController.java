// 公告路由；列表、详情、首屏聚合、未读数、登录后强提醒和已读操作都从登录拦截器取当前用户。
package com.campushelp.content;

import com.campushelp.auth.AuthInterceptor;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/announcements")
// 类/接口职责：公告路由；列表、详情、首屏聚合、未读数、登录后强提醒和已读操作都从登录拦截器取当前用户。
public class AnnouncementController {
    private final AnnouncementService announcements;

    // AnnouncementController：构造器由 Spring 注入业务层；Controller 不直接执行 SQL。
    public AnnouncementController(AnnouncementService announcements) {
        this.announcements = announcements;
    }

    @GetMapping
    // list：filter=all 全部 / unread 未读；分类筛选由前端在已取回的数据上完成。
    public List<AnnouncementService.AnnouncementView> list(HttpServletRequest request,
    @RequestParam(defaultValue = "all") String filter) {
        return announcements.list(userId(request), filter);
    }

    @GetMapping("/overview")
    // overview：首页一次拿到最新公告、未读数和待弹公告。
    public AnnouncementService.Overview overview(HttpServletRequest request) {
        return announcements.overview(userId(request));
    }

    @GetMapping("/unread-count")
    // unreadCount：只返回数字，供公告中心或首页刷新红点。
    public Map<String, Integer> unreadCount(HttpServletRequest request) {
        return Map.of("unreadCount", announcements.unreadCount(userId(request)));
    }

    @GetMapping("/pending")
    // pending：登录后强提醒入口；没有待弹公告时 announcement 为 null。
    public AnnouncementService.PendingResult pending(HttpServletRequest request) {
        return new AnnouncementService.PendingResult(announcements.pending(userId(request)));
    }

    @GetMapping("/{id}")
    // detail：按公告 id 读取已发布公告，并带上当前用户已读状态。
    public AnnouncementService.AnnouncementView detail(HttpServletRequest request, @PathVariable Long id) {
        return announcements.detail(id, userId(request));
    }

    @PostMapping("/{id}/read")
    // read：记录单条已读，返回最新未读数供前端更新小红点。
    public AnnouncementService.ReadResult read(HttpServletRequest request, @PathVariable Long id) {
        return announcements.markRead(id, userId(request));
    }

    @PostMapping("/read-all")
    // readAll：把当前所有已发布公告标记为已读。
    public AnnouncementService.ReadResult readAll(HttpServletRequest request) {
        return announcements.markAllRead(userId(request));
    }

    private Long userId(HttpServletRequest request) {
        return AuthInterceptor.current(request).getId();
    }
}
