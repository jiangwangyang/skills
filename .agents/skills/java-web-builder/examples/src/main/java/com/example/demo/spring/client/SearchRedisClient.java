package com.example.demo.spring.client;

import com.example.demo.spring.domain.SearchRedisEntity;
import lombok.extern.slf4j.Slf4j;
import org.springframework.resilience.annotation.ConcurrencyLimit;
import org.springframework.resilience.annotation.Retryable;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * Redis 搜索数据客户端
 * 访问外部数据必须进行限流和重试
 */
@Repository
@Slf4j
public class SearchRedisClient {

    /**
     * 根据名称模拟查询 Redis 数据
     * @param name 搜索名称
     * @return Redis 查询结果
     */
    @ConcurrencyLimit(100)
    @Retryable
    public Optional<SearchRedisEntity> findByName(String name) {
        log.info("SearchRedisClient.findByName: {}", name);
        if (name != null && name.startsWith("redis")) {
            SearchRedisEntity entity = new SearchRedisEntity(
                    1001L,
                    name,
                    "Redis模拟描述",
                    LocalDateTime.now().minusHours(2),
                    LocalDateTime.now()
            );
            return Optional.of(entity);
        }
        return Optional.empty();
    }
}
