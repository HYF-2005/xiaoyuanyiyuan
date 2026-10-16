// MyBatis 登录会话数据库接口；只保存 token 的 SHA-256 哈希与过期时间，不存明文 token。
package com.campushelp.mapper;

import com.campushelp.model.User;
import java.time.LocalDateTime;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
// 类/接口职责：MyBatis 登录会话数据库接口；只保存 token 的 SHA-256 哈希与过期时间，不存明文 token。
public interface SessionMapper {
    @Insert("INSERT INTO login_sessions (user_id, token_hash, expires_at) VALUES (#{userId}, #{hash}, #{expiresAt})")
    // insert：保存用户 id、token SHA-256 哈希和有效期限。
    int insert(@Param("userId") Long userId, @Param("hash") String hash, @Param("expiresAt") LocalDateTime expiresAt);

    @Select("SELECT u.* FROM login_sessions s JOIN users u ON u.id = s.user_id WHERE s.token_hash = #{hash} AND s.expires_at > NOW(3) AND u.active = 1")
    // findUserByHash：联表读取未过期且 active=1 的会话用户。
    User findUserByHash(String hash);

    @Delete("DELETE FROM login_sessions WHERE token_hash = #{hash}")
    // deleteByHash：删除单个 token 会话，用于退出本次登录。
    int deleteByHash(String hash);

    @Delete("DELETE FROM login_sessions WHERE user_id = #{userId}")
    // deleteByUserId：删除该用户全部会话，密码修改和账号注销后旧令牌失效。
    int deleteByUserId(Long userId);
}
