// 后台用户管理业务；列表、详情、信用查看和账号启停。
package com.campushelp.admin;

import com.campushelp.account.AccountService;
import com.campushelp.mapper.AdminUserMapper;
import com.campushelp.mapper.SessionMapper;
import com.campushelp.model.User;
import com.campushelp.web.ApiException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
// 类/接口职责：后台用户管理；停用账号时同步撤销该用户的全部小程序登录会话。
public class AdminUserService {
    private final AdminUserMapper users;
    private final SessionMapper sessions;
    private final AccountService accounts;

    public AdminUserService(AdminUserMapper users, SessionMapper sessions, AccountService accounts) {
        this.users = users;
        this.sessions = sessions;
        this.accounts = accounts;
    }

    public record UserItem(
        Long id,
        String username,
        String displayName,
        String phone,
        int creditBase,
        boolean active,
        boolean closed,
        LocalDateTime createdAt,
        long publishedOrders,
        long acceptedOrders,
        long openOrders
    ) {}

    public record UserOrder(
        Long id,
        String type,
        String title,
        String status,
        BigDecimal reward,
        LocalDateTime createdAt,
        String role
    ) {}

    public record UserDetail(
        Long id,
        String username,
        String displayName,
        String phone,
        String college,
        String major,
        String grade,
        String className,
        String dormitory,
        int creditBase,
        boolean active,
        boolean closed,
        LocalDateTime createdAt,
        long publishedOrders,
        long acceptedOrders,
        long openOrders,
        int creditScore,
        List<AccountService.CreditRecord> creditRecords,
        List<UserOrder> recentOrders
    ) {}

    public record UserPage(int page, int size, long total, List<UserItem> items) {}

    // list：keyword 只做模糊搜索；status 只接受 all/active/disabled，非法值直接 400。
    public UserPage list(String keyword, String status, int page, int size) {
        if (page < 1) throw new ApiException(HttpStatus.BAD_REQUEST, "页码不能小于 1");
        if (size < 1 || size > 50) throw new ApiException(HttpStatus.BAD_REQUEST, "每页数量需在 1–50 之间");
        String normalizedKeyword = keyword == null ? "" : keyword.trim();
        if (normalizedKeyword.length() > 64) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "搜索关键词不能超过 64 字");
        }
        Integer active = switch (status == null ? "all" : status) {
            case "all", "" -> null;
            case "active" -> 1;
            case "disabled" -> 0;
            default -> throw new ApiException(HttpStatus.BAD_REQUEST, "用户状态筛选无效");
        };

        long total = users.count(normalizedKeyword, active);
        List<UserItem> items = users.page(normalizedKeyword, active, (page - 1) * size, size)
            .stream()
            .map(this::toItem)
            .toList();
        return new UserPage(page, size, total, items);
    }

    public UserDetail detail(Long id) {
        AdminUserRow.User row = requireUser(id);
        User user = new User();
        user.setId(row.getId());
        user.setCreditBase(row.getCreditBase());
        AccountService.CreditSummary credit = accounts.credit(user);
        List<UserOrder> recentOrders = users.recentOrders(id).stream()
            .map(order -> new UserOrder(order.getId(), order.getType(), order.getTitle(), order.getStatus(),
                order.getReward(), order.getCreatedAt(), order.getRole()))
            .toList();

        return new UserDetail(
            row.getId(),
            row.getUsername(),
            row.getDisplayName(),
            row.getPhone(),
            row.getCollege(),
            row.getMajor(),
            row.getGrade(),
            row.getClassName(),
            row.getDormitory(),
            row.getCreditBase() == null ? 61 : row.getCreditBase(),
            Boolean.TRUE.equals(row.getActive()),
            isClosed(row),
            row.getCreatedAt(),
            row.getPublishedOrders() == null ? 0 : row.getPublishedOrders(),
            row.getAcceptedOrders() == null ? 0 : row.getAcceptedOrders(),
            row.getOpenOrders() == null ? 0 : row.getOpenOrders(),
            credit.score(),
            credit.records(),
            recentOrders
        );
    }

    // updateStatus：停用立即撤销全部登录会话；自主注销的账号不允许后台直接恢复。
    @Transactional
    public void updateStatus(Long id, Boolean active) {
        if (active == null) throw new ApiException(HttpStatus.BAD_REQUEST, "缺少启用状态");
        AdminUserRow.User row = requireUser(id);
        if (active && isClosed(row)) {
            throw new ApiException(HttpStatus.CONFLICT, "该账号已自主注销，不能直接启用");
        }
        if (Boolean.TRUE.equals(row.getActive()) == active) return;

        users.updateActive(id, active);
        if (!active) sessions.deleteByUserId(id);
    }

    private AdminUserRow.User requireUser(Long id) {
        AdminUserRow.User row = users.findById(id);
        if (row == null) throw new ApiException(HttpStatus.NOT_FOUND, "用户不存在");
        return row;
    }

    private UserItem toItem(AdminUserRow.User row) {
        return new UserItem(
            row.getId(),
            row.getUsername(),
            row.getDisplayName(),
            row.getPhone(),
            row.getCreditBase() == null ? 61 : row.getCreditBase(),
            Boolean.TRUE.equals(row.getActive()),
            isClosed(row),
            row.getCreatedAt(),
            row.getPublishedOrders() == null ? 0 : row.getPublishedOrders(),
            row.getAcceptedOrders() == null ? 0 : row.getAcceptedOrders(),
            row.getOpenOrders() == null ? 0 : row.getOpenOrders()
        );
    }

    private boolean isClosed(AdminUserRow.User row) {
        return "已注销用户".equals(row.getDisplayName());
    }
}
