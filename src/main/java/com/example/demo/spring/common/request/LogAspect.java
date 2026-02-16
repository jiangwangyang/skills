package com.example.demo.spring.common.request;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

/**
 * 日志切面，记录请求参数、结果和时间
 */
@Component
@Aspect
@Order(Ordered.HIGHEST_PRECEDENCE)
@Slf4j
public class LogAspect {
    private final JsonMapper jsonMapper;

    /**
     * 日志切面构造
     * @param jsonMapper JsonMapper
     */
    public LogAspect(JsonMapper jsonMapper) {
        this.jsonMapper = jsonMapper;
    }

    /**
     * 切入点：匹配所有 Controller/RestController 类中的方法
     * @see org.springframework.stereotype.Controller
     * @see org.springframework.web.bind.annotation.RestController
     */
    @Pointcut("@within(org.springframework.stereotype.Controller)" +
            "|| @within(org.springframework.web.bind.annotation.RestController)")
    public void classPointcut() {
        // 切入点定义，无实际代码
    }

    /**
     * 切入点：匹配所有请求映射注解的方法
     * @see org.springframework.web.bind.annotation.RequestMapping
     * @see org.springframework.web.bind.annotation.GetMapping
     * @see org.springframework.web.bind.annotation.PostMapping
     * @see org.springframework.web.bind.annotation.PutMapping
     * @see org.springframework.web.bind.annotation.DeleteMapping
     */
    @Pointcut("@annotation(org.springframework.web.bind.annotation.RequestMapping)" +
            "|| @annotation(org.springframework.web.bind.annotation.GetMapping)" +
            "|| @annotation(org.springframework.web.bind.annotation.PostMapping)" +
            "|| @annotation(org.springframework.web.bind.annotation.PutMapping)" +
            "|| @annotation(org.springframework.web.bind.annotation.DeleteMapping)")
    public void methodPointcut() {
        // 切入点定义，无实际代码
    }

    /**
     * 记录请求参数、响应结果与耗时
     * @param joinPoint 切点
     * @return 调用结果
     * @throws Throwable 执行异常
     */
    @Around("classPointcut() && methodPointcut()")
    public Object logAround(ProceedingJoinPoint joinPoint) throws Throwable {
        long startNanoTime = System.nanoTime();

        // 记录请求参数和结果
        log.info("Request: {}", jsonMapper.writeValueAsString(joinPoint.getArgs()));
        Object result = joinPoint.proceed();
        log.info("Response: {}", jsonMapper.writeValueAsString(result));

        // 记录时间
        long endNanoTime = System.nanoTime();
        long duration = endNanoTime - startNanoTime;
        if (duration > 1_000_000_000) {
            log.warn("Request took too long: {} ms", (int) (duration / 1_000_000));
        } else {
            log.info("Request took: {} ms", (int) (duration / 1_000_000));
        }

        // 返回
        return result;
    }
}
