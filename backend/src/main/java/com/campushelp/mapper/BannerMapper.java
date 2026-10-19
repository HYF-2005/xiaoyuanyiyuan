// MyBatis 轮播数据库接口；只返回当前启用且在有效期内的轮播。
package com.campushelp.mapper;

import com.campushelp.model.Banner;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

@Mapper
// 类/接口职责：MyBatis 轮播数据库接口；只返回当前启用且在有效期内的轮播。
public interface BannerMapper {
    @Select("""
        SELECT * FROM banners
        WHERE enabled = 1
          AND (start_at IS NULL OR start_at <= NOW(3))
          AND (end_at IS NULL OR end_at > NOW(3))
        ORDER BY sort_order ASC, id ASC
        LIMIT 20
        """)
    // listActive：按排序读取当前有效轮播，最多 20 条；不返回停用或过期内容。
    List<Banner> listActive();
}
