package com.example.demo.spring.service.impl;

import com.example.demo.spring.client.SearchMysqlClient;
import com.example.demo.spring.client.SearchRedisClient;
import com.example.demo.spring.common.exception.BusinessException;
import com.example.demo.spring.domain.SearchDto;
import com.example.demo.spring.domain.SearchMysqlEntity;
import com.example.demo.spring.domain.SearchRedisEntity;
import com.example.demo.spring.domain.SearchVo;
import com.example.demo.spring.service.SearchService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * 搜索业务服务实现
 */
@Service
@Slf4j
public class SearchServiceImpl implements SearchService {
    // Redis 搜索客户端
    private final SearchRedisClient searchRedisClient;
    // MySQL 搜索客户端
    private final SearchMysqlClient searchMysqlClient;

    /**
     * 搜索服务构造
     * @param searchRedisClient Redis 搜索客户端
     * @param searchMysqlClient MySQL 搜索客户端
     */
    public SearchServiceImpl(SearchRedisClient searchRedisClient, SearchMysqlClient searchMysqlClient) {
        this.searchRedisClient = searchRedisClient;
        this.searchMysqlClient = searchMysqlClient;
    }

    /**
     * 先查 Redis，未命中时再查 MySQL
     * @param dto 搜索请求参数
     * @return 搜索返回对象
     */
    @Override
    public SearchVo search(SearchDto dto) {
        Optional<SearchRedisEntity> redisOptional = searchRedisClient.findByName(dto.name());
        if (redisOptional.isPresent()) {
            SearchRedisEntity redisEntity = redisOptional.get();
            return new SearchVo(redisEntity.id(), redisEntity.name(), redisEntity.description());
        }

        SearchMysqlEntity mysqlEntity = searchMysqlClient.findByName(dto.name())
                .orElseThrow(() -> new BusinessException("未查询到数据"));
        return new SearchVo(mysqlEntity.id(), mysqlEntity.name(), mysqlEntity.description());
    }
}
