// 订单业务层；校验创建数据、转换状态、返回按身份过滤后的订单信息。
package com.campushelp.order;

import com.campushelp.mapper.OrderMapper;
import com.campushelp.mapper.UserMapper;
import com.campushelp.model.HelpOrder;
import com.campushelp.model.User;
import com.campushelp.account.AccountService;
import com.campushelp.web.ApiException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Objects;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
// 类/接口职责：订单业务层；校验创建数据、转换状态、返回按身份过滤后的订单信息。
public class OrderService {
    private final OrderMapper orders;
    private final UserMapper users;
    // json：Jackson JSON 序列化/反序列化工具；final 引用在构造后不再换成别的对象。
    private final ObjectMapper json;
    private final AccountService accounts;

    // OrderService：构造器由 Spring 注入所需依赖（测试中可手动创建）；不在每次请求中重复 new 数据库工具。
    public OrderService(OrderMapper orders, UserMapper users, ObjectMapper json, AccountService accounts) {
        this.orders = orders;
        this.users = users;
        this.json = json;
        this.accounts = accounts;
    }

    public record PackageItem(String code, String size) {}
    public record CreateOrder(String type, String station, String restaurant, List<PackageItem> packages,
    List<String> codes, String deliveryBuilding, String deliveryAddress,
    BigDecimal reward, LocalDate deadlineDate, LocalTime deadlineTime) {}
    public record OrderView(Long id, String type, String title, String station, String restaurant,
    List<PackageItem> packages, List<String> codes, int packageCount, int codeCount, String deliveryBuilding,
    String deliveryAddress, BigDecimal reward, LocalDate deadlineDate,
    LocalTime deadlineTime, String status, Long publisherId, String publisherName,
    Integer publisherCreditScore, Long runnerId, String runnerName, String myRole,
    String publisherPhone, String runnerPhone, boolean hasCompletionPhoto,
    LocalDateTime createdAt, LocalDateTime acceptedAt,
    LocalDateTime submittedAt, LocalDateTime completedAt, LocalDateTime cancelledAt) {}

    // create：校验类型、地点、取件/取餐码、金额精度/范围和未来期限；数组存 JSON，发布者从认证用户取得，插入后返回过滤视图。
    public OrderView create(Long userId, CreateOrder request) {
        if (request == null || request.type() == null || !List.of("parcel", "meal").contains(request.type())) throw bad("订单类型无效");
        if (blank(request.deliveryBuilding()) || blank(request.deliveryAddress())) throw bad("请填写送达地点");
        if (request.reward() == null || request.reward().scale() > 2 || request.reward().compareTo(BigDecimal.ZERO) <= 0
        || request.reward().compareTo(new BigDecimal("99999.99")) > 0) throw bad("报酬需在 0.01–99999.99 元之间");
        if (request.type().equals("parcel") && blank(request.station())) throw bad("请选择取件驿站");
        if (request.type().equals("meal") && blank(request.restaurant())) throw bad("请选择取餐餐厅");
        if (request.type().equals("parcel") && (request.packages() == null || request.packages().isEmpty()
        || request.packages().stream().anyMatch(p -> p == null || blank(p.code()) || p.size() == null || !List.of("小件", "中件", "大件").contains(p.size())))) {
            throw bad("取件订单需要有效取件码和包裹大小");
        }
        if (request.type().equals("meal") && (request.codes() == null || request.codes().isEmpty()
        || request.codes().stream().anyMatch(OrderService::blank))) throw bad("取餐订单需要取餐码");
        if (request.deliveryBuilding().length() > 120 || request.deliveryAddress().length() > 255) throw bad("送达地址过长");
        if (request.station() != null && request.station().length() > 120
        || request.restaurant() != null && request.restaurant().length() > 120) throw bad("取件或取餐地点过长");
        if (request.packages() != null && (request.packages().size() > 50 || request.packages().stream().anyMatch(p -> p != null && p.code() != null && p.code().length() > 100))
        || request.codes() != null && (request.codes().size() > 50 || request.codes().stream().anyMatch(c -> c != null && c.length() > 100))) throw bad("每笔订单最多 50 件，取件码最多 100 字");
        if (request.type().equals("parcel") && request.deadlineDate() == null) throw bad("请选择截止时间");
        LocalDateTime deadline = request.deadlineDate() == null ? null
        : LocalDateTime.of(request.deadlineDate(), request.deadlineTime() == null ? LocalTime.of(23, 59) : request.deadlineTime());
        if (deadline != null && !deadline.isAfter(LocalDateTime.now())) throw bad("截止时间必须晚于当前时间");
        HelpOrder order = new HelpOrder();
        order.setType(request.type());
        order.setStation(request.type().equals("parcel") ? request.station().trim() : null);
        order.setRestaurant(request.type().equals("meal") ? request.restaurant().trim() : null);
        order.setPackagesJson(request.type().equals("parcel") ? write(request.packages()) : null);
        order.setCodesJson(request.type().equals("meal") ? write(request.codes()) : null);
        order.setTitle(request.type().equals("parcel") ? parcelTitle(request.packages()) : request.codes().size() + "份餐");
        order.setDeliveryBuilding(request.deliveryBuilding().trim());
        order.setDeliveryAddress(request.deliveryAddress().trim());
        order.setReward(request.reward());
        order.setDeadlineAt(deadline);
        order.setPublisherId(userId);
        orders.insert(order);
        return detail(order.getId(), userId);
    }

    // market：按类型查询数据库中的未过期待接单，再按当前身份转换展示字段。
    public List<OrderView> market(String type, Long userId) {
        if (type == null || !List.of("parcel", "meal").contains(type)) throw bad("订单类型无效");
        return orders.market(type).stream().map(order -> view(order, userId)).toList();
    }

    // mine：查询用户发布或接取的订单，可按 type 筛选，再转成权限过滤后的视图。
    public List<OrderView> mine(Long userId, String type) {
        if (type != null && !List.of("parcel", "meal").contains(type)) throw bad("订单类型无效");
        return orders.mine(userId).stream().filter(order -> type == null || type.equals(order.getType()))
        .map(order -> view(order, userId)).toList();
    }

    // detail：订单不存在返回 404；存在则按当前用户身份过滤敏感信息。
    public OrderView detail(Long id, Long userId) {
        HelpOrder order = orders.findById(id);
        if (order == null) throw new ApiException(HttpStatus.NOT_FOUND, "订单不存在");
        return view(order, userId);
    }

    // accept：调用带状态/身份条件的 UPDATE；影响行数必须为 1，否则 409；再读取最新订单。
    public OrderView accept(Long id, Long userId) {
        changed(orders.accept(id, userId));
        return detail(id, userId);
    }

    // submitProof：仅接单者能把已接单改为待确认，并保存照片名与提交时刻；条件更新失败返回冲突。
    public OrderView submitProof(Long id, Long userId, String file) {
        changed(orders.submitProof(id, userId, file));
        return detail(id, userId);
    }

    // confirm：仅发布者确认有照片的待确认订单；变为已完成后，信用分和收入统计才能计入。
    public OrderView confirm(Long id, Long userId) {
        changed(orders.confirm(id, userId));
        return detail(id, userId);
    }

    // cancel：先尝试发布者取消待接单，再尝试接单者取消已接单；其他身份/状态返回 409。
    public OrderView cancel(Long id, Long userId) {
        if (orders.cancelByPublisher(id, userId) == 0) changed(orders.cancelByRunner(id, userId));
        return detail(id, userId);
    }

    // proofOrder：订单照片只向发布者/接单者开放；不存在或无权限统一返回 404。
    public HelpOrder proofOrder(Long id, Long userId) {
        HelpOrder order = orders.findById(id);
        if (order == null || !Objects.equals(order.getPublisherId(), userId) && !Objects.equals(order.getRunnerId(), userId)
        || order.getProofFile() == null) throw new ApiException(HttpStatus.NOT_FOUND, "照片不存在");
        return order;
    }

    // view：构造返回 DTO：游客保留数量但隐藏取件码、详细地址、电话与照片；这是后端真正的信息过滤。
    private OrderView view(HelpOrder order, Long userId) {
        User publisher = users.findById(order.getPublisherId());
        User runner = order.getRunnerId() == null ? null : users.findById(order.getRunnerId());
        LocalDateTime deadline = order.getDeadlineAt();
        String role = Objects.equals(order.getPublisherId(), userId) ? "publisher"
        : Objects.equals(order.getRunnerId(), userId) ? "runner" : "visitor";
        // 只有 publisher/runner 是参与者；返回 DTO 时基于这个值过滤敏感数据。
        boolean participant = !role.equals("visitor");
        boolean revealCodes = participant;
        boolean revealProof = participant && order.getProofFile() != null;
        List<PackageItem> packages = readPackages(order.getPackagesJson());
        List<String> codes = readCodes(order.getCodesJson());
        return new OrderView(order.getId(), order.getType(), order.getTitle(), order.getStation(), order.getRestaurant(),
        revealCodes ? packages : List.of(), revealCodes ? codes : List.of(), packages.size(), codes.size(), order.getDeliveryBuilding(),
        participant ? order.getDeliveryAddress() : null, order.getReward(), deadline == null ? null : deadline.toLocalDate(),
        deadline == null ? null : deadline.toLocalTime(), order.getStatus(), order.getPublisherId(),
        publisher == null ? "同学" : publisher.getDisplayName(),
        publisher == null ? null : accounts.creditScore(publisher.getId()),
        order.getRunnerId(), runner == null ? null : runner.getDisplayName(), role,
        participant && publisher != null ? publisher.getPhone() : null,
        participant && runner != null ? runner.getPhone() : null,
        revealProof, order.getCreatedAt(), order.getAcceptedAt(), order.getSubmittedAt(),
        order.getCompletedAt(), order.getCancelledAt());
    }

    // readPackages：将 MySQL JSON 字符串反序列化成包裹列表；null 返回空列表，损坏数据抛内部错误。
    private List<PackageItem> readPackages(String value) {
        if (value == null) return List.of();
        try {
            return json.readValue(value, json.getTypeFactory().constructCollectionType(List.class, PackageItem.class));
        }
        catch (JsonProcessingException error) {
            throw new IllegalStateException("包裹数据损坏", error);
        }
    }

    // readCodes：将 MySQL JSON 字符串还原成取餐码数组。
    private List<String> readCodes(String value) {
        if (value == null) return List.of();
        try {
            return json.readValue(value, json.getTypeFactory().constructCollectionType(List.class, String.class));
        }
        catch (JsonProcessingException error) {
            throw new IllegalStateException("取餐码数据损坏", error);
        }
    }

    // write：将 Java 对象序列化为 JSON 字符串，供数据库 JSON 列保存。
    private String write(Object value) {
        try {
            return json.writeValueAsString(value);
        }
        catch (JsonProcessingException error) {
            throw new IllegalStateException("订单数据无法序列化", error);
        }
    }

    // parcelTitle：分别统计小/中/大件数量，拼成订单标题，不直接信任前端标题。
    private static String parcelTitle(List<PackageItem> items) {
        return List.of("小件", "中件", "大件").stream().map(size -> {
            long count = items.stream().filter(item -> size.equals(item.size())).count();
            return count == 0 ? "" : size + count + "个";
        }).filter(s -> !s.isEmpty()).reduce((a, b) -> a + "、" + b).orElse("取件订单");
    }

    // changed：检查 UPDATE 影响行数为 1；0 表示状态/身份条件不满足，返回 409 提醒刷新。
    private static void changed(int affected) {
        if (affected != 1) throw new ApiException(HttpStatus.CONFLICT, "订单状态已变化，请刷新");
    }

    // blank：统一判断 null 或全为空白字符串，避免漏掉必填校验。
    private static boolean blank(String value) {
        return value == null || value.isBlank();
    }
    private static ApiException bad(String message) {
        return new ApiException(HttpStatus.BAD_REQUEST, message);
    }
}
