// 风险订单规则扫描与人工审核；只生成风险快照，不自动取消订单、扣信用或封禁账号。
package com.campushelp.admin;

import com.campushelp.mapper.OrderRiskMapper;
import com.campushelp.web.ApiException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
// 类/接口职责：可解释的风险规则扫描、风险队列查询和人工审核结论保存。
public class OrderRiskService {
    private static final Set<String> LEVELS = Set.of("low", "medium", "high");
    private static final Set<String> REVIEW_STATUSES = Set.of("待审核", "已放行", "已确认风险");
    private static final long SCAN_TTL_MILLIS = 60_000L;
    private static final ObjectMapper JSON = new ObjectMapper();

    private final OrderRiskMapper risks;
    private volatile long lastScanAt = 0L;

    public OrderRiskService(OrderRiskMapper risks) {
        this.risks = risks;
    }

    public record Item(
        Long orderId,
        int riskScore,
        String riskLevel,
        List<String> riskFlags,
        String reviewStatus,
        String reviewNote,
        String reviewerName,
        LocalDateTime reviewedAt,
        LocalDateTime scannedAt,
        String type,
        String title,
        String orderStatus,
        BigDecimal reward,
        LocalDateTime createdAt,
        Long publisherId,
        String publisherName,
        Long runnerId,
        String runnerName
    ) {}

    public record RiskPage(int page, int size, long total, OrderRiskRow.Summary summary, List<Item> items) {}

    public record ReviewRequest(String status, String note) {}

    public record ScanResult(int scanned, int flagged, int high, int medium, int low, LocalDateTime scannedAt) {}

    // list：默认每页 12 条；风险等级与审核状态都允许多选值 all。列表读取前自动刷新超过 60 秒的快照。
    public RiskPage list(String keyword, String level, String reviewStatus, int page, int size) {
        if (page < 1) throw new ApiException(HttpStatus.BAD_REQUEST, "页码不能小于 1");
        if (size < 1 || size > 50) throw new ApiException(HttpStatus.BAD_REQUEST, "每页数量需在 1–50 之间");
        String normalizedKeyword = keyword == null ? "" : keyword.trim();
        if (normalizedKeyword.length() > 64) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "搜索关键词不能超过 64 字");
        }
        String normalizedLevel = normalizeFilter(level);
        if (normalizedLevel != null && !LEVELS.contains(normalizedLevel)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "风险等级筛选无效");
        }
        String normalizedReview = normalizeFilter(reviewStatus);
        if (normalizedReview != null && !REVIEW_STATUSES.contains(normalizedReview)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "审核状态筛选无效");
        }
        ensureFresh();
        long total = risks.count(normalizedKeyword, normalizedLevel, normalizedReview);
        List<Item> items = risks.page(normalizedKeyword, normalizedLevel, normalizedReview,
                (page - 1) * size, size).stream().map(this::toItem).toList();
        OrderRiskRow.Summary summary = risks.summary();
        return new RiskPage(page, size, total, summary, items);
    }

    // scan：重建全部风险快照；人工审核结论不会被覆盖，便于重新扫描后继续处理。
    @Transactional
    public ScanResult scan() {
        List<OrderRiskRow.Signal> signals = risks.signals();
        Map<Long, Integer> userTotal = new HashMap<>();
        Map<Long, Integer> userCancelled = new HashMap<>();
        Map<String, List<OrderRiskRow.Signal>> acceptedPairs = new HashMap<>();
        Map<Long, List<LocalDateTime>> publisherTimes = new HashMap<>();
        Map<Long, List<LocalDateTime>> runnerTimes = new HashMap<>();

        for (OrderRiskRow.Signal order : signals) {
            userTotal.merge(order.getPublisherId(), 1, Integer::sum);
            if (order.getRunnerId() != null) userTotal.merge(order.getRunnerId(), 1, Integer::sum);
            if ("已取消".equals(order.getStatus()) && order.getAcceptedAt() != null
                && order.getCancelledBy() != null) {
                userCancelled.merge(order.getCancelledBy(), 1, Integer::sum);
            }
            if (order.getRunnerId() != null && !order.getRunnerId().equals(order.getPublisherId())
                && order.getAcceptedAt() != null) {
                String pair = pairKey(order.getPublisherId(), order.getRunnerId());
                acceptedPairs.computeIfAbsent(pair, ignored -> new ArrayList<>()).add(order);
            }
            if (order.getCreatedAt() != null) {
                publisherTimes.computeIfAbsent(order.getPublisherId(), ignored -> new ArrayList<>())
                    .add(order.getCreatedAt());
            }
            if (order.getRunnerId() != null && order.getAcceptedAt() != null) {
                runnerTimes.computeIfAbsent(order.getRunnerId(), ignored -> new ArrayList<>())
                    .add(order.getAcceptedAt());
            }
        }

        Set<Long> reciprocalBurstOrders = reciprocalBurstOrderIds(acceptedPairs);

        int flagged = 0;
        int high = 0;
        int medium = 0;
        int low = 0;
        for (OrderRiskRow.Signal order : signals) {
            List<Flag> flags = new ArrayList<>();
            boolean selfOrder = order.getRunnerId() != null
                && order.getRunnerId().equals(order.getPublisherId());
            if (selfOrder) {
                addFlag(flags, "发布者与接单者相同", 100);
            }
            if (reciprocalBurstOrders.contains(order.getId())) {
                addFlag(flags, "1 天内互相下单超过 5 笔且接单间隔≤10 分钟", 75);
            }
            if (!selfOrder && order.getRunnerId() != null) {
                if (order.getAcceptedAt() != null) {
                    long seconds = secondsBetween(order.getAcceptedAt(),
                        order.getSubmittedAt() != null ? order.getSubmittedAt() : order.getCompletedAt());
                    if (seconds >= 0 && seconds <= 60) {
                        addFlag(flags, seconds == 0 ? "接单后立即完成/提交" : "接单后 " + seconds + " 秒内完成/提交", 25);
                    }
                }
            }
            if (order.getCreatedAt() != null
                && (countWindow(publisherTimes.get(order.getPublisherId()), order.getCreatedAt(), 24) > 5
                || (order.getRunnerId() != null
                && countWindow(runnerTimes.get(order.getRunnerId()), order.getAcceptedAt(), 24) > 5))) {
                addFlag(flags, "24 小时内发布或接单超过 5 笔", 30);
            }
            if (cancelRateHigh(order.getPublisherId(), userTotal, userCancelled)
                || (order.getRunnerId() != null
                && cancelRateHigh(order.getRunnerId(), userTotal, userCancelled))) {
                addFlag(flags, "账号取消率超过 30%", 20);
            }
            if (order.getReward() != null && order.getReward().compareTo(BigDecimal.valueOf(50)) >= 0
                && (newAccount(order.getPublisherCreatedAt(), order.getCreatedAt())
                || newAccount(order.getRunnerCreatedAt(), order.getCreatedAt()))) {
                addFlag(flags, "新账号参与高报酬订单", 20);
            }
            if ("已完成".equals(order.getStatus()) && order.getProofFile() == null) {
                addFlag(flags, "已完成但缺少完成凭证", 50);
            }

            int score = Math.min(100, flags.stream().mapToInt(Flag::score).sum());
            String level = score >= 60 ? "high" : score >= 30 ? "medium" : score > 0 ? "low" : "none";
            if (score > 0) {
                flagged++;
                if ("high".equals(level)) high++;
                else if ("medium".equals(level)) medium++;
                else low++;
            }
            risks.upsert(order.getId(), score, level, flagsJson(flags));
        }
        lastScanAt = System.currentTimeMillis();
        return new ScanResult(signals.size(), flagged, high, medium, low, LocalDateTime.now());
    }

    // review：人工结论只落在风险快照，不自动改订单/信用/账号状态，避免误伤。
    @Transactional
    public Item review(Long orderId, ReviewRequest request, Long adminId) {
        if (request == null || request.status() == null || !REVIEW_STATUSES.contains(request.status())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "审核状态无效");
        }
        String note = request.note() == null ? "" : request.note().trim();
        if (note.length() > 500) throw new ApiException(HttpStatus.BAD_REQUEST, "审核备注不能超过 500 字");
        if (risks.review(orderId, request.status(), note, adminId) != 1) {
            throw new ApiException(HttpStatus.NOT_FOUND, "风险记录不存在，请先重新扫描");
        }
        OrderRiskRow.Risk row = risks.findItem(orderId);
        if (row == null) throw new ApiException(HttpStatus.NOT_FOUND, "风险记录不存在");
        return toItem(row);
    }

    private void ensureFresh() {
        if (System.currentTimeMillis() - lastScanAt > SCAN_TTL_MILLIS) {
            scan();
        }
    }

    private static String normalizeFilter(String value) {
        return value == null || value.isBlank() || "all".equals(value) ? null : value;
    }

    private static void addFlag(List<Flag> flags, String message, int score) {
        flags.add(new Flag(message + " (+" + score + ")", score));
    }

    private static long secondsBetween(LocalDateTime start, LocalDateTime end) {
        if (start == null || end == null) return -1;
        return Duration.between(start, end).getSeconds();
    }


    private static Set<Long> reciprocalBurstOrderIds(Map<String, List<OrderRiskRow.Signal>> acceptedPairs) {
        Set<Long> result = new HashSet<>();
        for (List<OrderRiskRow.Signal> pairOrders : acceptedPairs.values()) {
            pairOrders.sort(Comparator.comparing(OrderRiskRow.Signal::getAcceptedAt)
                .thenComparing(OrderRiskRow.Signal::getId));
            List<OrderRiskRow.Signal> burst = new ArrayList<>();
            for (OrderRiskRow.Signal order : pairOrders) {
                if (!burst.isEmpty()) {
                    long gap = secondsBetween(burst.get(burst.size() - 1).getAcceptedAt(), order.getAcceptedAt());
                    if (gap > 600) {
                        markReciprocalBurst(burst, result);
                        burst.clear();
                    }
                }
                burst.add(order);
            }
            markReciprocalBurst(burst, result);
        }
        return result;
    }

    // 同一对用户、相邻接单间隔≤10 分钟形成一段；超过 5 笔且双方都有下单才命中。
    private static void markReciprocalBurst(List<OrderRiskRow.Signal> burst, Set<Long> result) {
        if (burst.size() <= 5) return;
        Set<String> directions = new HashSet<>();
        for (OrderRiskRow.Signal order : burst) {
            directions.add(order.getPublisherId() + "->" + order.getRunnerId());
        }
        if (directions.size() < 2) return;
        for (OrderRiskRow.Signal order : burst) result.add(order.getId());
    }

    private static int countWindow(List<LocalDateTime> times, LocalDateTime base, int hours) {
        if (times == null || base == null) return 0;
        return (int) times.stream().filter(time -> time != null
            && !time.isBefore(base.minusHours(hours)) && !time.isAfter(base.plusHours(hours))).count();
    }

    private static boolean cancelRateHigh(Long userId, Map<Long, Integer> total,
        Map<Long, Integer> cancelled) {
        if (userId == null) return false;
        int all = total.getOrDefault(userId, 0);
        int cancel = cancelled.getOrDefault(userId, 0);
        return all >= 5 && cancel * 1.0 / all > 0.30;
    }

    private static boolean newAccount(LocalDateTime accountCreatedAt, LocalDateTime orderCreatedAt) {
        return accountCreatedAt != null && orderCreatedAt != null
            && accountCreatedAt.plusHours(24).isAfter(orderCreatedAt);
    }

    private static String pairKey(Long first, Long second) {
        return Math.min(first, second) + ":" + Math.max(first, second);
    }

    private static String flagsJson(List<Flag> flags) {
        try {
            return JSON.writeValueAsString(flags.stream().map(Flag::message).toList());
        } catch (Exception ignored) {
            return "[]";
        }
    }

    private Item toItem(OrderRiskRow.Risk row) {
        return new Item(
            row.getOrderId(), row.getRiskScore() == null ? 0 : row.getRiskScore(),
            row.getRiskLevel(), parseFlags(row.getRiskFlags()), row.getReviewStatus(),
            row.getReviewNote(), row.getReviewerName(), row.getReviewedAt(), row.getScannedAt(),
            row.getType(), row.getTitle(), row.getStatus(), row.getReward(), row.getCreatedAt(),
            row.getPublisherId(), row.getPublisherName(), row.getRunnerId(), row.getRunnerName()
        );
    }

    private static List<String> parseFlags(String raw) {
        if (raw == null || raw.isBlank()) return List.of();
        try {
            return JSON.readValue(raw, new TypeReference<List<String>>() {});
        } catch (Exception ignored) {
            return List.of();
        }
    }

    private record Flag(String message, int score) {}
}
