// MyBatis 反馈写入接口；正文和用户 id 通过绑定参数写数据库。
package com.campushelp.mapper;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
// 类/接口职责：MyBatis 反馈写入接口；正文和用户 id 通过绑定参数写数据库。
public interface FeedbackMapper {
    @Insert("INSERT INTO feedback (user_id, content) VALUES (#{userId}, #{content})")
    // insert：将认证用户 id 与反馈正文写入反馈表。
    int insert(@Param("userId") Long userId, @Param("content") String content);
}
