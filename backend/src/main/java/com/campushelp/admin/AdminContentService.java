// 后台内容管理业务；轮播和公告的校验、保存与删除。
package com.campushelp.admin;

import com.campushelp.mapper.AdminContentMapper;
import com.campushelp.model.Announcement;
import com.campushelp.model.Banner;
import com.campushelp.web.ApiException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
// 类/接口职责：后台维护小程序首页轮播和公告；不改变小程序公开读取逻辑。
public class AdminContentService {
    private static final Set<String> LINK_TYPES = Set.of("notice", "page", "web", "miniProgram", "none");
    private static final Set<String> CATEGORIES = Set.of("系统公告", "活动");
    private static final Set<String> LEVELS = Set.of("普通", "重要", "紧急");
    private final AdminContentMapper content;

    public AdminContentService(AdminContentMapper content) {
        this.content = content;
    }

    public List<Banner> banners() {
        return content.banners();
    }

    @Transactional
    public Banner saveBanner(Banner banner) {
        validateBanner(banner);
        long excludeId = banner.getId() == null ? 0L : banner.getId();
        if (content.bannerCodeCount(banner.getCode(), excludeId) > 0) {
            throw new ApiException(HttpStatus.CONFLICT, "轮播编码已存在");
        }
        if (banner.getId() == null) {
            content.insertBanner(banner);
        } else {
            if (content.bannerById(banner.getId()) == null) {
                throw new ApiException(HttpStatus.NOT_FOUND, "轮播不存在");
            }
            content.updateBanner(banner);
        }
        return content.bannerById(banner.getId());
    }

    @Transactional
    public void deleteBanner(Long id) {
        if (content.deleteBanner(id) != 1) throw new ApiException(HttpStatus.NOT_FOUND, "轮播不存在");
    }

    public List<Announcement> announcements() {
        return content.announcements();
    }

    @Transactional
    public Announcement saveAnnouncement(Announcement announcement) {
        validateAnnouncement(announcement);
        Announcement existing = announcement.getId() == null ? null : content.announcementById(announcement.getId());
        if (announcement.getId() != null && existing == null) {
            throw new ApiException(HttpStatus.NOT_FOUND, "公告不存在");
        }
        if (announcement.getSummary() == null || announcement.getSummary().isBlank()) {
            String compact = announcement.getContent().replaceAll("\\s+", " ").trim();
            announcement.setSummary(compact.length() <= 120 ? compact : compact.substring(0, 120) + "…");
        }
        if (announcement.getPublishedAt() == null) {
            announcement.setPublishedAt(existing != null && existing.getPublishedAt() != null
                ? existing.getPublishedAt() : LocalDateTime.now());
        }
        if (announcement.getId() == null) {
            content.insertAnnouncement(announcement);
        } else {
            content.updateAnnouncement(announcement);
        }
        return content.announcementById(announcement.getId());
    }

    @Transactional
    public void deleteAnnouncement(Long id) {
        if (content.deleteAnnouncement(id) != 1) throw new ApiException(HttpStatus.NOT_FOUND, "公告不存在");
    }

    private void validateBanner(Banner banner) {
        banner.setCode(trim(banner.getCode()));
        banner.setImageUrl(trim(banner.getImageUrl()));
        banner.setTag(blank(banner.getTag()) ? "公告" : trim(banner.getTag()));
        banner.setTitle(trim(banner.getTitle()));
        banner.setSubtitle(trim(banner.getSubtitle()));
        banner.setLinkType(blank(banner.getLinkType()) ? "none" : trim(banner.getLinkType()));
        banner.setLinkValue(trim(banner.getLinkValue()));
        if (blank(banner.getCode()) || !banner.getCode().matches("[A-Za-z0-9_-]{2,64}")) {
            throw bad("轮播编码只能包含字母、数字、下划线或短横线，长度 2–64");
        }
        if (blank(banner.getImageUrl()) || banner.getImageUrl().length() > 500
            || !(banner.getImageUrl().startsWith("/assets/") || banner.getImageUrl().startsWith("https://"))) {
            throw bad("图片地址必须是 /assets/ 路径或 HTTPS 地址");
        }
        if (blank(banner.getTitle()) || banner.getTitle().length() > 120) throw bad("标题不能为空且不能超过 120 字");
        if (banner.getSubtitle() != null && banner.getSubtitle().length() > 255) throw bad("副标题不能超过 255 字");
        if (!LINK_TYPES.contains(banner.getLinkType())) throw bad("跳转类型无效");
        if (!"none".equals(banner.getLinkType()) && blank(banner.getLinkValue())) throw bad("请填写跳转目标");
        if (banner.getLinkValue() != null && banner.getLinkValue().length() > 500) throw bad("跳转目标不能超过 500 字");
        if (banner.getSortOrder() == null) banner.setSortOrder(0);
        if (banner.getSortOrder() < 0 || banner.getSortOrder() > 9999) throw bad("排序需在 0–9999 之间");
        if (banner.getEnabled() == null) banner.setEnabled(true);
        if (banner.getStartAt() != null && banner.getEndAt() != null
            && !banner.getEndAt().isAfter(banner.getStartAt())) {
            throw bad("失效时间必须晚于生效时间");
        }
    }

    private void validateAnnouncement(Announcement announcement) {
        announcement.setTitle(trim(announcement.getTitle()));
        announcement.setSummary(trim(announcement.getSummary()));
        announcement.setContent(trim(announcement.getContent()));
        announcement.setCategory(blank(announcement.getCategory()) ? "系统公告" : trim(announcement.getCategory()));
        announcement.setLevel(blank(announcement.getLevel()) ? "普通" : trim(announcement.getLevel()));
        if (blank(announcement.getTitle()) || announcement.getTitle().length() > 120) {
            throw bad("公告标题不能为空且不能超过 120 字");
        }
        if (blank(announcement.getContent())) throw bad("公告正文不能为空");
        if (announcement.getSummary() != null && announcement.getSummary().length() > 255) {
            throw bad("摘要不能超过 255 字");
        }
        if (!CATEGORIES.contains(announcement.getCategory())) throw bad("公告分类无效");
        if (!LEVELS.contains(announcement.getLevel())) throw bad("重要级别无效");
        if (announcement.getPinned() == null) announcement.setPinned(false);
        if (announcement.getForceRead() == null) announcement.setForceRead(true);
        if (announcement.getIsPublished() == null) announcement.setIsPublished(true);
    }

    private static boolean blank(String value) {
        return value == null || value.isBlank();
    }

    private static String trim(String value) {
        return value == null ? null : value.trim();
    }

    private static ApiException bad(String message) {
        return new ApiException(HttpStatus.BAD_REQUEST, message);
    }
}
