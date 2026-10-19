// MyBatis 公告数据库接口；已读状态由 announcement_reads 关联计算，避免在公告表重复写用户数据。
package com.campushelp.mapper;

import com.campushelp.model.Announcement;
import java.util.List;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
// 类/接口职责：MyBatis 公告数据库接口；已读状态由 announcement_reads 关联计算，避免在公告表重复写用户数据。
public interface AnnouncementMapper {
    @Select("""
        SELECT a.*, EXISTS(
          SELECT 1 FROM announcement_reads r
          WHERE r.announcement_id = a.id AND r.user_id = #{userId}
        ) AS is_read
        FROM announcements a
        WHERE a.is_published = 1 AND a.published_at <= NOW(3)
        ORDER BY a.pinned DESC, a.published_at DESC, a.id DESC
        LIMIT 100
        """)
    // listForUser：读取已发布公告，并给当前用户计算 is_read；最多 100 条。
    List<Announcement> listForUser(@Param("userId") Long userId);

    @Select("""
        SELECT a.*, EXISTS(
          SELECT 1 FROM announcement_reads r
          WHERE r.announcement_id = a.id AND r.user_id = #{userId}
        ) AS is_read
        FROM announcements a
        WHERE a.id = #{id} AND a.is_published = 1 AND a.published_at <= NOW(3)
        """)
    // findForUser：读取单条已发布公告，同时计算当前用户是否已读；不存在返回 null。
    Announcement findForUser(@Param("id") Long id, @Param("userId") Long userId);

    @Select("""
        SELECT a.* FROM announcements a
        WHERE a.id = #{id} AND a.is_published = 1 AND a.published_at <= NOW(3)
        """)
    // findPublished：只校验公告是否存在且已发布，用于写已读前确认目标合法。
    Announcement findPublished(@Param("id") Long id);

    @Select("""
        SELECT COUNT(*) FROM announcements a
        WHERE a.is_published = 1 AND a.published_at <= NOW(3)
          AND NOT EXISTS(
            SELECT 1 FROM announcement_reads r
            WHERE r.announcement_id = a.id AND r.user_id = #{userId}
          )
        """)
    // unreadCount：统计当前用户尚未读的已发布公告数量。
    int unreadCount(@Param("userId") Long userId);

    @Select("""
        SELECT a.*, 0 AS is_read
        FROM announcements a
        WHERE a.is_published = 1 AND a.published_at <= NOW(3) AND a.force_read = 1
          AND NOT EXISTS(
            SELECT 1 FROM announcement_reads r
            WHERE r.announcement_id = a.id AND r.user_id = #{userId}
          )
        ORDER BY a.pinned DESC, a.published_at DESC, a.id DESC
        LIMIT 1
        """)
    // pendingForUser：取当前用户最需要强提醒的一条未读公告；没有则返回 null。
    Announcement pendingForUser(@Param("userId") Long userId);

    @Insert("""
        INSERT IGNORE INTO announcement_reads(user_id, announcement_id)
        VALUES(#{userId}, #{announcementId})
        """)
    // markRead：写入已读记录；唯一键保证重复调用不会重复插入。
    int markRead(@Param("userId") Long userId, @Param("announcementId") Long announcementId);

    @Insert("""
        INSERT IGNORE INTO announcement_reads(user_id, announcement_id)
        SELECT #{userId}, a.id FROM announcements a
        WHERE a.is_published = 1 AND a.published_at <= NOW(3)
        """)
    // markAllRead：把当前所有已发布公告标记为已读；已读的行由 IGNORE 跳过。
    int markAllRead(@Param("userId") Long userId);
}
