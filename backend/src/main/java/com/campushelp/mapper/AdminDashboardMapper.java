// 数据看板聚合查询；只读，不修改任何业务表。
package com.campushelp.mapper;

import com.campushelp.admin.DashboardRow;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
// 类/接口职责：后台首页统计所需的计数、按日趋势和最近记录查询。
public interface AdminDashboardMapper {
    @Select("SELECT COUNT(*) FROM users")
    long countUsers();

    @Select("SELECT COUNT(*) FROM users WHERE active = 1")
    long countActiveUsers();

    @Select("SELECT COUNT(*) FROM users WHERE created_at >= CURDATE()")
    long countUsersToday();

    @Select("SELECT COUNT(*) FROM orders")
    long countOrders();

    @Select("SELECT COUNT(*) FROM orders WHERE created_at >= CURDATE()")
    long countOrdersToday();

    @Select("SELECT COUNT(*) FROM orders WHERE status IN ('待接单', '已接单', '待确认')")
    long countOpenOrders();

    @Select("SELECT COUNT(*) FROM orders WHERE status = '已完成'")
    long countCompletedOrders();

    @Select("SELECT COUNT(*) FROM feedback")
    long countFeedback();

    @Select("SELECT COALESCE(SUM(reward), 0) FROM orders WHERE status = '已完成'")
    BigDecimal completedAmount();

    @Select("SELECT status, COUNT(*) AS total FROM orders GROUP BY status")
    List<DashboardRow.Status> countOrdersByStatus();

    @Select("""
        SELECT DATE(created_at) AS day, COUNT(*) AS total
        FROM users
        WHERE created_at >= #{from}
        GROUP BY DATE(created_at)
        ORDER BY day
        """)
    List<DashboardRow.UserDay> countUsersByDay(@Param("from") LocalDateTime from);

    @Select("""
        SELECT DATE(created_at) AS day,
               COUNT(*) AS total,
               SUM(CASE WHEN status = '已完成' THEN 1 ELSE 0 END) AS completed,
               COALESCE(SUM(CASE WHEN status = '已完成' THEN reward ELSE 0 END), 0) AS amount
        FROM orders
        WHERE created_at >= #{from}
        GROUP BY DATE(created_at)
        ORDER BY day
        """)
    List<DashboardRow.OrderDay> countOrdersByDay(@Param("from") LocalDateTime from);

    @Select("""
        SELECT o.id, o.type, o.title, o.status, o.reward, o.created_at,
               p.id AS publisher_id, p.display_name AS publisher_name,
               r.id AS runner_id, r.display_name AS runner_name
        FROM orders o
        JOIN users p ON p.id = o.publisher_id
        LEFT JOIN users r ON r.id = o.runner_id
        ORDER BY o.created_at DESC, o.id DESC
        LIMIT 6
        """)
    List<DashboardRow.RecentOrder> recentOrders();

    @Select("""
        SELECT f.id, f.content, f.created_at, u.display_name AS user_name
        FROM feedback f
        JOIN users u ON u.id = f.user_id
        ORDER BY f.created_at DESC, f.id DESC
        LIMIT 5
        """)
    List<DashboardRow.RecentFeedback> recentFeedback();
}
