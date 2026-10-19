// 财务只读查询；账单由订单状态推导，不修改订单或资金。
package com.campushelp.mapper;

import com.campushelp.admin.AdminFinanceRow;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
// 类/接口职责：财务汇总和演示账单分页。
public interface AdminFinanceMapper {
    @Select("""
        SELECT COUNT(*) AS total_orders,
               SUM(CASE WHEN status = '已完成' THEN 1 ELSE 0 END) AS completed_orders,
               SUM(CASE WHEN status = '已取消' THEN 1 ELSE 0 END) AS cancelled_orders,
               COALESCE(SUM(reward), 0) AS published_amount,
               COALESCE(SUM(CASE WHEN status = '已完成' THEN reward ELSE 0 END), 0) AS completed_amount,
               COALESCE(SUM(CASE WHEN status = '已取消' THEN reward ELSE 0 END), 0) AS refunded_amount
        FROM orders
        """)
    AdminFinanceRow.Summary summary();

    @Select("""
        <script>
        SELECT * FROM (
          SELECT CONCAT('publish-', o.id) AS id, o.id AS order_id, 'publish' AS kind,
                 CONCAT('发布', IF(o.type = 'meal', '取餐', '取件'), '订单：', o.title) AS title,
                 -o.reward AS amount, o.created_at AS occurred_at, '成功' AS status
          FROM orders o
          UNION ALL
          SELECT CONCAT('refund-', o.id), o.id, 'refund',
                 CONCAT('取消退款：', o.title), o.reward, o.cancelled_at, '成功'
          FROM orders o
          WHERE o.status = '已取消' AND o.cancelled_at IS NOT NULL
          UNION ALL
          SELECT CONCAT('income-', o.id), o.id, 'income',
                 CONCAT('完成收入：', o.title), o.reward, o.completed_at, '成功'
          FROM orders o
          WHERE o.status = '已完成' AND o.completed_at IS NOT NULL
        ) ledger
        <where>
          <if test="kind != null">AND ledger.kind = #{kind}</if>
          <if test="keyword != null and keyword != ''">
            AND (ledger.title LIKE CONCAT('%', #{keyword}, '%')
                 OR CAST(ledger.order_id AS CHAR) LIKE CONCAT('%', #{keyword}, '%'))
          </if>
        </where>
        ORDER BY ledger.occurred_at DESC, ledger.order_id DESC, ledger.kind
        LIMIT #{size} OFFSET #{offset}
        </script>
        """)
    List<AdminFinanceRow.Entry> ledger(@Param("kind") String kind, @Param("keyword") String keyword,
        @Param("offset") int offset, @Param("size") int size);

    @Select("""
        <script>
        SELECT COUNT(*) FROM (
          SELECT CONCAT('publish-', o.id) AS id, o.id AS order_id, 'publish' AS kind,
                 CONCAT('发布', IF(o.type = 'meal', '取餐', '取件'), '订单：', o.title) AS title,
                 -o.reward AS amount, o.created_at AS occurred_at
          FROM orders o
          UNION ALL
          SELECT CONCAT('refund-', o.id), o.id, 'refund',
                 CONCAT('取消退款：', o.title), o.reward, o.cancelled_at
          FROM orders o WHERE o.status = '已取消' AND o.cancelled_at IS NOT NULL
          UNION ALL
          SELECT CONCAT('income-', o.id), o.id, 'income',
                 CONCAT('完成收入：', o.title), o.reward, o.completed_at
          FROM orders o WHERE o.status = '已完成' AND o.completed_at IS NOT NULL
        ) ledger
        <where>
          <if test="kind != null">AND ledger.kind = #{kind}</if>
          <if test="keyword != null and keyword != ''">
            AND (ledger.title LIKE CONCAT('%', #{keyword}, '%')
                 OR CAST(ledger.order_id AS CHAR) LIKE CONCAT('%', #{keyword}, '%'))
          </if>
        </where>
        </script>
        """)
    long count(@Param("kind") String kind, @Param("keyword") String keyword);
}
