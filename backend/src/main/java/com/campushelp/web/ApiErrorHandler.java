// 全局异常处理器；把业务异常、重复键、格式和上传错误转成统一 message JSON。
package com.campushelp.web;

import java.util.Map;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@RestControllerAdvice
// 类/接口职责：全局异常处理器；把业务异常、重复键、格式和上传错误转成统一 message JSON。
public class ApiErrorHandler {
    @ExceptionHandler(ApiException.class)
    public ResponseEntity<Map<String,
    String>> api(ApiException error) {
        return ResponseEntity.status(error.status()).body(Map.of("message", error.getMessage()));
    }

    @ExceptionHandler(DuplicateKeyException.class)
    public ResponseEntity<Map<String,
    String>> duplicate() {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("message", "账号、手机号或订单数据已存在"));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String,
    String>> validation(MethodArgumentNotValidException error) {
        String message = error.getBindingResult().getFieldErrors().stream().findFirst()
        .map(item -> item.getDefaultMessage()).orElse("请求参数无效");
        return ResponseEntity.badRequest().body(Map.of("message", message));
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<Map<String,
    String>> oversized() {
        return ResponseEntity.badRequest().body(Map.of("message", "照片不能超过 5MB"));
    }

    @ExceptionHandler({
        HttpMessageNotReadableException.class,
        MethodArgumentTypeMismatchException.class
    })
    public ResponseEntity<Map<String,
    String>> invalidFormat() {
        return ResponseEntity.badRequest().body(Map.of("message", "请求参数格式错误，请检查日期、金额和编号"));
    }
}
