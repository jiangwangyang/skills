package com.example.demo.spring.common.request;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.util.Enumeration;
import java.util.UUID;

/**
 * Trace过滤器，用于给每个请求添加TraceID，可通过RequestUtil获取
 * TraceID用于日志跟踪，帮助定位请求的处理路径
 * @see RequestUtil
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class TraceFilter implements Filter {
    public static final String TRACE_ID_KEY = "traceid";

    /**
     * 注入 traceId 并透传至响应头
     * @param servletRequest 请求
     * @param servletResponse 响应
     * @param filterChain 过滤器链
     * @throws ServletException 过滤异常
     * @throws IOException IO异常
     */
    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws ServletException, IOException {
        if (!(servletRequest instanceof HttpServletRequest httpServletRequest) || !(servletResponse instanceof HttpServletResponse httpServletResponse)) {
            return;
        }

        try {
            // 从请求头获取 TraceID
            String traceId = "";
            Enumeration<String> headerNames = httpServletRequest.getHeaderNames();
            if (headerNames != null) {
                while (headerNames.hasMoreElements()) {
                    String name = headerNames.nextElement();
                    if (name.toLowerCase().contains("trace-id") || name.toLowerCase().contains("traceid")) {
                        String value = httpServletRequest.getHeader(name);
                        if (StringUtils.hasText(value)) {
                            traceId = value.replace("-", "");
                            break;
                        }
                    }
                }
            }

            // 没有则随机生成
            if (!StringUtils.hasText(traceId)) {
                traceId = UUID.randomUUID().toString().replace("-", "");
            }

            // 设置 TraceID
            MDC.put(TRACE_ID_KEY, traceId);
            httpServletRequest.setAttribute(TRACE_ID_KEY, traceId);
            httpServletResponse.setHeader("X-Trace-Id", traceId);

            // 继续处理请求
            filterChain.doFilter(servletRequest, servletResponse);
        } finally {
            // 移除 TraceID
            MDC.remove(TRACE_ID_KEY);
        }
    }
}
