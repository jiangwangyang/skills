package com.example.demo.spring.config;

import org.slf4j.MDC;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;

import java.util.Map;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * 线程池配置类：包含虚拟线程池和普通线程池
 * 注意：虚拟线程池只有在JDK21及以后才使用
 */
@Configuration
public class ExecutorConfig {

    /**
     * 虚拟线程池
     * @return 虚拟线程执行器
     */
    @Bean
    public AsyncTaskExecutor virtualExecutor() {
        SimpleAsyncTaskExecutor asyncTaskExecutor = new SimpleAsyncTaskExecutor();
        asyncTaskExecutor.setVirtualThreads(true);
        asyncTaskExecutor.setThreadNamePrefix("vtask-");
        asyncTaskExecutor.setTaskDecorator(task -> {
            // 获取Request和MDC环境
            RequestAttributes attributes = RequestContextHolder.getRequestAttributes();
            Map<String, String> contextMap = MDC.getCopyOfContextMap();
            return () -> {
                try {
                    // 设置环境
                    if (attributes != null) {
                        RequestContextHolder.setRequestAttributes(attributes);
                    }
                    if (contextMap != null) {
                        MDC.setContextMap(contextMap);
                    }
                    task.run();
                } finally {
                    // 清理环境
                    RequestContextHolder.resetRequestAttributes();
                    MDC.clear();
                }
            };
        });
        return asyncTaskExecutor;
    }

    /**
     * 普通线程池
     * @return 普通线程执行器
     */
    @Bean
    public AsyncTaskExecutor threadExecutor() {
        ThreadPoolTaskExecutor asyncTaskExecutor = new ThreadPoolTaskExecutor();
        asyncTaskExecutor.setVirtualThreads(false);
        asyncTaskExecutor.setThreadNamePrefix("task-");
        asyncTaskExecutor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        asyncTaskExecutor.setQueueCapacity(0);
        asyncTaskExecutor.setTaskDecorator(task -> {
            // 获取Request和MDC环境
            RequestAttributes attributes = RequestContextHolder.getRequestAttributes();
            Map<String, String> contextMap = MDC.getCopyOfContextMap();
            return () -> {
                try {
                    // 设置环境
                    if (attributes != null) {
                        RequestContextHolder.setRequestAttributes(attributes);
                    }
                    if (contextMap != null) {
                        MDC.setContextMap(contextMap);
                    }
                    task.run();
                } finally {
                    // 清理环境
                    RequestContextHolder.resetRequestAttributes();
                    MDC.clear();
                }
            };
        });
        asyncTaskExecutor.initialize();
        return asyncTaskExecutor;
    }
}
