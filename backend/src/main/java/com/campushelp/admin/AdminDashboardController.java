// 后台数据看板接口；受管理员拦截器保护，只返回统计结果。
package com.campushelp.admin;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/dashboard")
// 类/接口职责：后台首页汇总接口；days 控制趋势图时间范围。
public class AdminDashboardController {
    private final AdminDashboardService dashboard;

    public AdminDashboardController(AdminDashboardService dashboard) {
        this.dashboard = dashboard;
    }

    @GetMapping
    public AdminDashboardService.DashboardResponse load(
        @RequestParam(defaultValue = "14") int days
    ) {
        return dashboard.load(days);
    }
}
