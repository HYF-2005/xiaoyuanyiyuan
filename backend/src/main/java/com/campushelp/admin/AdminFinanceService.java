// 财务只读业务；账单由订单状态推导，金额仅用于内部演示对账。
package com.campushelp.admin;

import com.campushelp.mapper.AdminFinanceMapper;
import com.campushelp.web.ApiException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
// 类/接口职责：财务汇总与收入/退款/发布流水查询；不提供修改资金操作。
public class AdminFinanceService {
    private static final Set<String> KINDS = Set.of("publish", "refund", "income");
    private final AdminFinanceMapper finance;

    public AdminFinanceService(AdminFinanceMapper finance) {
        this.finance = finance;
    }

    public record Summary(long totalOrders, long completedOrders, long cancelledOrders,
        BigDecimal publishedAmount, BigDecimal completedAmount, BigDecimal refundedAmount,
        BigDecimal netAmount) {}

    public record Entry(String id, Long orderId, String kind, String title, BigDecimal amount,
        LocalDateTime occurredAt, String status) {}

    public record LedgerPage(int page, int size, long total, Summary summary, List<Entry> items) {}

    public LedgerPage ledger(String kind, String keyword, int page, int size) {
        if (page < 1) throw new ApiException(HttpStatus.BAD_REQUEST, "页码不能小于 1");
        if (size < 1 || size > 50) throw new ApiException(HttpStatus.BAD_REQUEST, "每页数量需在 1–50 之间");
        String normalizedKind = kind == null || kind.isBlank() || "all".equals(kind) ? null : kind;
        if (normalizedKind != null && !KINDS.contains(normalizedKind)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "流水类型筛选无效");
        }
        String normalizedKeyword = keyword == null ? "" : keyword.trim();
        if (normalizedKeyword.length() > 64) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "搜索关键词不能超过 64 字");
        }
        long total = finance.count(normalizedKind, normalizedKeyword);
        List<Entry> items = finance.ledger(normalizedKind, normalizedKeyword,
                (page - 1) * size, size).stream()
            .map(row -> new Entry(row.getId(), row.getOrderId(), row.getKind(), row.getTitle(),
                row.getAmount(), row.getOccurredAt(), row.getStatus()))
            .toList();
        return new LedgerPage(page, size, total, summary(), items);
    }

    public Summary summary() {
        AdminFinanceRow.Summary row = finance.summary();
        BigDecimal completed = row.getCompletedAmount() == null ? BigDecimal.ZERO : row.getCompletedAmount();
        BigDecimal refunded = row.getRefundedAmount() == null ? BigDecimal.ZERO : row.getRefundedAmount();
        return new Summary(
            row.getTotalOrders() == null ? 0 : row.getTotalOrders(),
            row.getCompletedOrders() == null ? 0 : row.getCompletedOrders(),
            row.getCancelledOrders() == null ? 0 : row.getCancelledOrders(),
            row.getPublishedAmount() == null ? BigDecimal.ZERO : row.getPublishedAmount(),
            completed,
            refunded,
            completed.subtract(refunded)
        );
    }
}
