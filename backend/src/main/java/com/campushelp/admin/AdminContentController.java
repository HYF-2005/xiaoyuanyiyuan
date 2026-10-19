// 后台内容管理接口；轮播和公告的增删改查。
package com.campushelp.admin;

import com.campushelp.audit.AdminAuditService;
import com.campushelp.model.Announcement;
import com.campushelp.model.Banner;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/content")
public class AdminContentController {
    private final AdminContentService content;
    private final AdminAuditService audit;

    public AdminContentController(AdminContentService content, AdminAuditService audit) {
        this.content = content;
        this.audit = audit;
    }

    @GetMapping("/banners")
    public List<Banner> banners() {
        return content.banners();
    }

    @PostMapping("/banners")
    public Banner createBanner(HttpServletRequest request, @RequestBody Banner banner) {
        banner.setId(null);
        Banner saved = content.saveBanner(banner);
        audit.record(request, "BANNER_CREATE", "banner", saved.getId(), "创建轮播：" + saved.getTitle());
        return saved;
    }

    @PutMapping("/banners/{id}")
    public Banner updateBanner(HttpServletRequest request, @PathVariable Long id, @RequestBody Banner banner) {
        banner.setId(id);
        Banner saved = content.saveBanner(banner);
        audit.record(request, "BANNER_UPDATE", "banner", id, "更新轮播：" + saved.getTitle());
        return saved;
    }

    @DeleteMapping("/banners/{id}")
    public Map<String, String> deleteBanner(HttpServletRequest request, @PathVariable Long id) {
        content.deleteBanner(id);
        audit.record(request, "BANNER_DELETE", "banner", id, "删除轮播 #" + id);
        return Map.of("message", "轮播已删除");
    }

    @GetMapping("/announcements")
    public List<Announcement> announcements() {
        return content.announcements();
    }

    @PostMapping("/announcements")
    public Announcement createAnnouncement(HttpServletRequest request, @RequestBody Announcement announcement) {
        announcement.setId(null);
        Announcement saved = content.saveAnnouncement(announcement);
        audit.record(request, "ANNOUNCEMENT_CREATE", "announcement", saved.getId(),
            "创建公告：" + saved.getTitle());
        return saved;
    }

    @PutMapping("/announcements/{id}")
    public Announcement updateAnnouncement(HttpServletRequest request, @PathVariable Long id,
        @RequestBody Announcement announcement) {
        announcement.setId(id);
        Announcement saved = content.saveAnnouncement(announcement);
        audit.record(request, "ANNOUNCEMENT_UPDATE", "announcement", id,
            "更新公告：" + saved.getTitle());
        return saved;
    }

    @DeleteMapping("/announcements/{id}")
    public Map<String, String> deleteAnnouncement(HttpServletRequest request, @PathVariable Long id) {
        content.deleteAnnouncement(id);
        audit.record(request, "ANNOUNCEMENT_DELETE", "announcement", id, "删除公告 #" + id);
        return Map.of("message", "公告已删除");
    }
}
