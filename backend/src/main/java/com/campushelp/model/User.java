// users 表对应的 Java 数据对象；Lombok @Data 自动生成 getter/setter 等方法，不直接返回密码哈希给前端。
package com.campushelp.model;

import java.time.LocalDateTime;
import lombok.Data;

@Data
// 类/接口职责：users 表对应的 Java 数据对象；Lombok @Data 自动生成 getter/setter 等方法，不直接返回密码哈希给前端。
public class User {
    private Long id;
    // username：登录账号，users 表唯一
    private String username;
    // passwordHash：BCrypt 密码哈希，不是明文密码
    private String passwordHash;
    // phone：用户绑定手机号/验证码目标号码
    private String phone;
    // displayName：界面展示姓名
    private String displayName;
    // creditBase：基础信用分，数据库默认 61；业务层叠加订单事件
    private Integer creditBase;
    // active：账号有效标记，1 正常 / 0 已停用
    private Boolean active;
    private LocalDateTime createdAt;
}
