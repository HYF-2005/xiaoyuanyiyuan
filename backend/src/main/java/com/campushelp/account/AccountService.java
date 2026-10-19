// 账户统计业务；根据订单重算信用分与演示账单/收入，没有接微信支付或钱包系统。
package com.campushelp.account;

import com.campushelp.mapper.OrderMapper;
import com.campushelp.mapper.UserMapper;
import com.campushelp.model.HelpOrder;
import com.campushelp.model.User;
import com.campushelp.web.ApiException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
// 类/接口职责：账户统计业务；根据订单重算信用分与演示账单/收入，没有接微信支付或钱包系统。
public class AccountService {
    private final OrderMapper orders;
    private final UserMapper users;

    // AccountService：构造器由 Spring 注入所需依赖（测试中可手动创建）；不在每次请求中重复 new 数据库工具。
    public AccountService(OrderMapper orders, UserMapper users) {
        this.orders = orders;
        this.users = users;
    }

    public record CreditRecord(String id, String reason, int change, LocalDateTime at) {}
    public record CreditSummary(int score, List<CreditRecord> records) {}
    public record Transaction(String id, Long orderId, String kind, String title, BigDecimal amount,
    LocalDateTime at, String status) {}
    public record AccountSummary(Long userId, String username, String displayName, String phone,
    int creditScore, BigDecimal incomeTotal) {}

    // credit：从基础分加总订单记录：已确认完成且按提交时间未超期 +1；接单者取消取餐 -4 / 取件 -7；最终限制 0–100。每次重算，不是每次刷新再扣一次。
    public CreditSummary credit(Long userId) {
        User user = users.findById(userId);
        if (user == null) throw new ApiException(HttpStatus.UNAUTHORIZED, "账号不存在");
        return credit(user);
    }

    // credit(User)：后台可传入已查询出的任意用户（包括已停用账号），不需要再走 active=1 的登录查询。
    public CreditSummary credit(User user) {
        if (user == null || user.getId() == null) throw new ApiException(HttpStatus.UNAUTHORIZED, "账号不存在");
        Long userId = user.getId();
        List<CreditRecord> records = new ArrayList<>();
        for (HelpOrder order : orders.byRunner(userId)) {
            if ("已完成".equals(order.getStatus()) && order.getCompletedAt() != null
            && (order.getDeadlineAt() == null || order.getSubmittedAt() != null
            && !order.getSubmittedAt().isAfter(order.getDeadlineAt()))) {
                records.add(new CreditRecord(order.getType() + "-" + order.getId() + "-complete",
                "按时完成订单：" + order.getTitle(), 1, order.getCompletedAt()));
            }
            if ("已取消".equals(order.getStatus()) && userId.equals(order.getCancelledBy())
            && order.getAcceptedAt() != null && order.getCancelledAt() != null) {
                // 扣分依据是谁取消了已接订单，取餐 -4、取件 -7；普通发布者取消待接单不会按此扣分。
                int penalty = "meal".equals(order.getType()) ? -4 : -7;
                records.add(new CreditRecord(order.getType() + "-" + order.getId() + "-cancel",
                "接单后取消：" + order.getTitle(), penalty, order.getCancelledAt()));
            }
        }
        records.sort(Comparator.comparing(CreditRecord::at).reversed());
        // 在基础分上汇总变动，再用 min/max 把结果限制在 0–100。
        int base = user.getCreditBase() == null ? 61 : user.getCreditBase();
        int score = Math.max(0, Math.min(100, base
        + records.stream().mapToInt(CreditRecord::change).sum()));
        return new CreditSummary(score, records);
    }

    // creditScore：只取信用汇总中的分数，供账户信息/订单发布者信用展示使用。
    public int creditScore(Long userId) {
        return credit(userId).score();
    }

    // 与当前小程序一致：账单是订单状态推导出的演示记录，不表示真实支付已发生。
    // transactions：按订单状态推导发布负金额、取消退回、完成收入，并按时间降序；没有真实扣款/退款。
    public List<Transaction> transactions(Long userId) {
        List<Transaction> records = new ArrayList<>();
        for (HelpOrder order : orders.byPublisher(userId)) {
            records.add(new Transaction(order.getType() + "-" + order.getId() + "-publish", order.getId(),
            "publish", "发布" + ("meal".equals(order.getType()) ? "取餐" : "取件") + "订单",
            order.getReward().negate(), order.getCreatedAt(), "成功"));
            if ("已取消".equals(order.getStatus()) && order.getCancelledAt() != null) {
                records.add(new Transaction(order.getType() + "-" + order.getId() + "-refund", order.getId(),
                "refund", "订单取消退款：" + order.getTitle(), order.getReward(), order.getCancelledAt(), "成功"));
            }
        }
        for (HelpOrder order : orders.byRunner(userId)) {
            if ("已完成".equals(order.getStatus()) && order.getCompletedAt() != null) {
                records.add(new Transaction(order.getType() + "-" + order.getId() + "-income", order.getId(),
                "income", "订单完成收入：" + order.getTitle(), order.getReward(), order.getCompletedAt(), "成功"));
            }
        }
        records.sort(Comparator.comparing(Transaction::at).reversed());
        return records;
    }

    // summary：读取本用户信息；只累计 kind=income 的完成收入，退款不计入个人中心累计收入。
    public AccountSummary summary(Long userId) {
        User user = users.findById(userId);
        if (user == null) throw new ApiException(HttpStatus.UNAUTHORIZED, "账号不存在");
        // 只累计完成收入，使用 BigDecimal 保持金额精度；不把取消退回当成接单收入。
        BigDecimal income = transactions(userId).stream().filter(item -> item.kind().equals("income"))
        .map(Transaction::amount).reduce(BigDecimal.ZERO, BigDecimal::add);
        return new AccountSummary(userId, user.getUsername(), user.getDisplayName(), user.getPhone(),
        creditScore(userId), income);
    }
}
