// 操作日志数据库接口；日志只追加，不提供修改/删除。
package com.campushelp.mapper;

import com.campushelp.audit.AdminAuditRow;
import java.util.List;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
// 类/接口职责：后台操作日志写入与分页查询。
public interface AdminAuditMapper {
    @Insert("""
        INSERT INTO admin_audit_logs (admin_id, admin_name, action, target_type, target_id, detail, ip)
        VALUES (#{adminId}, #{adminName}, #{action}, #{targetType}, #{targetId}, #{detail}, #{ip})
        """)
    int insert(@Param("adminId") Long adminId, @Param("adminName") String adminName,
        @Param("action") String action, @Param("targetType") String targetType,
        @Param("targetId") String targetId, @Param("detail") String detail, @Param("ip") String ip);

    @Select("""
        <script>
        SELECT id, admin_id, admin_name, action, target_type, target_id, detail, ip, created_at
        FROM admin_audit_logs
        <where>
          <if test="action != null">AND action = #{action}</if>
          <if test="keyword != null and keyword != ''">
            AND (admin_name LIKE CONCAT('%', #{keyword}, '%')
                 OR action LIKE CONCAT('%', #{keyword}, '%')
                 OR detail LIKE CONCAT('%', #{keyword}, '%')
                 OR target_id LIKE CONCAT('%', #{keyword}, '%'))
          </if>
        </where>
        ORDER BY created_at DESC, id DESC
        LIMIT #{size} OFFSET #{offset}
        </script>
        """)
    List<AdminAuditRow> page(@Param("action") String action, @Param("keyword") String keyword,
        @Param("offset") int offset, @Param("size") int size);

    @Select("""
        <script>
        SELECT COUNT(*) FROM admin_audit_logs
        <where>
          <if test="action != null">AND action = #{action}</if>
          <if test="keyword != null and keyword != ''">
            AND (admin_name LIKE CONCAT('%', #{keyword}, '%')
                 OR action LIKE CONCAT('%', #{keyword}, '%')
                 OR detail LIKE CONCAT('%', #{keyword}, '%')
                 OR target_id LIKE CONCAT('%', #{keyword}, '%'))
          </if>
        </where>
        </script>
        """)
    long count(@Param("action") String action, @Param("keyword") String keyword);
}
