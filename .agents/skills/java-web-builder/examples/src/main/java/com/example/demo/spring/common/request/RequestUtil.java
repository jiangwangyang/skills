package com.example.demo.spring.common.request;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.slf4j.MDC;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Objects;

/**
 * 请求工具类，用于获取请求相关的各种信息
 */
public class RequestUtil {
    /**
     * 尝试获取 traceId
     * @return traceId，可能为空
     */
    @Nullable
    public static String tryGetTraceId() {
        return MDC.get(TraceFilter.TRACE_ID_KEY);
    }

    /**
     * 尝试获取 userId
     * @return userId，可能为空
     */
    @Nullable
    public static Integer tryGetUserId() {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes == null) {
            return null;
        }
        return (Integer) attributes.getRequest().getAttribute(UserFilter.USER_ID_KEY);
    }

    /**
     * 获取 traceId（为空则抛异常）
     * @return traceId
     */
    @Nonnull
    public static String getTraceId() {
        return Objects.requireNonNull(tryGetTraceId());
    }

    /**
     * 获取 userId（为空则抛异常）
     * @return userId
     */
    @Nonnull
    public static Integer getUserId() {
        return Objects.requireNonNull(tryGetUserId());
    }
}
