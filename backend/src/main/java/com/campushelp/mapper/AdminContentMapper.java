// 后台内容管理查询；与小程序公开接口分离，允许读取停用/草稿内容。
package com.campushelp.mapper;

import com.campushelp.model.Announcement;
import com.campushelp.model.Banner;
import java.util.List;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface AdminContentMapper {
    @Select("SELECT * FROM banners ORDER BY sort_order ASC, id ASC")
    List<Banner> banners();

    @Select("SELECT * FROM banners WHERE id = #{id}")
    Banner bannerById(Long id);

    @Select("SELECT COUNT(*) FROM banners WHERE code = #{code} AND id <> #{id}")
    int bannerCodeCount(@Param("code") String code, @Param("id") Long id);

    @Insert("""
        INSERT INTO banners (code, image_url, tag, title, subtitle, link_type, link_value,
          sort_order, enabled, start_at, end_at)
        VALUES (#{code}, #{imageUrl}, #{tag}, #{title}, #{subtitle}, #{linkType}, #{linkValue},
          #{sortOrder}, #{enabled}, #{startAt}, #{endAt})
        """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insertBanner(Banner banner);

    @Update("""
        UPDATE banners SET code = #{code}, image_url = #{imageUrl}, tag = #{tag}, title = #{title},
          subtitle = #{subtitle}, link_type = #{linkType}, link_value = #{linkValue},
          sort_order = #{sortOrder}, enabled = #{enabled}, start_at = #{startAt}, end_at = #{endAt}
        WHERE id = #{id}
        """)
    int updateBanner(Banner banner);

    @Delete("DELETE FROM banners WHERE id = #{id}")
    int deleteBanner(Long id);

    @Select("SELECT * FROM announcements ORDER BY pinned DESC, published_at DESC, id DESC")
    List<Announcement> announcements();

    @Select("SELECT * FROM announcements WHERE id = #{id}")
    Announcement announcementById(Long id);

    @Insert("""
        INSERT INTO announcements (title, summary, content, category, level, pinned, force_read,
          is_published, published_at)
        VALUES (#{title}, #{summary}, #{content}, #{category}, #{level}, #{pinned}, #{forceRead},
          #{isPublished}, #{publishedAt})
        """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insertAnnouncement(Announcement announcement);

    @Update("""
        UPDATE announcements SET title = #{title}, summary = #{summary}, content = #{content},
          category = #{category}, level = #{level}, pinned = #{pinned}, force_read = #{forceRead},
          is_published = #{isPublished}, published_at = #{publishedAt}
        WHERE id = #{id}
        """)
    int updateAnnouncement(Announcement announcement);

    @Delete("DELETE FROM announcements WHERE id = #{id}")
    int deleteAnnouncement(Long id);
}
