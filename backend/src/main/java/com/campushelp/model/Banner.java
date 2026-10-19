// banners 表对应的数据对象；首页轮播位置、图片、标签和跳转配置。
package com.campushelp.model;

import java.time.LocalDateTime;
import lombok.Data;

// Lombok 在编译时生成 getter/setter、toString 等方法；不需要手写全部字段访问器。
@Data
// 类/接口职责：banners 表对应的数据对象；首页轮播位置、图片、标签和跳转配置。
public class Banner {
    // id：数据库自增主键，唯一标识该轮播。
    private Long id;
    // code：后台识别用的稳定编码，避免用标题做业务标识。
    private String code;
    // imageUrl：海报地址；可以是小程序包内 /assets 路径，也可以是 HTTPS 地址。
    private String imageUrl;
    // tag：展示角标，例如公告、活动、推广。
    private String tag;
    // title：图片加载失败时的兜底标题，也便于后台识别。
    private String title;
    // subtitle：兜底副标题，海报通常已包含文案。
    private String subtitle;
    // linkType：notice / page / web / miniProgram / none。
    private String linkType;
    // linkValue：公告 id、站内路径、H5 地址或目标 appId，由 linkType 决定。
    private String linkValue;
    // sortOrder：数字越小越靠前。
    private Integer sortOrder;
    // enabled：1 展示 / 0 停用。
    private Boolean enabled;
    // startAt：可选生效时间，null 表示立即生效。
    private LocalDateTime startAt;
    // endAt：可选失效时间，null 表示长期有效。
    private LocalDateTime endAt;
    // createdAt：数据库记录创建时间。
    private LocalDateTime createdAt;
}
