// 财务只读接口；不包含真实支付、提现或退款操作。
package com.campushelp.admin;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/finance")
public class AdminFinanceController {
    private final AdminFinanceService finance;

    public AdminFinanceController(AdminFinanceService finance) {
        this.finance = finance;
    }

    @GetMapping("/summary")
    public AdminFinanceService.Summary summary() {
        return finance.summary();
    }

    @GetMapping("/ledger")
    public AdminFinanceService.LedgerPage ledger(
        @RequestParam(defaultValue = "1") int page,
        @RequestParam(defaultValue = "20") int size,
        @RequestParam(defaultValue = "all") String kind,
        @RequestParam(defaultValue = "") String keyword
    ) {
        return finance.ledger(kind, keyword, page, size);
    }
}
