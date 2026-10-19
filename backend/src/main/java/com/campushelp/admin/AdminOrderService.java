// 后台订单管理业务；列表、详情、取消和强制完成都通过订单主键操作。
package com.campushelp.admin;

import com.campushelp.mapper.AdminOrderMapper;
import com.campushelp.web.ApiException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
// 类/接口职责：后台订单异常处理；不暴露用户取件码原文，列表只给参与者摘要。
public class AdminOrderService {
    private static final Set<String> STATUSES = Set.of("待接单", "已接单", "待确认", "已完成", "已取消");
    private static final Set<String> OPEN_STATUSES = Set.of("待接单", "已接单", "待确认");
    private static final Set<String> TYPES = Set.of("parcel", "meal");

    private final AdminOrderMapper orders;

    public AdminOrderService(AdminOrderMapper orders) {
        this.orders = orders;
    }

    public record OrderItem(
        Long id,
        String type,
        String title,
        String status,
        BigDecimal reward,
        LocalDateTime createdAt,
        LocalDateTime deadlineAt,
        Long publisherId,
        String publisherName,
        Long runnerId,
        String runnerName
    ) {}

    public record OrderDetail(
        Long id,
        String type,
        String title,
        String station,
        String restaurant,
        String deliveryBuilding,
        String deliveryAddress,
        BigDecimal reward,
        String status,
        Long publisherId,
        String publisherName,
        String publisherPhone,
        Long runnerId,
        String runnerName,
        String runnerPhone,
        int packageCount,
        int codeCount,
        boolean hasCompletionPhoto,
        LocalDateTime createdAt,
        LocalDateTime deadlineAt,
        LocalDateTime acceptedAt,
        LocalDateTime submittedAt,
        LocalDateTime completedAt,
        LocalDateTime cancelledAt,
        String cancelledByName,
        boolean cancellable,
        boolean canForceComplete
    ) {}

    public record OrderPage(int page, int size, long total, List<OrderItem> items) {}

    // list：status 用中文精确匹配；type 只接受 parcel/meal；keyword 可搜订单号或参与者姓名。
    public OrderPage list(String keyword, String status, String type, int page, int size) {
        if (page < 1) throw new ApiException(HttpStatus.BAD_REQUEST, "页码不能小于 1");
        if (size < 1 || size > 50) throw new ApiException(HttpStatus.BAD_REQUEST, "每页数量需在 1–50 之间");
        String normalizedKeyword = keyword == null ? "" : keyword.trim();
        if (normalizedKeyword.length() > 64) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "搜索关键词不能超过 64 字");
        }
        String normalizedStatus = status == null || status.isBlank() || "all".equals(status) ? null : status;
        if (normalizedStatus != null && !STATUSES.contains(normalizedStatus)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "订单状态筛选无效");
        }
        String normalizedType = type == null || type.isBlank() || "all".equals(type) ? null : type;
        if (normalizedType != null && !TYPES.contains(normalizedType)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "订单类型筛选无效");
        }

        long total = orders.count(normalizedKeyword, normalizedStatus, normalizedType);
        List<OrderItem> items = orders.page(normalizedKeyword, normalizedStatus, normalizedType,
                (page - 1) * size, size)
            .stream()
            .map(this::toItem)
            .toList();
        return new OrderPage(page, size, total, items);
    }

    public OrderDetail detail(Long id) {
        return toDetail(requireOrder(id));
    }

    // cancel：管理员取消不归属某个用户，cancelled_by 置空，不触发接单者取消扣分。
    @Transactional
    public OrderDetail cancel(Long id) {
        AdminOrderRow.Order row = requireOrder(id);
        if (!OPEN_STATUSES.contains(row.getStatus())) {
            throw new ApiException(HttpStatus.CONFLICT, "当前状态不可取消");
        }
        if (orders.cancel(id) != 1) {
            throw new ApiException(HttpStatus.CONFLICT, "订单状态已变化，请刷新");
        }
        return detail(id);
    }

    // forceComplete：只处理已上传凭证的待确认订单，避免后台凭空完成未提交服务的订单。
    @Transactional
    public OrderDetail forceComplete(Long id) {
        AdminOrderRow.Order row = requireOrder(id);
        if (!"待确认".equals(row.getStatus()) || !Boolean.TRUE.equals(row.getHasProof())) {
            throw new ApiException(HttpStatus.CONFLICT, "只有已上传凭证的待确认订单可以强制完成");
        }
        if (orders.forceComplete(id) != 1) {
            throw new ApiException(HttpStatus.CONFLICT, "订单状态已变化，请刷新");
        }
        return detail(id);
    }

    // proofFile：只有存在凭证时才返回文件名，Controller 再限制在 uploads 目录内。
    public String proofFile(Long id) {
        requireOrder(id);
        String file = orders.proofFile(id);
        if (file == null || file.isBlank()) throw new ApiException(HttpStatus.NOT_FOUND, "订单没有完成凭证");
        return file;
    }

    private AdminOrderRow.Order requireOrder(Long id) {
        AdminOrderRow.Order row = orders.findById(id);
        if (row == null) throw new ApiException(HttpStatus.NOT_FOUND, "订单不存在");
        return row;
    }

    private OrderItem toItem(AdminOrderRow.Order row) {
        return new OrderItem(row.getId(), row.getType(), row.getTitle(), row.getStatus(), row.getReward(),
            row.getCreatedAt(), row.getDeadlineAt(), row.getPublisherId(), row.getPublisherName(),
            row.getRunnerId(), row.getRunnerName());
    }

    private OrderDetail toDetail(AdminOrderRow.Order row) {
        boolean hasProof = Boolean.TRUE.equals(row.getHasProof());
        boolean cancelled = "已取消".equals(row.getStatus());
        String cancelledByName = cancelled ? row.getCancelledByName() == null ? "管理员" : row.getCancelledByName() : null;
        return new OrderDetail(
            row.getId(),
            row.getType(),
            row.getTitle(),
            row.getStation(),
            row.getRestaurant(),
            row.getDeliveryBuilding(),
            row.getDeliveryAddress(),
            row.getReward(),
            row.getStatus(),
            row.getPublisherId(),
            row.getPublisherName(),
            row.getPublisherPhone(),
            row.getRunnerId(),
            row.getRunnerName(),
            row.getRunnerPhone(),
            row.getPackageCount() == null ? 0 : row.getPackageCount(),
            row.getCodeCount() == null ? 0 : row.getCodeCount(),
            hasProof,
            row.getCreatedAt(),
            row.getDeadlineAt(),
            row.getAcceptedAt(),
            row.getSubmittedAt(),
            row.getCompletedAt(),
            row.getCancelledAt(),
            cancelledByName,
            OPEN_STATUSES.contains(row.getStatus()),
            "待确认".equals(row.getStatus()) && hasProof
        );
    }
}
