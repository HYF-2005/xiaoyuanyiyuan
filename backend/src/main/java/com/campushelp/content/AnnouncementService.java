// 公告业务层；组合公告列表、未读数、登录后强提醒和已读写入。
package com.campushelp.content;

import com.campushelp.mapper.AnnouncementMapper;
import com.campushelp.model.Announcement;
import com.campushelp.web.ApiException;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
// 类/接口职责：公告业务层；组合公告列表、未读数、登录后强提醒和已读写入。
public class AnnouncementService {
    private static final List<String> FILTERS = List.of("all", "unread");
    private final AnnouncementMapper announcements;

    // AnnouncementService：构造器由 Spring 注入 Mapper；测试时可以传入 mock。
    public AnnouncementService(AnnouncementMapper announcements) {
        this.announcements = announcements;
    }

    // AnnouncementView：列表、详情和弹窗共用的公告视图；read 是当前用户维度，不是公告本身字段。
    public record AnnouncementView(Long id, String title, String summary, String content,
    String category, String level, boolean pinned, boolean forceRead,
    LocalDateTime publishedAt, boolean read) {}

    // Overview：首页一次请求拿到最新公告、未读数和待强提醒公告，减少首屏请求数。
    public record Overview(AnnouncementView latest, int unreadCount, AnnouncementView pending) {}

    // PendingResult：没有待弹公告时返回 announcement:null，保证响应始终是 JSON 对象。
    public record PendingResult(AnnouncementView announcement) {}

    // ReadResult：写已读后返回最新未读数，前端据此同步小红点。
    public record ReadResult(int unreadCount) {}

    // list：读取当前用户可见公告；filter=unread 时只保留未读，非法筛选直接 400。
    public List<AnnouncementView> list(Long userId, String filter) {
        if (filter != null && !FILTERS.contains(filter)) throw bad("公告筛选条件无效");
        return announcements.listForUser(userId).stream()
        .filter(item -> !"unread".equals(filter) || !Boolean.TRUE.equals(item.getIsRead()))
        .map(this::view).toList();
    }

    // overview：首页聚合接口；最新公告取排序后的第一条，pending 由数据库挑选未读强提醒。
    public Overview overview(Long userId) {
        List<AnnouncementView> list = list(userId, "all");
        Announcement pending = announcements.pendingForUser(userId);
        return new Overview(list.isEmpty() ? null : list.get(0), announcements.unreadCount(userId),
        pending == null ? null : view(pending));
    }

    // pending：单独查询登录后需要强制阅读的公告；没有返回 null。
    public AnnouncementView pending(Long userId) {
        Announcement announcement = announcements.pendingForUser(userId);
        return announcement == null ? null : view(announcement);
    }

    // unreadCount：只查未读数，供轻量红点刷新使用，避免为拿一个数字读取整份列表。
    public int unreadCount(Long userId) {
        return announcements.unreadCount(userId);
    }

    // detail：公告不存在或尚未发布统一 404，避免暴露草稿内容。
    public AnnouncementView detail(Long id, Long userId) {
        Announcement announcement = announcements.findForUser(id, userId);
        if (announcement == null) throw notFound();
        return view(announcement);
    }

    // markRead：先确认公告存在且已发布，再写已读；重复调用不会产生重复行。
    public ReadResult markRead(Long id, Long userId) {
        if (announcements.findPublished(id) == null) throw notFound();
        announcements.markRead(userId, id);
        return new ReadResult(announcements.unreadCount(userId));
    }

    // markAllRead：批量写已读，返回实际剩余未读数，防止并发新公告导致红点误消失。
    public ReadResult markAllRead(Long userId) {
        announcements.markAllRead(userId);
        return new ReadResult(announcements.unreadCount(userId));
    }

    // view：把数据库对象转成前端视图；摘要为空时从正文截取，避免列表出现空白。
    private AnnouncementView view(Announcement announcement) {
        return new AnnouncementView(announcement.getId(), announcement.getTitle(), summaryOf(announcement),
        announcement.getContent(), announcement.getCategory(), announcement.getLevel(),
        Boolean.TRUE.equals(announcement.getPinned()), Boolean.TRUE.equals(announcement.getForceRead()),
        announcement.getPublishedAt(), Boolean.TRUE.equals(announcement.getIsRead()));
    }

    // summaryOf：优先使用后台填写的摘要；没有摘要时压缩正文空白并截取 60 字。
    private static String summaryOf(Announcement announcement) {
        String summary = announcement.getSummary();
        if (summary != null && !summary.isBlank()) return summary;
        String content = announcement.getContent() == null ? "" : announcement.getContent().replaceAll("\\s+", " ").trim();
        return content.length() <= 60 ? content : content.substring(0, 60) + "…";
    }

    private static ApiException bad(String message) {
        return new ApiException(HttpStatus.BAD_REQUEST, message);
    }

    private static ApiException notFound() {
        return new ApiException(HttpStatus.NOT_FOUND, "公告不存在");
    }
}
