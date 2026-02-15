package com.example.demo.spring.domain;

import java.time.LocalDateTime;

/**
 * MySQL 搜索实体对象
 * @param id          主键ID
 * @param name        名称
 * @param description 描述
 * @param createTime  创建时间
 * @param updateTime  更新时间
 */
public record SearchMysqlEntity(
        // 主键ID
        Long id,
        // 名称
        String name,
        // 描述
        String description,
        // 创建时间
        LocalDateTime createTime,
        // 更新时间
        LocalDateTime updateTime
) {
}
