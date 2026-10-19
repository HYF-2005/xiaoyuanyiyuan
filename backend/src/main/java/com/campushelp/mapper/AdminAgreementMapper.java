// 协议后台数据库接口。
package com.campushelp.mapper;

import com.campushelp.model.Agreement;
import java.util.List;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface AdminAgreementMapper {
    @Select("SELECT * FROM agreements ORDER BY id ASC")
    List<Agreement> list();

    @Select("SELECT * FROM agreements WHERE id = #{id}")
    Agreement findById(Long id);

    @Select("SELECT COUNT(*) FROM agreements WHERE code = #{code} AND id <> #{id}")
    int codeCount(@Param("code") String code, @Param("id") Long id);

    @Insert("""
        INSERT INTO agreements (code, title, version, content, is_published)
        VALUES (#{code}, #{title}, #{version}, #{content}, #{isPublished})
        """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(Agreement agreement);

    @Update("""
        UPDATE agreements SET code = #{code}, title = #{title}, version = #{version},
          content = #{content}, is_published = #{isPublished}
        WHERE id = #{id}
        """)
    int update(Agreement agreement);

    @Delete("DELETE FROM agreements WHERE id = #{id}")
    int delete(Long id);
}
