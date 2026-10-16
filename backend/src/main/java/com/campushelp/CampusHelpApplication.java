// Spring Boot 启动入口；扫描 com.campushelp 下的组件与 Mapper，启动内置 HTTP 服务。
// 调用链：页面 → HTTP Controller → Service 业务规则 → Mapper SQL → MySQL；响应沿相反方向返回。
package com.campushelp;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import java.util.TimeZone;

@SpringBootApplication
// @Mapper 让 MyBatis 根据 SQL 注解生成接口实现；#{字段} 使用预编译绑定参数。
@MapperScan("com.campushelp.mapper")
// 类/接口职责：Spring Boot 启动入口；扫描 com.campushelp 下的组件与 Mapper，启动内置 HTTP 服务。
public class CampusHelpApplication {
    // main：JVM 执行的程序起点；SpringApplication.run 初始化依赖并启动服务，需要 Java 17。
    public static void main(String[] args) {
        TimeZone.setDefault(TimeZone.getTimeZone("Asia/Shanghai"));
        SpringApplication.run(CampusHelpApplication.class, args);
    }
}
