// 数据看板业务；汇总实时计数、按日趋势和最近记录，全部来自现有业务表。
package com.campushelp.admin;

import com.campushelp.account.AccountService;
import com.campushelp.mapper.AdminDashboardMapper;
import com.campushelp.web.ApiException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
// 类/接口职责：把数据库聚合行组装成前端直接可用的看板结构，不伪造演示数据。
public class AdminDashboardService {
    private static final List<String> ORDER_STATUSES =
        List.of("待接单", "已接单", "待确认", "已完成", "已取消");
    private final AdminDashboardMapper dashboard;
    private final AccountService accounts;

    public AdminDashboardService(AdminDashboardMapper dashboard, AccountService accounts) {
        this.dashboard = dashboard;
        this.accounts = accounts;
    }

    public record Summary(
        long totalUsers,
        long activeUsers,
        long newUsersToday,
        long totalOrders,
        long newOrdersToday,
        long openOrders,
        long completedOrders,
        long feedbackCount,
        double completionRate,
        BigDecimal completedAmount
    ) {}

    public record TrendPoint(String date, long users, long orders, long completed, BigDecimal amount) {}

    public record StatusCount(String status, long total) {}

    public record RecentOrder(
        Long id,
        String type,
        String title,
        String status,
        BigDecimal reward,
        BigDecimal platformFee,
        BigDecimal actualIncome,
        Long publisherId,
        String publisherName,
        int publisherCredit,
        Long runnerId,
        String runnerName,
        Integer runnerCredit,
        LocalDateTime createdAt
    ) {}

    public record RecentFeedback(Long id, String content, String userName, LocalDateTime createdAt) {}

    public record DashboardResponse(
        LocalDateTime generatedAt,
        int days,
        Summary summary,
        List<TrendPoint> trend,
        List<StatusCount> status,
        List<RecentOrder> recentOrders,
        List<RecentFeedback> recentFeedback
    ) {}

    // load：days 只允许 7/14/30，避免任意大范围查询拖慢后台。
    public DashboardResponse load(int days) {
        if (days != 7 && days != 14 && days != 30) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "时间范围只支持 7、14 或 30 天");
        }

        long totalUsers = dashboard.countUsers();
        long totalOrders = dashboard.countOrders();
        long completedOrders = dashboard.countCompletedOrders();
        BigDecimal completedAmount = dashboard.completedAmount();

        Summary summary = new Summary(
            totalUsers,
            dashboard.countActiveUsers(),
            dashboard.countUsersToday(),
            totalOrders,
            dashboard.countOrdersToday(),
            dashboard.countOpenOrders(),
            completedOrders,
            dashboard.countFeedback(),
            totalOrders == 0 ? 0 : Math.round(completedOrders * 1000.0 / totalOrders) / 10.0,
            completedAmount == null ? BigDecimal.ZERO : completedAmount
        );

        return new DashboardResponse(
            LocalDateTime.now(),
            days,
            summary,
            buildTrend(days),
            buildStatus(),
            buildRecentOrders(),
            dashboard.recentFeedback().stream()
                .map(row -> new RecentFeedback(row.getId(), row.getContent(), row.getUserName(),
                    row.getCreatedAt()))
                .toList()
        );
    }

    // buildRecentOrders：看板表格需要的参与者、信用分和金额一次组装，避免前端逐行再查。
    private List<RecentOrder> buildRecentOrders() {
        Map<Long, Integer> creditCache = new HashMap<>();
        List<RecentOrder> result = new ArrayList<>();
        for (DashboardRow.RecentOrder row : dashboard.recentOrders()) {
            int publisherCredit = creditCache.computeIfAbsent(row.getPublisherId(), accounts::creditScore);
            Integer runnerCredit = row.getRunnerId() == null ? null
                : creditCache.computeIfAbsent(row.getRunnerId(), accounts::creditScore);
            boolean completed = "已完成".equals(row.getStatus());
            result.add(new RecentOrder(
                row.getId(), row.getType(), row.getTitle(), row.getStatus(), row.getReward(),
                BigDecimal.ZERO, completed ? row.getReward() : BigDecimal.ZERO,
                row.getPublisherId(), row.getPublisherName(), publisherCredit,
                row.getRunnerId(), row.getRunnerName(), runnerCredit, row.getCreatedAt()
            ));
        }
        return result;
    }

    // buildTrend：按日期补零，保证图表在“今天没有订单”时也不会断成不连续的点。
    private List<TrendPoint> buildTrend(int days) {
        LocalDate start = LocalDate.now().minusDays(days - 1L);
        LocalDateTime from = start.atStartOfDay();

        Map<LocalDate, Long> usersByDay = new LinkedHashMap<>();
        for (DashboardRow.UserDay row : dashboard.countUsersByDay(from)) {
            usersByDay.put(row.getDay(), row.getTotal() == null ? 0 : row.getTotal());
        }

        Map<LocalDate, DashboardRow.OrderDay> ordersByDay = new LinkedHashMap<>();
        for (DashboardRow.OrderDay row : dashboard.countOrdersByDay(from)) {
            ordersByDay.put(row.getDay(), row);
        }

        List<TrendPoint> trend = new ArrayList<>();
        for (int offset = 0; offset < days; offset++) {
            LocalDate date = start.plusDays(offset);
            DashboardRow.OrderDay orderDay = ordersByDay.get(date);
            trend.add(new TrendPoint(
                date.toString(),
                usersByDay.getOrDefault(date, 0L),
                orderDay == null || orderDay.getTotal() == null ? 0 : orderDay.getTotal(),
                orderDay == null || orderDay.getCompleted() == null ? 0 : orderDay.getCompleted(),
                orderDay == null || orderDay.getAmount() == null ? BigDecimal.ZERO : orderDay.getAmount()
            ));
        }
        return trend;
    }

    // buildStatus：先固定订单状态顺序，再补数据库里可能出现的其他状态。
    private List<StatusCount> buildStatus() {
        Map<String, Long> counts = new LinkedHashMap<>();
        ORDER_STATUSES.forEach(status -> counts.put(status, 0L));
        for (DashboardRow.Status row : dashboard.countOrdersByStatus()) {
            counts.put(row.getStatus(), row.getTotal() == null ? 0 : row.getTotal());
        }
        return counts.entrySet().stream()
            .map(entry -> new StatusCount(entry.getKey(), entry.getValue()))
            .toList();
    }
}
