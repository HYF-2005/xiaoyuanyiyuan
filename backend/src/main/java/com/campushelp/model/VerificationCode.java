// verification_codes 表对象；验证码存 BCrypt 哈希，区分用途、过期、已使用与尝试次数。
package com.campushelp.model;

import java.time.LocalDateTime;
import lombok.Data;

@Data
// 类/接口职责：verification_codes 表对象；验证码存 BCrypt 哈希，区分用途、过期、已使用与尝试次数。
public class VerificationCode {
    private Long id;
    private String phone;
    // purpose：验证码用途 login 登录 / reset 重置密码 / bind 绑定号码
    private String purpose;
    // codeHash：验证码 BCrypt 哈希，不是 token 的 SHA-256
    private String codeHash;
    // expiresAt：验证码有效期限
    private LocalDateTime expiresAt;
    // consumedAt：已使用时刻，null 表示尚未消费
    private LocalDateTime consumedAt;
    // attempts：验证码累计尝试次数，上限由业务和条件 SQL 共同限制
    private Integer attempts;
    private LocalDateTime createdAt;
}
