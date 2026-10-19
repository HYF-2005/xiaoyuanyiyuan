// 首次部署时可选的初始管理员创建器；只有显式设置 ADMIN_INIT_PASSWORD 才会执行。
package com.campushelp.admin;

import com.campushelp.mapper.AdminMapper;
import com.campushelp.model.Admin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

@Component
// 类/接口职责：用环境变量创建第一个管理员；不把默认密码写进源码或 data.sql。
public class AdminBootstrap implements ApplicationRunner {
    private static final Logger log = LoggerFactory.getLogger(AdminBootstrap.class);
    private final AdminMapper admins;
    private final String username;
    private final String password;
    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    public AdminBootstrap(AdminMapper admins,
        @Value("${app.admin-init.username:admin}") String username,
        @Value("${app.admin-init.password:}") String password) {
        this.admins = admins;
        this.username = username == null ? "admin" : username.trim();
        this.password = password;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (password == null || password.isBlank()) return;
        if (username.isBlank() || password.length() < 8) {
            log.warn("ADMIN_INIT_PASSWORD 至少 8 位，初始管理员未创建");
            return;
        }
        if (admins.findByUsername(username) != null) return;

        Admin admin = new Admin();
        admin.setUsername(username);
        admin.setPasswordHash(encoder.encode(password));
        admin.setDisplayName("超级管理员");
        admin.setRole("super_admin");
        admin.setActive(true);
        admins.insert(admin);
        log.info("已创建初始管理员账号：{}；请登录后尽快修改密码", username);
    }
}
