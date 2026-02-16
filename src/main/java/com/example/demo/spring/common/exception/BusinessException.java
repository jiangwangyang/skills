package com.example.demo.spring.common.exception;

import org.springframework.core.NestedRuntimeException;

/**
 * 业务异常
 */
public class BusinessException extends NestedRuntimeException {
    /**
     * 业务异常构造
     * @param message 异常信息
     */
    public BusinessException(String message) {
        super(message);
    }

    /**
     * 业务异常构造
     * @param message 异常信息
     * @param cause 异常原因
     */
    public BusinessException(String message, Throwable cause) {
        super(message, cause);
    }
}
