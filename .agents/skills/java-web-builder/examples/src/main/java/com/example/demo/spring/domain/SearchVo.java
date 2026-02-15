package com.example.demo.spring.domain;

/**
 * 搜索返回视图对象
 * @param id          主键ID
 * @param name        名称
 * @param description 描述
 */
public record SearchVo(
        // 主键ID
        Long id,
        // 名称
        String name,
        // 描述
        String description
) {
}
