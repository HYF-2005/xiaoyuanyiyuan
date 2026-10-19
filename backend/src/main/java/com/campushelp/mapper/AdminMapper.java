// 后台管理员数据库接口；登录会话只保存 token 的 SHA-256 哈希。
package com.campushelp.mapper;

import com.campushelp.model.Admin;
import java.time.LocalDateTime;
import java.util.List;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
// 类/接口职责：后台管理员与管理员会话的数据库访问；不直接暴露密码哈希给 Controller。
public interface AdminMapper {
    @Select("SELECT * FROM admins WHERE username = #{username} LIMIT 1")
    // findByUsername：登录时按账号读取管理员，password_hash 仅供 Service 做 BCrypt 校验。
    Admin findByUsername(String username);

    @Select("SELECT * FROM admins WHERE id = #{id}")
    Admin findById(Long id);

    @Insert("""
        INSERT INTO admins (username, password_hash, display_name, phone, email, role, active, remark, permission_codes)
        VALUES (#{username}, #{passwordHash}, #{displayName}, #{phone}, #{email}, #{role}, #{active}, #{remark}, #{permissionCodes})
        """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    // insert：创建管理员；初始密码由调用方完成 BCrypt 编码。
    int insert(Admin admin);

    @Select("""
        SELECT a.* FROM admin_sessions s
        JOIN admins a ON a.id = s.admin_id
        WHERE s.token_hash = #{hash}
          AND s.expires_at > NOW(3)
          AND a.active = 1
        LIMIT 1
        """)
    // findByTokenHash：联表读取未过期且仍启用的管理员会话。
    Admin findByTokenHash(String hash);

    @Insert("INSERT INTO admin_sessions (admin_id, token_hash, expires_at) VALUES (#{adminId}, #{hash}, #{expiresAt})")
    // insertSession：只保存 token 哈希和过期时间，明文 token 仅返回给浏览器。
    int insertSession(@Param("adminId") Long adminId, @Param("hash") String hash,
        @Param("expiresAt") LocalDateTime expiresAt);

    @Delete("DELETE FROM admin_sessions WHERE token_hash = #{hash}")
    // deleteSession：退出登录时删除当前后台会话。
    int deleteSession(String hash);

    @Update("UPDATE admins SET last_login_at = NOW(3) WHERE id = #{id}")
    // touchLastLogin：登录成功后记录最近登录时间。
    int touchLastLogin(Long id);

    @Select("SELECT * FROM admins ORDER BY id ASC")
    List<Admin> findAll();

    @Select("SELECT COUNT(*) FROM admins WHERE role = 'super_admin' AND active = 1 AND id <> #{excludeId}")
    int countOtherActiveSuperAdmins(@Param("excludeId") Long excludeId);

    @Update("""
        UPDATE admins
        SET display_name = #{displayName}, phone = #{phone}, email = #{email},
            role = #{role}, remark = #{remark}
        WHERE id = #{id}
        """)
    int updateProfile(@Param("id") Long id, @Param("displayName") String displayName,
        @Param("phone") String phone, @Param("email") String email,
        @Param("role") String role, @Param("remark") String remark);

    @Update("UPDATE admins SET permission_codes = #{permissionCodes} WHERE id = #{id}")
    int updatePermissions(@Param("id") Long id, @Param("permissionCodes") String permissionCodes);

    @Update("UPDATE admins SET active = #{active} WHERE id = #{id}")
    int updateStatus(@Param("id") Long id, @Param("active") boolean active);

    @Update("UPDATE admins SET password_hash = #{passwordHash} WHERE id = #{id}")
    int updatePassword(@Param("id") Long id, @Param("passwordHash") String passwordHash);

    @Delete("DELETE FROM admin_sessions WHERE admin_id = #{adminId}")
    int deleteSessionsByAdminId(Long adminId);
}
