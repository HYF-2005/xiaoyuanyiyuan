// 后台地址字典接口；新增/修改/删除都会写入操作审计。
package com.campushelp.admin;

import com.campushelp.audit.AdminAuditService;
import com.campushelp.location.LocationService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/locations")
public class AdminLocationController {
    private final LocationService locations;
    private final AdminAuditService audit;

    public AdminLocationController(LocationService locations, AdminAuditService audit) {
        this.locations = locations;
        this.audit = audit;
    }

    @GetMapping
    public LocationService.Overview list() {
        return locations.list();
    }

    @PostMapping
    public LocationService.Item create(HttpServletRequest request,
        @RequestBody LocationService.SaveRequest body) {
        LocationService.Item item = locations.create(body);
        audit.record(request, "LOCATION_CREATE", "location", item.id(),
            "新增" + item.kindLabel() + "：" + item.name());
        return item;
    }

    @PutMapping("/{id}")
    public LocationService.Item update(HttpServletRequest request, @PathVariable Long id,
        @RequestBody LocationService.UpdateRequest body) {
        LocationService.Item item = locations.update(id, body);
        audit.record(request, "LOCATION_UPDATE", "location", id,
            "更新" + item.kindLabel() + "：" + item.name());
        return item;
    }

    @DeleteMapping("/{id}")
    public void delete(HttpServletRequest request, @PathVariable Long id) {
        LocationService.Item item = locations.detail(id);
        locations.delete(id);
        audit.record(request, "LOCATION_DELETE", "location", id,
            "删除" + item.kindLabel() + "：" + item.name());
    }
}
