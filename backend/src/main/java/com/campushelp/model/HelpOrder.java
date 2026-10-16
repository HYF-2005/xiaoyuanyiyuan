// orders 表对应的数据对象；包括取件/取餐资料、金额、状态、参与者和各阶段时间。
package com.campushelp.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;

// Lombok 在编译时生成 getter/setter、toString 等方法；不需要手写全部字段访问器。
@Data
// 类/接口职责：orders 表对应的数据对象；包括取件/取餐资料、金额、状态、参与者和各阶段时间。
public class HelpOrder {
    // id：数据库自增主键，唯一标识该记录
    private Long id;
    // type：parcel 取件 / meal 取餐
    private String type;
    // title：服务器生成的订单标题
    private String title;
    // station：取件驿站，仅取件订单使用
    private String station;
    // restaurant：食堂/楼层/档口，仅取餐订单使用
    private String restaurant;
    // packagesJson：包裹列表 JSON 字符串，包含取件码和大小
    private String packagesJson;
    // codesJson：取餐码数组 JSON 字符串
    private String codesJson;
    // deliveryBuilding：送达建筑
    private String deliveryBuilding;
    // deliveryAddress：送达详细地址，游客返回时隐藏
    private String deliveryAddress;
    // reward：报酬金额，BigDecimal 对应 DECIMAL(10,2)，不是 double 浮点金额
    private BigDecimal reward;
    // deadlineAt：完整截止时刻；取餐可为 null
    private LocalDateTime deadlineAt;
    // status：待接单 → 已接单 → 待确认 → 已完成，或已取消
    private String status;
    // publisherId：发布者用户主键
    private Long publisherId;
    // runnerId：接单者用户主键，尚未接单为 null
    private Long runnerId;
    // proofFile：服务器保存的 JPEG 文件名，不接收用户任意路径
    private String proofFile;
    // createdAt：数据库记录创建时间
    private LocalDateTime createdAt;
    // acceptedAt：接单时刻
    private LocalDateTime acceptedAt;
    // submittedAt：上传完成凭证时刻，用于是否按时判断
    private LocalDateTime submittedAt;
    // completedAt：发布者确认完成时刻
    private LocalDateTime completedAt;
    // cancelledAt：取消时刻
    private LocalDateTime cancelledAt;
    // cancelledBy：取消操作者用户主键，决定是否是接单者取消
    private Long cancelledBy;
}
