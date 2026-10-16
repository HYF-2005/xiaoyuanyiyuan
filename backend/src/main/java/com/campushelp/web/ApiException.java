// 业务异常对象；同时携带 HTTP 状态和人能读的错误消息，交给全局异常处理器。
package com.campushelp.web;

import org.springframework.http.HttpStatus;

// 类/接口职责：业务异常对象；同时携带 HTTP 状态和人能读的错误消息，交给全局异常处理器。
public class ApiException extends RuntimeException {
    // status：异常携带的 HTTP 状态；final 引用在构造后不再换成别的对象。
    private final HttpStatus status;

    // ApiException：构造器由 Spring 注入所需依赖（测试中可手动创建）；不在每次请求中重复 new 数据库工具。
    public ApiException(HttpStatus status, String message) {
        super(message);
        this.status = status;
    }

    // status：提供异常携带的 HTTP 状态，供错误处理器构造响应。
    public HttpStatus status() {
        return status;
    }
}
