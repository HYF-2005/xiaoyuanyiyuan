// 小程序发布页地址树；只返回启用节点，未登录请求由统一拦截器拒绝。
package com.campushelp.location;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/locations")
public class LocationController {
    private final LocationService locations;

    public LocationController(LocationService locations) {
        this.locations = locations;
    }

    @GetMapping
    public LocationService.PublicLocations list() {
        return locations.publicTree();
    }
}
