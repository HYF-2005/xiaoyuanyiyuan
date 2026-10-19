// announcements 表对应的数据对象；公告正文、分类、重要级别和强提醒标记。
package com.campushelp.model;

import java.time.LocalDateTime;
import lombok.Data;

// Lombok 在编译时生成 getter/setter、toString 等方法；不需要手写全部字段访问器。
@Data
// 类/接口职责：announcements 表对应的数据对象；公告正文、分类、重要级别和强提醒标记。
public class Announcement {
    // id：数据库自增主键，唯一标识该公告。
    private Long id;
    // title：公告标题，列表和弹窗共用。
    private String title;
    // summary：列表摘要；为空时由业务层从正文截取。
    private String summary;
    // content：公告正文，纯文本保留换行，前端不做 HTML 注入。
    private String content;
    // category：系统公告 / 活动。
    private String category;
    // level：普通 / 重要 / 紧急，用于列表和弹窗角标。
    private String level;
    // pinned：1 置顶，排序优先于发布时间。
    private Boolean pinned;
    // forceRead：1 登录后有未读时自动弹出，并执行 5 秒强制阅读。
    private Boolean forceRead;
    // isPublished：1 已发布 / 0 草稿。
    private Boolean isPublished;
    // publishedAt：发布时间；未来时间可做定时发布。
    private LocalDateTime publishedAt;
    // createdAt：数据库记录创建时间。
    private LocalDateTime createdAt;
    // isRead：查询时由 EXISTS 子查询计算，不是 announcements 表的真实列。
    private Boolean isRead;
}
