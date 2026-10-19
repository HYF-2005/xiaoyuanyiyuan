// 地址字典数据访问；父子关系由 service 校验，SQL 只做无状态读写。
package com.campushelp.mapper;

import com.campushelp.model.Location;
import java.util.List;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface LocationMapper {
    @Select("SELECT id, kind, parent_id, name, sort_order, enabled, created_at, updated_at " +
        "FROM locations ORDER BY FIELD(kind, 'station', 'canteen', 'floor', 'vendor', 'building_category', 'building'), parent_id, sort_order, id")
    List<Location> findAll();

    @Select("SELECT id, kind, parent_id, name, sort_order, enabled, created_at, updated_at " +
        "FROM locations WHERE enabled = 1 ORDER BY parent_id, sort_order, id")
    List<Location> findEnabled();

    @Select("SELECT id, kind, parent_id, name, sort_order, enabled, created_at, updated_at " +
        "FROM locations WHERE id = #{id}")
    Location findById(@Param("id") Long id);

    @Select("SELECT COUNT(*) FROM locations WHERE kind = #{kind} AND parent_id = #{parentId} " +
        "AND name = #{name} AND id <> #{excludeId}")
    int countSame(@Param("kind") String kind, @Param("parentId") Long parentId,
        @Param("name") String name, @Param("excludeId") Long excludeId);

    @Select("SELECT COUNT(*) FROM locations WHERE parent_id = #{id}")
    int countChildren(@Param("id") Long id);

    @Select("SELECT id, kind, parent_id, name, sort_order, enabled, created_at, updated_at " +
        "FROM locations WHERE parent_id = #{id} ORDER BY sort_order, id")
    List<Location> findChildren(@Param("id") Long id);

    @Insert("INSERT INTO locations (kind, parent_id, name, sort_order, enabled) " +
        "VALUES (#{kind}, #{parentId}, #{name}, #{sortOrder}, #{enabled})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(Location location);

    @Update("UPDATE locations SET kind = #{kind}, parent_id = #{parentId}, name = #{name}, " +
        "sort_order = #{sortOrder}, enabled = #{enabled}, updated_at = CURRENT_TIMESTAMP(3) WHERE id = #{id}")
    int update(Location location);

    @Delete("DELETE FROM locations WHERE id = #{id}")
    int delete(@Param("id") Long id);
}
