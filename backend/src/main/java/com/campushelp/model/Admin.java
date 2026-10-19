// 后台管理员表对应的 Java 数据对象；密码只保存 BCrypt 哈希，接口不返回该字段。
package com.campushelp.model;

import java.time.LocalDateTime;
import lombok.Data;

@Data
// 类/接口职责：admins 表映射对象；后台管理员与小程序普通用户是两套账号体系。
public class Admin {
    private Long id;
    // username：后台登录账号，admins 表唯一。
    private String username;
    // passwordHash：BCrypt 密码哈希，不是明文密码。
    private String passwordHash;
    // displayName：后台顶部栏展示名称，同时作为管理员真实姓名。
    private String displayName;
    // phone：管理员联系电话，仅供后台联系，不参与登录。
    private String phone;
    // email：管理员邮箱，仅供后台联系，不参与登录。
    private String email;
    // role：super_admin / operator / finance / content，后续用于权限控制。
    private String role;
    // active：1 可登录 / 0 已停用。
    private Boolean active;
    // remark：管理员备注，用于交接、权限说明等。
    private String remark;
    // permissionCodes：自定义菜单权限 JSON；为空时使用角色默认权限。
    private String permissionCodes;
    private LocalDateTime createdAt;
    private LocalDateTime lastLoginAt;
}
