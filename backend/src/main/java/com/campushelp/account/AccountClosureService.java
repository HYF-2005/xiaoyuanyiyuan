// 账号停用业务；校验密码和未完成订单，事务内停用账号并清理会话，保留历史。
package com.campushelp.account;

import com.campushelp.mapper.OrderMapper;
import com.campushelp.mapper.SessionMapper;
import com.campushelp.mapper.UserMapper;
import com.campushelp.model.User;
import com.campushelp.web.ApiException;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// @Service 交给 Spring 管理业务对象，控制器通过构造器依赖它。
@Service
// 类/接口职责：账号停用业务；校验密码和未完成订单，事务内停用账号并清理会话，保留历史。
public class AccountClosureService {
    // users：用户数据库访问接口；final 引用在构造后不再换成别的对象。
    private final UserMapper users;
    // orders：订单数据库访问接口/订单业务依赖，具体类型见声明；final 引用在构造后不再换成别的对象。
    private final OrderMapper orders;
    // sessions：登录会话数据库接口；final 引用在构造后不再换成别的对象。
    private final SessionMapper sessions;
    // encoder：BCrypt 编码与校验工具，不把密码以明文保存；final 引用在构造后不再换成别的对象。
    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    // AccountClosureService：构造器由 Spring 注入所需依赖（测试中可手动创建）；不在每次请求中重复 new 数据库工具。
    public AccountClosureService(UserMapper users, OrderMapper orders, SessionMapper sessions) {
        this.users = users;
        this.orders = orders;
        this.sessions = sessions;
    }

    // 事务：本方法数据库更新要么整体成功，要么运行时异常时回滚；不是对照片文件的自动回滚。
    @Transactional
    // deactivate：核对密码并阻止有未完成订单的用户注销；事务内 soft delete（active=0）并删除全部登录会话。
    public void deactivate(Long userId, String password) {
        User user = users.findById(userId);
        if (user == null || password == null || !encoder.matches(password, user.getPasswordHash())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "密码错误");
        }
        if (orders.countOpenForUser(userId) != 0) {
            throw new ApiException(HttpStatus.CONFLICT, "还有未完成订单，暂不能注销账号");
        }
        if (users.deactivate(userId) != 1) {
            throw new ApiException(HttpStatus.CONFLICT, "账号状态已改变，请刷新后重试");
        }
        sessions.deleteByUserId(userId);
    }
}
