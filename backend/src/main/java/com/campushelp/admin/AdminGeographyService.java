// 地理信息只读业务；没有坐标时只展示订单地点分布，不伪造地图点位。
package com.campushelp.admin;

import com.campushelp.mapper.AdminGeographyMapper;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class AdminGeographyService {
    private final AdminGeographyMapper geography;

    public AdminGeographyService(AdminGeographyMapper geography) {
        this.geography = geography;
    }

    public record Point(String name, String kind, long orderCount, BigDecimal totalAmount) {}

    public record Overview(long distinctPickupPoints, List<Point> pickupPoints, List<Point> deliveryBuildings) {}

    public Overview overview() {
        List<Point> pickups = geography.pickupPoints().stream()
            .map(row -> new Point(row.getName(), row.getKind(),
                row.getOrderCount() == null ? 0 : row.getOrderCount(),
                row.getTotalAmount() == null ? BigDecimal.ZERO : row.getTotalAmount()))
            .toList();
        List<Point> buildings = geography.deliveryBuildings().stream()
            .map(row -> new Point(row.getName(), "送达",
                row.getOrderCount() == null ? 0 : row.getOrderCount(),
                row.getTotalAmount() == null ? BigDecimal.ZERO : row.getTotalAmount()))
            .toList();
        return new Overview(geography.distinctPickupPoints(), pickups, buildings);
    }
}
