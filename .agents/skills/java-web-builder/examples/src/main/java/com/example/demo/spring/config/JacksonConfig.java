package com.example.demo.spring.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import tools.jackson.databind.ext.javatime.deser.LocalDateTimeDeserializer;
import tools.jackson.databind.ext.javatime.ser.LocalDateTimeSerializer;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.module.SimpleModule;

import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.TimeZone;

/**
 * Jackson JsonMapper 配置类
 * 配置LocalDateTime日期时间序列化格式
 */
@Configuration
public class JacksonConfig {
    // 时间格式化模板
    public static final String DATE_TIME_PATTERN = "yyyy-MM-dd HH:mm:ss";
    // 时区（东八区）
    public static final String TIME_ZONE = "GMT+8";

    /**
     * JsonMapper 配置
     * @return JsonMapper 实例
     */
    @Bean
    public JsonMapper jsonMapper() {
        // Java 8时间模块
        SimpleModule simpleModule = new SimpleModule();
        simpleModule.addSerializer(LocalDateTime.class, new LocalDateTimeSerializer(DateTimeFormatter.ofPattern(DATE_TIME_PATTERN)));
        simpleModule.addDeserializer(LocalDateTime.class, new LocalDateTimeDeserializer(DateTimeFormatter.ofPattern(DATE_TIME_PATTERN)));
        return JsonMapper.builder()
                // 注册Java 8时间模块（必须）配置LocalDateTime的序列化/反序列化器（核心解决时间格式问题）
                .addModule(simpleModule)
                // 配置java.util.Date时区和格式（等价于yml中的time-zone:GMT+8和date-format）
                .defaultTimeZone(TimeZone.getTimeZone(TIME_ZONE))
                .defaultDateFormat(new SimpleDateFormat(DATE_TIME_PATTERN))
                .build();
    }
}
