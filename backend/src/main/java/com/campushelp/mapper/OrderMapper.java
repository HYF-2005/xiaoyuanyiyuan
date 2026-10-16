// MyBatis 订单数据库接口；状态变更在 UPDATE 的 WHERE 内同时校验状态/操作者，防止抢单覆盖。
package com.campushelp.mapper;

import com.campushelp.model.HelpOrder;
import java.util.List;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
// 类/接口职责：MyBatis 订单数据库接口；状态变更在 UPDATE 的 WHERE 内同时校验状态/操作者，防止抢单覆盖。
public interface OrderMapper {
    @Insert("""
        INSERT INTO orders (type, title, station, restaurant, packages_json, codes_json,
          delivery_building, delivery_address, reward, deadline_at, publisher_id)
        VALUES (#{type}, #{title}, #{station}, #{restaurant}, #{packagesJson}, #{codesJson},
          #{deliveryBuilding}, #{deliveryAddress}, #{reward}, #{deadlineAt}, #{publisherId})
        """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    // insert：创建待接单订单，数据库生成 id 和创建时间，再通过 keyProperty 回填 id。
    int insert(HelpOrder order);

    @Select("SELECT * FROM orders WHERE id = #{id}")
    // findById：按数据库订单 id 查单条原始订单，敏感信息过滤在业务层进行。
    HelpOrder findById(Long id);

    @Select("SELECT * FROM orders WHERE type = #{type} AND status = '待接单' AND (deadline_at IS NULL OR deadline_at > NOW(3)) ORDER BY created_at DESC LIMIT 100")
    // market：按类型查未过期待接单，最多 100 条；不是本人订单列表。
    List<HelpOrder> market(String type);

    @Select("SELECT * FROM orders WHERE publisher_id = #{userId} OR runner_id = #{userId} ORDER BY created_at DESC LIMIT 500")
    // mine：查用户发布/接取的最近订单，最多 500 条；业务层进一步按类型筛选。
    List<HelpOrder> mine(Long userId);

    @Select("SELECT * FROM orders WHERE runner_id = #{userId} ORDER BY created_at DESC")
    // byRunner：查该接单者的订单，供信用分与完成收入重算。
    List<HelpOrder> byRunner(Long userId);

    @Select("SELECT * FROM orders WHERE publisher_id = #{userId} ORDER BY created_at DESC")
    // byPublisher：查该发布者的订单，供发布支出和取消退回演示记录。
    List<HelpOrder> byPublisher(Long userId);

    @Select("SELECT COUNT(*) FROM orders WHERE (publisher_id = #{userId} OR runner_id = #{userId}) AND status NOT IN ('已完成', '已取消')")
    // countOpenForUser：统计本人参与且未完成/未取消的订单，注销前必须为零。
    int countOpenForUser(Long userId);

    @Update("UPDATE orders SET status = '已接单', runner_id = #{userId}, accepted_at = NOW(3) WHERE id = #{id} AND status = '待接单' AND publisher_id <> #{userId} AND (deadline_at IS NULL OR deadline_at > NOW(3))")
    // accept：UPDATE 同时要求待接单、不是本人发布、未超期；数据库原子更新防止多人同时接走一单。
    int accept(@Param("id") Long id, @Param("userId") Long userId);

    @Update("UPDATE orders SET status = '待确认', proof_file = #{file}, submitted_at = NOW(3) WHERE id = #{id} AND status = '已接单' AND runner_id = #{userId}")
    // submitProof：UPDATE 同时要求已接单且 runner_id 为本人，保存照片并转待确认。
    int submitProof(@Param("id") Long id, @Param("userId") Long userId, @Param("file") String file);

    @Update("UPDATE orders SET status = '已完成', completed_at = NOW(3) WHERE id = #{id} AND status = '待确认' AND publisher_id = #{userId} AND proof_file IS NOT NULL")
    // confirm：UPDATE 同时要求待确认、有照片且 publisher_id 为本人，记录完成时刻。
    int confirm(@Param("id") Long id, @Param("userId") Long userId);

    @Update("UPDATE orders SET status = '已取消', cancelled_at = NOW(3), cancelled_by = #{userId} WHERE id = #{id} AND status = '待接单' AND publisher_id = #{userId}")
    // cancelByPublisher：发布者只可取消自己的待接单，记录取消人和时间。
    int cancelByPublisher(@Param("id") Long id, @Param("userId") Long userId);

    @Update("UPDATE orders SET status = '已取消', cancelled_at = NOW(3), cancelled_by = #{userId} WHERE id = #{id} AND status = '已接单' AND runner_id = #{userId}")
    // cancelByRunner：接单者只可取消自己的已接单，记录取消人以便计算扣分。
    int cancelByRunner(@Param("id") Long id, @Param("userId") Long userId);
}
