// agreements 表对象：服务协议/隐私政策等文本内容。
package com.campushelp.model;

import java.time.LocalDateTime;
import lombok.Data;

@Data
public class Agreement {
    private Long id;
    private String code;
    private String title;
    private String version;
    private String content;
    private Boolean isPublished;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
