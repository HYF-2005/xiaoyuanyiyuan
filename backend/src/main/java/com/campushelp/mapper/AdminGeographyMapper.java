// 地理信息只读统计；按订单取件点和送达建筑聚合，不含精确经纬度。
package com.campushelp.mapper;

import com.campushelp.admin.AdminGeographyRow;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface AdminGeographyMapper {
    @Select("""
        SELECT COALESCE(NULLIF(station, ''), restaurant) AS name,
               IF(type = 'meal', '取餐', '取件') AS kind,
               COUNT(*) AS order_count,
               COALESCE(SUM(reward), 0) AS total_amount
        FROM orders
        WHERE COALESCE(NULLIF(station, ''), restaurant) IS NOT NULL
        GROUP BY COALESCE(NULLIF(station, ''), restaurant), kind
        ORDER BY order_count DESC, name ASC
        """)
    List<AdminGeographyRow.Point> pickupPoints();

    @Select("""
        SELECT delivery_building AS name, COUNT(*) AS order_count,
               COALESCE(SUM(reward), 0) AS total_amount
        FROM orders
        GROUP BY delivery_building
        ORDER BY order_count DESC, name ASC
        """)
    List<AdminGeographyRow.Point> deliveryBuildings();

    @Select("""
        SELECT COUNT(DISTINCT COALESCE(NULLIF(station, ''), restaurant)) FROM orders
        """)
    long distinctPickupPoints();
}
