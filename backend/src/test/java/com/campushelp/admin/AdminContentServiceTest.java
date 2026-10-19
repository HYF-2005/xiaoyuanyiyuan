// 内容管理业务单元测试；覆盖校验、编码冲突和公告摘要生成。
package com.campushelp.admin;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.campushelp.mapper.AdminContentMapper;
import com.campushelp.model.Announcement;
import com.campushelp.model.Banner;
import com.campushelp.web.ApiException;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;

class AdminContentServiceTest {
    @Test
    void rejectsUnsafeBannerImageUrl() {
        var service = new AdminContentService(mock(AdminContentMapper.class));
        var banner = banner();
        banner.setImageUrl("javascript:alert(1)");

        var error = assertThrows(ApiException.class, () -> service.saveBanner(banner));
        assertEquals(400, error.status().value());
    }

    @Test
    void rejectsDuplicateBannerCode() {
        var mapper = mock(AdminContentMapper.class);
        var service = new AdminContentService(mapper);
        when(mapper.bannerCodeCount("home-test", 0L)).thenReturn(1);

        var error = assertThrows(ApiException.class, () -> service.saveBanner(banner()));
        assertEquals(409, error.status().value());
        verify(mapper, never()).insertBanner(any());
    }

    @Test
    void autoGeneratesAnnouncementSummaryAndPublishTime() {
        var mapper = mock(AdminContentMapper.class);
        var service = new AdminContentService(mapper);
        var saved = new AtomicReference<Announcement>();
        when(mapper.insertAnnouncement(any())).thenAnswer(invocation -> {
            Announcement item = invocation.getArgument(0);
            item.setId(1L);
            saved.set(item);
            return 1;
        });
        when(mapper.announcementById(1L)).thenAnswer(invocation -> saved.get());
        var announcement = new Announcement();
        announcement.setTitle("公告标题");
        announcement.setContent("这是一段很长的公告正文。".repeat(20));
        announcement.setCategory("系统公告");
        announcement.setLevel("重要");

        var result = service.saveAnnouncement(announcement);

        assertNotNull(result.getSummary());
        assertTrue(result.getSummary().endsWith("…"));
        assertTrue(result.getPublishedAt() != null);
        assertEquals(Boolean.TRUE, result.getForceRead());
        assertEquals(Boolean.TRUE, result.getIsPublished());
    }

    private Banner banner() {
        var banner = new Banner();
        banner.setCode("home-test");
        banner.setImageUrl("/assets/banners/test.jpg");
        banner.setTitle("测试轮播");
        banner.setTag("公告");
        banner.setLinkType("none");
        banner.setSortOrder(1);
        banner.setEnabled(true);
        return banner;
    }
}
