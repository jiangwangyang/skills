package com.example.demo.spring.client;

import com.example.demo.spring.domain.SearchMysqlEntity;
import lombok.extern.slf4j.Slf4j;
import org.springframework.resilience.annotation.ConcurrencyLimit;
import org.springframework.resilience.annotation.Retryable;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * MySQL 搜索数据客户端
 * 访问外部数据必须进行限流和重试
 */
@Repository
@Slf4j
public class SearchMysqlClient {

    /**
     * 根据名称模拟查询 MySQL 数据
     * @param name 搜索名称
     * @return MySQL 查询结果
     */
    @ConcurrencyLimit(100)
    @Retryable
    public Optional<SearchMysqlEntity> findByName(String name) {
        log.info("SearchMysqlClient.findByName: {}", name);
        if (name != null && name.startsWith("mysql")) {
            SearchMysqlEntity entity = new SearchMysqlEntity(
                    2001L,
                    name,
                    "MySQL模拟描述",
                    LocalDateTime.now().minusDays(1),
                    LocalDateTime.now()
            );
            return Optional.of(entity);
        }
        return Optional.empty();
    }
}
