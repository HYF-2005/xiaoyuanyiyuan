// MyBatis 反馈写入接口；正文、用户 id 和可选的关联订单通过绑定参数写数据库。
package com.campushelp.mapper;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
// 类/接口职责：MyBatis 反馈写入接口；正文、用户 id 和可选的关联订单通过绑定参数写数据库。
public interface FeedbackMapper {
    @Insert("INSERT INTO feedback (user_id, order_id, content) VALUES (#{userId}, #{orderId}, #{content})")
    // insert：写入反馈；orderId 为空表示普通意见反馈，非空表示针对某笔订单的反馈。
    int insert(@Param("userId") Long userId, @Param("orderId") Long orderId,
        @Param("content") String content);
}
