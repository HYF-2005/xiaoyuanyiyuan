// 系统信息只读接口；不返回密码、密钥或服务器环境变量。
package com.campushelp.admin;

import java.time.LocalDateTime;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/system")
public class AdminSystemController {
    private final JdbcTemplate jdbc;
    private final Environment environment;
    private final String uploadsDir;

    public AdminSystemController(JdbcTemplate jdbc, Environment environment,
        @Value("${app.uploads-dir}") String uploadsDir) {
        this.jdbc = jdbc;
        this.environment = environment;
        this.uploadsDir = uploadsDir;
    }

    public record Param(String name, String value, String note) {}
    public record Feature(String name, String status, String note) {}
    public record SystemInfo(String version, String serverTime, String database, String databaseVersion,
        String javaVersion, String profile, String uploadsDir, String adminSession,
        List<Param> parameters, List<Feature> features) {}

    @GetMapping("/info")
    public SystemInfo info() {
        String database = jdbc.queryForObject("SELECT DATABASE()", String.class);
        String databaseVersion = jdbc.queryForObject("SELECT VERSION()", String.class);
        String profile = environment.getActiveProfiles().length == 0
            ? "default" : String.join(",", environment.getActiveProfiles());
        return new SystemInfo(
            "1.1.0",
            LocalDateTime.now().withNano(0).toString(),
            database,
            databaseVersion,
            System.getProperty("java.version"),
            profile,
            uploadsDir,
            "12 小时（管理员登录）",
            List.of(
                new Param("服务端口", environment.getProperty("server.port", "8080"), "后端 API 监听端口"),
                new Param("上传目录", uploadsDir, "完成凭证图片保存位置"),
                new Param("后台会话", "12 小时", "token 过期时间"),
                new Param("数据库", database == null ? "未知" : database, "当前连接的 MySQL 库"),
                new Param("运行环境", profile, "Spring Boot active profile")
            ),
            List.of(
                new Feature("小程序用户认证", "已接入", "独立用户 token，与后台隔离"),
                new Feature("订单管理", "已接入", "取消、强制完成和凭证查看"),
                new Feature("风险订单", "已接入", "规则扫描与人工审核，不自动处罚"),
                new Feature("财务流水", "只读演示", "金额由订单状态推导，不接真实支付"),
                new Feature("钱包", "未接入", "小程序暂无钱包/提现业务"),
                new Feature("内容配置", "已接入", "轮播、公告实时影响小程序公开接口"),
                new Feature("地址字典", "已接入", "驿站、食堂档口、送达建筑供发布页读取"),
                new Feature("操作审计", "已接入", "写操作和登录/退出写入日志")
            )
        );
    }
}
