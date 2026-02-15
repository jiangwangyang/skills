package com.example.demo.spring.common.request;

import com.example.demo.spring.common.response.ApiResponse;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * User过滤器，用于校验用户信息，并给每个请求添加UserID，可通过RequestUtil获取
 * UserID用于日志跟踪，帮助定位用户请求的处理路径
 * @see RequestUtil
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 1)
public class UserFilter implements Filter {
    public static final String USER_ID_KEY = "userid";

    private final JsonMapper jsonMapper;

    /**
     * 用户过滤器构造
     * @param jsonMapper JsonMapper
     */
    public UserFilter(JsonMapper jsonMapper) {
        this.jsonMapper = jsonMapper;
    }

    /**
     * 校验用户并注入 userId
     * @param servletRequest 请求
     * @param servletResponse 响应
     * @param filterChain 过滤器链
     * @throws IOException IO异常
     * @throws ServletException 过滤异常
     */
    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {
        if (!(servletRequest instanceof HttpServletRequest httpServletRequest) || !(servletResponse instanceof HttpServletResponse httpServletResponse)) {
            return;
        }

        try {
            // 获取token
            String token = httpServletRequest.getHeader("Authorization");

            // 模拟校验token
            if (!StringUtils.hasText(token)) {
                httpServletResponse.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                httpServletResponse.setContentType(MediaType.APPLICATION_JSON_VALUE);
                httpServletResponse.setCharacterEncoding(StandardCharsets.UTF_8);
                httpServletResponse.getWriter().write(jsonMapper.writeValueAsString(ApiResponse.fail("未登录或登录已过期")));
                return;
            }
            Long userId = 12345L;

            // 注入请求信息
            MDC.put(USER_ID_KEY, userId.toString());
            httpServletRequest.setAttribute(USER_ID_KEY, userId);

            // 继续处理请求
            filterChain.doFilter(servletRequest, servletResponse);
        } finally {
            // 移除 TraceID
            MDC.remove(USER_ID_KEY);
        }
    }
}
