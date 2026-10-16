// MyBatis 用户数据库接口；Spring 创建实现，SQL 参数 #{...} 使用绑定值而不是字符串拼接。
package com.campushelp.mapper;

import com.campushelp.model.User;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
// 类/接口职责：MyBatis 用户数据库接口；Spring 创建实现，SQL 参数 #{...} 使用绑定值而不是字符串拼接。
public interface UserMapper {
    @Select("SELECT * FROM users WHERE id = #{id} AND active = 1")
    // findById：按主键读取 active=1 用户，已注销用户不作为有效身份。
    User findById(Long id);

    @Select("SELECT * FROM users WHERE username = #{username} AND active = 1")
    // findByUsername：按唯一账号查找有效用户，供登录/重置密码。
    User findByUsername(String username);

    @Select("SELECT * FROM users WHERE phone = #{phone} AND active = 1")
    // findByPhone：按绑定手机号找有效用户，供手机号登录和防重绑定。
    User findByPhone(String phone);

    @Insert("INSERT INTO users (username, password_hash, phone, display_name) VALUES (#{username}, #{passwordHash}, #{phone}, #{displayName})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    // insert：插入用户；useGeneratedKeys 把数据库生成主键写回 user.id。
    int insert(User user);

    @Update("UPDATE users SET password_hash = #{hash} WHERE id = #{id} AND active = 1")
    // updatePassword：给有效用户更新 BCrypt 密码哈希；传入 hash，不传明文。
    int updatePassword(@Param("id") Long id, @Param("hash") String hash);

    @Update("UPDATE users SET phone = #{phone} WHERE id = #{id} AND active = 1")
    // updatePhone：更新有效用户手机号；数据库唯一键防止重复绑定。
    int updatePhone(@Param("id") Long id, @Param("phone") String phone);

    @Update("UPDATE users SET active = 0, phone = NULL, display_name = '已注销用户' WHERE id = #{id} AND active = 1")
    // deactivate：停用用户并清空手机号/替换姓名，保留 id 和历史关联，不是 DELETE 用户。
    int deactivate(Long id);
}
