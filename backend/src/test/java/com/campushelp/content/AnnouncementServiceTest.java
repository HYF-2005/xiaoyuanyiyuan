// 公告业务单元测试；覆盖首页聚合、未读筛选、摘要兜底和已读写入的边界。
package com.campushelp.content;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.campushelp.mapper.AnnouncementMapper;
import com.campushelp.model.Announcement;
import com.campushelp.web.ApiException;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

// 类/接口职责：公告业务单元测试；覆盖首页聚合、未读筛选、摘要兜底和已读写入的边界。
class AnnouncementServiceTest {
    @Test
    // overviewReturnsLatestUnreadAndPending：首页一次聚合应返回最新公告、未读数和待强制阅读公告。
    void overviewReturnsLatestUnreadAndPending() {
        AnnouncementMapper mapper = mock(AnnouncementMapper.class);
        AnnouncementService service = new AnnouncementService(mapper);
        Announcement pinned = announcement(1L, "国庆取件调整", "摘要一", "系统公告", "重要", true, true, false);
        Announcement second = announcement(2L, "首单立减", "摘要二", "活动", "普通", false, true, false);
        when(mapper.listForUser(7L)).thenReturn(List.of(pinned, second));
        when(mapper.unreadCount(7L)).thenReturn(2);
        when(mapper.pendingForUser(7L)).thenReturn(pinned);

        AnnouncementService.Overview overview = service.overview(7L);

        assertEquals(1L, overview.latest().id());
        assertEquals(2, overview.unreadCount());
        assertEquals(1L, overview.pending().id());
        assertFalse(overview.latest().read());
    }

    @Test
    // summaryFallsBackToCompressedContent：后台没填摘要时，应从正文压缩空白并截取，不能返回 null。
    void summaryFallsBackToCompressedContent() {
        AnnouncementMapper mapper = mock(AnnouncementMapper.class);
        AnnouncementService service = new AnnouncementService(mapper);
        Announcement item = announcement(3L, "搬迁通知", null, "系统公告", "普通", false, false, false);
        item.setContent("第一段\n\n第二段");
        when(mapper.listForUser(7L)).thenReturn(List.of(item));

        AnnouncementService.AnnouncementView view = service.list(7L, "all").get(0);

        assertEquals("第一段 第二段", view.summary());
    }

    @Test
    // unreadFilterKeepsOnlyUnread：filter=unread 时已读公告必须被过滤掉。
    void unreadFilterKeepsOnlyUnread() {
        AnnouncementMapper mapper = mock(AnnouncementMapper.class);
        AnnouncementService service = new AnnouncementService(mapper);
        Announcement read = announcement(1L, "已读", "摘要", "系统公告", "普通", false, false, true);
        Announcement unread = announcement(2L, "未读", "摘要", "系统公告", "普通", false, false, false);
        when(mapper.listForUser(7L)).thenReturn(List.of(read, unread));

        List<AnnouncementService.AnnouncementView> result = service.list(7L, "unread");

        assertEquals(1, result.size());
        assertEquals(2L, result.get(0).id());
    }

    @Test
    // invalidFilterIsRejectedBeforeQuery：非法筛选条件应返回 400，避免把未知参数当成全部。
    void invalidFilterIsRejectedBeforeQuery() {
        AnnouncementMapper mapper = mock(AnnouncementMapper.class);
        AnnouncementService service = new AnnouncementService(mapper);

        ApiException error = assertThrows(ApiException.class, () -> service.list(7L, "draft"));

        assertEquals(HttpStatus.BAD_REQUEST, error.status());
    }

    @Test
    // pendingReturnsNullWhenNothingNeedsPopup：没有未读强提醒时返回 null，前端不应弹空卡片。
    void pendingReturnsNullWhenNothingNeedsPopup() {
        AnnouncementMapper mapper = mock(AnnouncementMapper.class);
        AnnouncementService service = new AnnouncementService(mapper);
        when(mapper.pendingForUser(7L)).thenReturn(null);

        assertNull(service.pending(7L));
    }

    @Test
    // markReadRejectsMissingAnnouncement：目标不存在或未发布时返回 404，并且不写已读表。
    void markReadRejectsMissingAnnouncement() {
        AnnouncementMapper mapper = mock(AnnouncementMapper.class);
        AnnouncementService service = new AnnouncementService(mapper);
        when(mapper.findPublished(99L)).thenReturn(null);

        ApiException error = assertThrows(ApiException.class, () -> service.markRead(99L, 7L));

        assertEquals(HttpStatus.NOT_FOUND, error.status());
        verify(mapper, never()).markRead(7L, 99L);
    }

    @Test
    // markReadReturnsLatestUnreadCount：写已读后返回最新未读数，供前端同步红点。
    void markReadReturnsLatestUnreadCount() {
        AnnouncementMapper mapper = mock(AnnouncementMapper.class);
        AnnouncementService service = new AnnouncementService(mapper);
        when(mapper.findPublished(1L)).thenReturn(announcement(1L, "公告", "摘要", "系统公告", "重要", true, true, false));
        when(mapper.unreadCount(7L)).thenReturn(0);

        AnnouncementService.ReadResult result = service.markRead(1L, 7L);

        assertEquals(0, result.unreadCount());
        verify(mapper).markRead(7L, 1L);
    }

    @Test
    // detailRejectsDraftOrMissingAnnouncement：列表看不到的公告，详情也必须 404。
    void detailRejectsDraftOrMissingAnnouncement() {
        AnnouncementMapper mapper = mock(AnnouncementMapper.class);
        AnnouncementService service = new AnnouncementService(mapper);
        when(mapper.findForUser(99L, 7L)).thenReturn(null);

        ApiException error = assertThrows(ApiException.class, () -> service.detail(99L, 7L));

        assertEquals(HttpStatus.NOT_FOUND, error.status());
    }

    private static Announcement announcement(Long id, String title, String summary, String category,
    String level, boolean pinned, boolean forceRead, boolean read) {
        Announcement item = new Announcement();
        item.setId(id);
        item.setTitle(title);
        item.setSummary(summary);
        item.setContent("正文");
        item.setCategory(category);
        item.setLevel(level);
        item.setPinned(pinned);
        item.setForceRead(forceRead);
        item.setIsPublished(true);
        item.setPublishedAt(LocalDateTime.of(2026, 9, 28, 18, 0));
        item.setIsRead(read);
        return item;
    }
}
