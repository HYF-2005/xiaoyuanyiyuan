// 后台用户反馈行对象；处理状态来自 feedback_handling。
package com.campushelp.admin;

import java.time.LocalDateTime;
import lombok.Data;

@Data
public class AdminFeedbackRow {
    private Long id;
    private Long userId;
    /** 关联订单主键；普通意见反馈为 null，订单问题反馈非空。 */
    private Long orderId;
    private String account;
    private String userName;
    private String content;
    private String status;
    private String reply;
    private String handledByName;
    private LocalDateTime handledAt;
    private LocalDateTime createdAt;
}
