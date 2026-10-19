// 地理信息接口；只读。
package com.campushelp.admin;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/geography")
public class AdminGeographyController {
    private final AdminGeographyService geography;

    public AdminGeographyController(AdminGeographyService geography) {
        this.geography = geography;
    }

    @GetMapping("/overview")
    public AdminGeographyService.Overview overview() {
        return geography.overview();
    }
}
