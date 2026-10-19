// 后台用户管理查询；动态筛选使用 MyBatis 绑定参数，不拼接用户输入。
package com.campushelp.mapper;

import com.campushelp.admin.AdminUserRow;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
// 类/接口职责：用户分页、详情、订单计数和账号启停的数据库访问。
public interface AdminUserMapper {
    @Select("""
        <script>
        SELECT u.id, u.username, u.display_name, u.phone, u.college, u.major, u.grade,
               u.class_name, u.dormitory, u.credit_base, u.active, u.created_at,
               (SELECT COUNT(*) FROM orders o WHERE o.publisher_id = u.id) AS published_orders,
               (SELECT COUNT(*) FROM orders o WHERE o.runner_id = u.id) AS accepted_orders,
               (SELECT COUNT(*) FROM orders o
                WHERE (o.publisher_id = u.id OR o.runner_id = u.id)
                  AND o.status IN ('待接单', '已接单', '待确认')) AS open_orders
        FROM users u
        <where>
          <if test="keyword != null and keyword != ''">
            AND (u.username LIKE CONCAT('%', #{keyword}, '%')
                 OR u.display_name LIKE CONCAT('%', #{keyword}, '%')
                 OR u.phone LIKE CONCAT('%', #{keyword}, '%'))
          </if>
          <if test="active != null">AND u.active = #{active}</if>
        </where>
        ORDER BY u.created_at DESC, u.id DESC
        LIMIT #{size} OFFSET #{offset}
        </script>
        """)
    List<AdminUserRow.User> page(
        @Param("keyword") String keyword,
        @Param("active") Integer active,
        @Param("offset") int offset,
        @Param("size") int size
    );

    @Select("""
        <script>
        SELECT COUNT(*) FROM users u
        <where>
          <if test="keyword != null and keyword != ''">
            AND (u.username LIKE CONCAT('%', #{keyword}, '%')
                 OR u.display_name LIKE CONCAT('%', #{keyword}, '%')
                 OR u.phone LIKE CONCAT('%', #{keyword}, '%'))
          </if>
          <if test="active != null">AND u.active = #{active}</if>
        </where>
        </script>
        """)
    long count(@Param("keyword") String keyword, @Param("active") Integer active);

    @Select("""
        SELECT u.id, u.username, u.display_name, u.phone, u.college, u.major, u.grade,
               u.class_name, u.dormitory, u.credit_base, u.active, u.created_at,
               (SELECT COUNT(*) FROM orders o WHERE o.publisher_id = u.id) AS published_orders,
               (SELECT COUNT(*) FROM orders o WHERE o.runner_id = u.id) AS accepted_orders,
               (SELECT COUNT(*) FROM orders o
                WHERE (o.publisher_id = u.id OR o.runner_id = u.id)
                  AND o.status IN ('待接单', '已接单', '待确认')) AS open_orders
        FROM users u
        WHERE u.id = #{id}
        """)
    AdminUserRow.User findById(Long id);

    @Select("""
        SELECT o.id, o.type, o.title, o.status, o.reward, o.created_at,
               CASE WHEN o.publisher_id = #{id} THEN 'publisher' ELSE 'runner' END AS role
        FROM orders o
        WHERE o.publisher_id = #{id} OR o.runner_id = #{id}
        ORDER BY o.created_at DESC, o.id DESC
        LIMIT 5
        """)
    List<AdminUserRow.Order> recentOrders(Long id);

    @Update("UPDATE users SET active = #{active} WHERE id = #{id}")
    // updateActive：只改变登录状态，不清空手机号、姓名或历史订单。
    int updateActive(@Param("id") Long id, @Param("active") boolean active);
}
