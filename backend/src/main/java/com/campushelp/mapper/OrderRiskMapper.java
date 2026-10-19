// 风险订单扫描、队列查询和人工审核；扫描结果写入 order_risks，原订单数据不被修改。
package com.campushelp.mapper;

import com.campushelp.admin.OrderRiskRow;
import java.util.List;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
// 类/接口职责：后台风险订单的规则信号读取、快照 upsert、分页/汇总和审核写入。
public interface OrderRiskMapper {
    @Select("""
        SELECT o.id, o.type, o.title, o.status, o.reward, o.publisher_id, o.runner_id,
               o.proof_file, o.cancelled_by, o.created_at, o.accepted_at, o.submitted_at,
               o.completed_at, o.cancelled_at,
               p.created_at AS publisher_created_at,
               r.created_at AS runner_created_at
        FROM orders o
        JOIN users p ON p.id = o.publisher_id
        LEFT JOIN users r ON r.id = o.runner_id
        ORDER BY o.created_at, o.id
        """)
    List<OrderRiskRow.Signal> signals();

    @Insert("""
        INSERT INTO order_risks (order_id, risk_score, risk_level, risk_flags, scanned_at)
        VALUES (#{orderId}, #{score}, #{level}, CAST(#{flags} AS JSON), NOW(3))
        ON DUPLICATE KEY UPDATE
          risk_score = VALUES(risk_score),
          risk_level = VALUES(risk_level),
          risk_flags = VALUES(risk_flags),
          scanned_at = VALUES(scanned_at)
        """)
    int upsert(
        @Param("orderId") Long orderId,
        @Param("score") int score,
        @Param("level") String level,
        @Param("flags") String flags
    );

    @Select("""
        <script>
        SELECT r.order_id, r.risk_score, r.risk_level, r.risk_flags, r.review_status,
               r.review_note, r.reviewed_at, r.scanned_at, a.display_name AS reviewer_name,
               o.type, o.title, o.status, o.reward, o.created_at,
               p.id AS publisher_id, p.display_name AS publisher_name,
               rn.id AS runner_id, rn.display_name AS runner_name
        FROM order_risks r
        JOIN orders o ON o.id = r.order_id
        JOIN users p ON p.id = o.publisher_id
        LEFT JOIN users rn ON rn.id = o.runner_id
        LEFT JOIN admins a ON a.id = r.reviewed_by
        WHERE r.risk_level &lt;&gt; 'none'
          <if test="level != null and level != '' and level != 'all'">AND r.risk_level = #{level}</if>
          <if test="reviewStatus != null and reviewStatus != '' and reviewStatus != 'all'">AND r.review_status = #{reviewStatus}</if>
          <if test="keyword != null and keyword != ''">
            AND (CAST(o.id AS CHAR) LIKE CONCAT('%', #{keyword}, '%')
                 OR o.title LIKE CONCAT('%', #{keyword}, '%')
                 OR p.display_name LIKE CONCAT('%', #{keyword}, '%')
                 OR rn.display_name LIKE CONCAT('%', #{keyword}, '%'))
          </if>
        ORDER BY CASE r.risk_level WHEN 'high' THEN 0 WHEN 'medium' THEN 1 ELSE 2 END,
                 r.risk_score DESC, o.created_at DESC, o.id DESC
        LIMIT #{size} OFFSET #{offset}
        </script>
        """)
    List<OrderRiskRow.Risk> page(
        @Param("keyword") String keyword,
        @Param("level") String level,
        @Param("reviewStatus") String reviewStatus,
        @Param("offset") int offset,
        @Param("size") int size
    );

    @Select("""
        <script>
        SELECT COUNT(*)
        FROM order_risks r
        JOIN orders o ON o.id = r.order_id
        JOIN users p ON p.id = o.publisher_id
        LEFT JOIN users rn ON rn.id = o.runner_id
        WHERE r.risk_level &lt;&gt; 'none'
          <if test="level != null and level != '' and level != 'all'">AND r.risk_level = #{level}</if>
          <if test="reviewStatus != null and reviewStatus != '' and reviewStatus != 'all'">AND r.review_status = #{reviewStatus}</if>
          <if test="keyword != null and keyword != ''">
            AND (CAST(o.id AS CHAR) LIKE CONCAT('%', #{keyword}, '%')
                 OR o.title LIKE CONCAT('%', #{keyword}, '%')
                 OR p.display_name LIKE CONCAT('%', #{keyword}, '%')
                 OR rn.display_name LIKE CONCAT('%', #{keyword}, '%'))
          </if>
        </script>
        """)
    long count(
        @Param("keyword") String keyword,
        @Param("level") String level,
        @Param("reviewStatus") String reviewStatus
    );

    @Select("""
        SELECT COUNT(*) AS total,
               COUNT(CASE WHEN risk_level = 'high' THEN 1 END) AS high,
               COUNT(CASE WHEN risk_level = 'medium' THEN 1 END) AS medium,
               COUNT(CASE WHEN risk_level = 'low' THEN 1 END) AS low,
               COUNT(CASE WHEN review_status = '待审核' THEN 1 END) AS pending,
               COUNT(CASE WHEN review_status = '已放行' THEN 1 END) AS cleared,
               COUNT(CASE WHEN review_status = '已确认风险' THEN 1 END) AS confirmed
        FROM order_risks
        WHERE risk_level <> 'none'
        """)
    OrderRiskRow.Summary summary();

    @Select("""
        SELECT r.order_id, r.risk_score, r.risk_level, r.risk_flags, r.review_status,
               r.review_note, r.reviewed_at, r.scanned_at, a.display_name AS reviewer_name,
               o.type, o.title, o.status, o.reward, o.created_at,
               p.id AS publisher_id, p.display_name AS publisher_name,
               rn.id AS runner_id, rn.display_name AS runner_name
        FROM order_risks r
        JOIN orders o ON o.id = r.order_id
        JOIN users p ON p.id = o.publisher_id
        LEFT JOIN users rn ON rn.id = o.runner_id
        LEFT JOIN admins a ON a.id = r.reviewed_by
        WHERE r.order_id = #{orderId}
        """)
    OrderRiskRow.Risk findItem(@Param("orderId") Long orderId);

    @Update("""
        UPDATE order_risks
        SET review_status = #{status}, review_note = #{note},
            reviewed_by = #{adminId}, reviewed_at = NOW(3)
        WHERE order_id = #{orderId}
        """)
    int review(
        @Param("orderId") Long orderId,
        @Param("status") String status,
        @Param("note") String note,
        @Param("adminId") Long adminId
    );
}
