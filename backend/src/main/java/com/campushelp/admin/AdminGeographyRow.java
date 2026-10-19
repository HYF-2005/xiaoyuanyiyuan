// 地理统计行对象。
package com.campushelp.admin;

import java.math.BigDecimal;
import lombok.Data;

public final class AdminGeographyRow {
    private AdminGeographyRow() {}

    @Data
    public static class Point {
        private String name;
        private String kind;
        private Long orderCount;
        private BigDecimal totalAmount;
    }
}
