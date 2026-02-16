package com.example.demo.spring.common.exception;

import com.example.demo.spring.common.response.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.TypeMismatchException;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.support.DefaultHandlerExceptionResolver;

/**
 * 全局异常处理，统一处理业务异常与系统异常。
 * 打印日志并返回失败信息。
 * @see DefaultHandlerExceptionResolver
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {
    
    /**
     * 处理业务异常并返回统一响应。
     * @param e 业务异常
     * @return 失败响应
     */
    @ExceptionHandler(BusinessException.class)
    public ApiResponse<?> handleBusinessException(BusinessException e) {
        log.warn("业务异常 {} {}", e.getMessage(), e.getStackTrace()[0]);
        return ApiResponse.fail(e.getMessage());
    }

    /**
     * 处理未捕获异常并返回统一响应。
     * @param t 异常信息
     * @return 失败响应
     */
    @ExceptionHandler(Throwable.class)
    public ApiResponse<?> handleThrowable(Throwable t) {
        if (t instanceof ErrorResponse errorResponse && errorResponse.getStatusCode().is4xxClientError()) {
            HttpStatus httpStatus = HttpStatus.valueOf(errorResponse.getStatusCode().value());
            log.warn("客户端异常 {} {}", httpStatus.getReasonPhrase(), t.getMessage());
            return ApiResponse.fail(httpStatus.getReasonPhrase());
        }
        if (t instanceof TypeMismatchException || t instanceof HttpMessageNotReadableException) {
            log.warn("客户端异常 {}", t.getMessage());
            return ApiResponse.fail(t.getMessage());
        }
        log.error("服务端异常", t);
        return ApiResponse.fail("服务端异常:" + t.getMessage());
    }
}
