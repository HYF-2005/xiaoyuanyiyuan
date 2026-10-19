// 后台用户反馈行对象；处理状态来自 feedback_handling。
package com.campushelp.admin;

import java.time.LocalDateTime;
import lombok.Data;

@Data
public class AdminFeedbackRow {
    private Long id;
    private Long userId;
    private String account;
    private String userName;
    private String content;
    private String status;
    private String reply;
    private String handledByName;
    private LocalDateTime handledAt;
    private LocalDateTime createdAt;
}
