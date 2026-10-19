// 后台订单管理接口；列表/详情/异常操作，以及管理员查看完成凭证。
package com.campushelp.admin;

import com.campushelp.audit.AdminAuditService;
import com.campushelp.web.ApiException;
import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/orders")
// 类/接口职责：后台订单列表、详情、取消、强制完成和凭证读取。
public class AdminOrderController {
    private final AdminOrderService orders;
    private final AdminAuditService audit;
    private final Path uploads;

    public AdminOrderController(AdminOrderService orders, AdminAuditService audit,
        @Value("${app.uploads-dir}") String uploadsDir) {
        this.orders = orders;
        this.audit = audit;
        this.uploads = Path.of(uploadsDir).toAbsolutePath().normalize();
    }

    @GetMapping
    public AdminOrderService.OrderPage list(
        @RequestParam(defaultValue = "1") int page,
        @RequestParam(defaultValue = "10") int size,
        @RequestParam(defaultValue = "all") String status,
        @RequestParam(defaultValue = "all") String type,
        @RequestParam(defaultValue = "") String keyword
    ) {
        return orders.list(keyword, status, type, page, size);
    }

    @GetMapping("/{id}")
    public AdminOrderService.OrderDetail detail(@PathVariable Long id) {
        return orders.detail(id);
    }

    @PatchMapping("/{id}/cancel")
    public AdminOrderService.OrderDetail cancel(HttpServletRequest request, @PathVariable Long id) {
        AdminOrderService.OrderDetail detail = orders.cancel(id);
        audit.record(request, "ORDER_CANCEL", "order", id, "管理员取消订单 #" + id);
        return detail;
    }

    @PatchMapping("/{id}/complete")
    public AdminOrderService.OrderDetail complete(HttpServletRequest request, @PathVariable Long id) {
        AdminOrderService.OrderDetail detail = orders.forceComplete(id);
        audit.record(request, "ORDER_FORCE_COMPLETE", "order", id, "管理员强制完成订单 #" + id);
        return detail;
    }

    @GetMapping("/{id}/proof")
    public ResponseEntity<Resource> proof(@PathVariable Long id) throws IOException {
        String file = orders.proofFile(id);
        Path path = uploads.resolve(file).normalize();
        if (!path.startsWith(uploads) || !Files.isRegularFile(path)) {
            throw new ApiException(HttpStatus.NOT_FOUND, "完成凭证不存在");
        }
        return ResponseEntity.ok().contentType(MediaType.IMAGE_JPEG).body(new UrlResource(path.toUri()));
    }
}
