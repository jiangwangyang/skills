package com.example.demo.spring;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.resilience.annotation.EnableResilientMethods;

/**
 * Spring Boot 启动入口
 */
@EnableResilientMethods
@SpringBootApplication
public class MainApplication {
    /**
     * 应用启动
     * @param args 启动参数
     */
    public static void main(String[] args) {
        SpringApplication.run(MainApplication.class, args);
    }
}
