// 后台风险订单接口；扫描、分页查询和人工审核。
package com.campushelp.admin;

import com.campushelp.audit.AdminAuditService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/risk-orders")
// 类/接口职责：风险订单扫描、风险队列分页和人工审核结论保存。
public class OrderRiskController {
    private final OrderRiskService risks;
    private final AdminAuditService audit;

    public OrderRiskController(OrderRiskService risks, AdminAuditService audit) {
        this.risks = risks;
        this.audit = audit;
    }

    @GetMapping
    public OrderRiskService.RiskPage list(
        @RequestParam(defaultValue = "1") int page,
        @RequestParam(defaultValue = "12") int size,
        @RequestParam(defaultValue = "all") String level,
        @RequestParam(defaultValue = "all") String reviewStatus,
        @RequestParam(defaultValue = "") String keyword
    ) {
        return risks.list(keyword, level, reviewStatus, page, size);
    }

    @PostMapping("/scan")
    public OrderRiskService.ScanResult scan(HttpServletRequest request) {
        OrderRiskService.ScanResult result = risks.scan();
        audit.record(request, "ORDER_RISK_SCAN", "order_risk", null,
            "扫描风险订单：发现 " + result.flagged() + " 笔");
        return result;
    }

    @PatchMapping("/{id}/review")
    public OrderRiskService.Item review(HttpServletRequest request, @PathVariable Long id,
        @RequestBody OrderRiskService.ReviewRequest body) {
        var admin = AdminAuthInterceptor.current(request);
        OrderRiskService.Item item = risks.review(id, body, admin == null ? null : admin.getId());
        audit.record(request, "ORDER_RISK_REVIEW", "order", id,
            "风险订单 #" + id + " 标记为 " + item.reviewStatus());
        return item;
    }
}
