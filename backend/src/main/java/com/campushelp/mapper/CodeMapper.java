// MyBatis 验证码数据库接口；保存 BCrypt 哈希，限制尝试次数，并通过条件更新避免验证码重复使用。
package com.campushelp.mapper;

import com.campushelp.model.VerificationCode;
import java.time.LocalDateTime;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
// 类/接口职责：MyBatis 验证码数据库接口；保存 BCrypt 哈希，限制尝试次数，并通过条件更新避免验证码重复使用。
public interface CodeMapper {
    // SQL 注解：Select 查 / Insert 新增 / Update 改 / Delete 删；修改返回影响行数，业务据此判断是否真正成功。
    @Insert("INSERT INTO verification_codes (phone, purpose, code_hash, expires_at) VALUES (#{phone}, #{purpose}, #{codeHash}, #{expiresAt})")
    // 数据库自增 id 回填给 Java 对象；创建后无需自己猜订单编号。
    @Options(useGeneratedKeys = true, keyProperty = "id")
    // insert：保存号码/用途、BCrypt 验证码哈希与期限，回填数据库主键。
    int insert(VerificationCode code);

    @Select("SELECT * FROM verification_codes WHERE phone = #{phone} AND purpose = #{purpose} ORDER BY id DESC LIMIT 1")
    // latest：按号码+用途取最新一条验证码，登录/重置/绑定不能混用。
    VerificationCode latest(@Param("phone") String phone, @Param("purpose") String purpose);

    @Update("UPDATE verification_codes SET attempts = attempts + 1 WHERE id = #{id} AND consumed_at IS NULL AND attempts < 5")
    // recordAttempt：条件增加一次尝试，达到上限或已使用不能继续；返回影响行数检查并发结果。
    int recordAttempt(Long id);

    @Update("UPDATE verification_codes SET consumed_at = #{now} WHERE id = #{id} AND consumed_at IS NULL AND attempts <= 5 AND expires_at > #{now}")
    // consume：只把未用、未过期且次数允许的验证码标记已消费，防止重放。
    int consume(@Param("id") Long id, @Param("now") LocalDateTime now);
}
