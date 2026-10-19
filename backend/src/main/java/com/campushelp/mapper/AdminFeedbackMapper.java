// 后台反馈查询与处理状态保存。
package com.campushelp.mapper;

import com.campushelp.admin.AdminFeedbackRow;
import java.util.List;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface AdminFeedbackMapper {
    @Select("""
        <script>
        SELECT f.id, f.user_id, u.username AS account, u.display_name AS user_name, f.content, f.created_at,
               COALESCE(h.status, '待处理') AS status, h.reply,
               h.handled_at, a.display_name AS handled_by_name
        FROM feedback f
        JOIN users u ON u.id = f.user_id
        LEFT JOIN feedback_handling h ON h.feedback_id = f.id
        LEFT JOIN admins a ON a.id = h.handled_by
        <where>
          <if test="status != null">AND COALESCE(h.status, '待处理') = #{status}</if>
          <if test="keyword != null and keyword != ''">
            AND (f.content LIKE CONCAT('%', #{keyword}, '%')
                 OR u.display_name LIKE CONCAT('%', #{keyword}, '%')
                 OR u.username LIKE CONCAT('%', #{keyword}, '%'))
          </if>
        </where>
        ORDER BY f.created_at DESC, f.id DESC
        LIMIT #{size} OFFSET #{offset}
        </script>
        """)
    List<AdminFeedbackRow> page(@Param("status") String status, @Param("keyword") String keyword,
        @Param("offset") int offset, @Param("size") int size);

    @Select("""
        <script>
        SELECT COUNT(*) FROM feedback f
        JOIN users u ON u.id = f.user_id
        LEFT JOIN feedback_handling h ON h.feedback_id = f.id
        <where>
          <if test="status != null">AND COALESCE(h.status, '待处理') = #{status}</if>
          <if test="keyword != null and keyword != ''">
            AND (f.content LIKE CONCAT('%', #{keyword}, '%')
                 OR u.display_name LIKE CONCAT('%', #{keyword}, '%')
                 OR u.username LIKE CONCAT('%', #{keyword}, '%'))
          </if>
        </where>
        </script>
        """)
    long count(@Param("status") String status, @Param("keyword") String keyword);

    @Select("""
        SELECT f.id, f.user_id, u.username AS account, u.display_name AS user_name, f.content, f.created_at,
               COALESCE(h.status, '待处理') AS status, h.reply,
               h.handled_at, a.display_name AS handled_by_name
        FROM feedback f
        JOIN users u ON u.id = f.user_id
        LEFT JOIN feedback_handling h ON h.feedback_id = f.id
        LEFT JOIN admins a ON a.id = h.handled_by
        WHERE f.id = #{id}
        """)
    AdminFeedbackRow findById(Long id);

    @Insert("""
        INSERT INTO feedback_handling (feedback_id, status, reply, handled_by, handled_at)
        VALUES (#{feedbackId}, #{status}, #{reply}, #{handledBy}, NOW(3))
        ON DUPLICATE KEY UPDATE status = #{status}, reply = #{reply},
          handled_by = #{handledBy}, handled_at = NOW(3)
        """)
    int saveHandling(@Param("feedbackId") Long feedbackId, @Param("status") String status,
        @Param("reply") String reply, @Param("handledBy") Long handledBy);
}
