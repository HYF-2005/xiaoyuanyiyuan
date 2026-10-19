// 后台订单查询与异常处理；取消只允许未结束订单，强制完成只允许已上传凭证的待确认订单。
package com.campushelp.mapper;

import com.campushelp.admin.AdminOrderRow;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
// 类/接口职责：后台订单分页、详情、凭证文件名和异常状态更新。
public interface AdminOrderMapper {
    @Select("""
        <script>
        SELECT o.id, o.type, o.title, o.status, o.reward, o.created_at, o.deadline_at,
               p.id AS publisher_id, p.display_name AS publisher_name,
               r.id AS runner_id, r.display_name AS runner_name
        FROM orders o
        JOIN users p ON p.id = o.publisher_id
        LEFT JOIN users r ON r.id = o.runner_id
        <where>
          <if test="status != null">AND o.status = #{status}</if>
          <if test="type != null">AND o.type = #{type}</if>
          <if test="keyword != null and keyword != ''">
            AND (CAST(o.id AS CHAR) LIKE CONCAT('%', #{keyword}, '%')
                 OR o.title LIKE CONCAT('%', #{keyword}, '%')
                 OR p.display_name LIKE CONCAT('%', #{keyword}, '%')
                 OR r.display_name LIKE CONCAT('%', #{keyword}, '%'))
          </if>
        </where>
        ORDER BY o.created_at DESC, o.id DESC
        LIMIT #{size} OFFSET #{offset}
        </script>
        """)
    List<AdminOrderRow.Order> page(
        @Param("keyword") String keyword,
        @Param("status") String status,
        @Param("type") String type,
        @Param("offset") int offset,
        @Param("size") int size
    );

    @Select("""
        <script>
        SELECT COUNT(*)
        FROM orders o
        JOIN users p ON p.id = o.publisher_id
        LEFT JOIN users r ON r.id = o.runner_id
        <where>
          <if test="status != null">AND o.status = #{status}</if>
          <if test="type != null">AND o.type = #{type}</if>
          <if test="keyword != null and keyword != ''">
            AND (CAST(o.id AS CHAR) LIKE CONCAT('%', #{keyword}, '%')
                 OR o.title LIKE CONCAT('%', #{keyword}, '%')
                 OR p.display_name LIKE CONCAT('%', #{keyword}, '%')
                 OR r.display_name LIKE CONCAT('%', #{keyword}, '%'))
          </if>
        </where>
        </script>
        """)
    long count(
        @Param("keyword") String keyword,
        @Param("status") String status,
        @Param("type") String type
    );

    @Select("""
        SELECT o.id, o.type, o.title, o.station, o.restaurant,
               o.delivery_building, o.delivery_address, o.reward, o.status,
               o.publisher_id, p.display_name AS publisher_name, p.phone AS publisher_phone,
               o.runner_id, r.display_name AS runner_name, r.phone AS runner_phone,
               JSON_LENGTH(o.packages_json) AS package_count,
               JSON_LENGTH(o.codes_json) AS code_count,
               (o.proof_file IS NOT NULL) AS has_proof,
               o.created_at, o.deadline_at, o.accepted_at, o.submitted_at,
               o.completed_at, o.cancelled_at, c.display_name AS cancelled_by_name
        FROM orders o
        JOIN users p ON p.id = o.publisher_id
        LEFT JOIN users r ON r.id = o.runner_id
        LEFT JOIN users c ON c.id = o.cancelled_by
        WHERE o.id = #{id}
        """)
    AdminOrderRow.Order findById(Long id);

    @Select("SELECT proof_file FROM orders WHERE id = #{id}")
    String proofFile(Long id);

    @Update("""
        UPDATE orders
        SET status = '已取消', cancelled_at = NOW(3), cancelled_by = NULL
        WHERE id = #{id} AND status IN ('待接单', '已接单', '待确认')
        """)
    int cancel(Long id);

    @Update("""
        UPDATE orders
        SET status = '已完成', completed_at = NOW(3)
        WHERE id = #{id} AND status = '待确认' AND proof_file IS NOT NULL
        """)
    int forceComplete(Long id);
}
