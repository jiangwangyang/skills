package com.example.demo.spring.domain;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 搜索请求数据传输对象
 * @param name 搜索名称
 */
public record SearchDto(
        // 搜索名称
        @NotBlank(message = "name不能为空")
        @Size(max = 64, message = "name长度不能超过64")
        String name
) {
}
