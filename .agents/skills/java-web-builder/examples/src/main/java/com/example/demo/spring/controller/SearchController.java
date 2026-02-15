package com.example.demo.spring.controller;

import com.example.demo.spring.common.response.ApiResponse;
import com.example.demo.spring.domain.SearchDto;
import com.example.demo.spring.domain.SearchVo;
import com.example.demo.spring.service.SearchService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * 搜索控制器
 */
@RestController
@Validated
@Slf4j
public class SearchController {
    // 搜索服务
    private final SearchService searchService;

    /**
     * 搜索控制器构造
     * @param searchService 搜索服务
     */
    public SearchController(SearchService searchService) {
        this.searchService = searchService;
    }

    /**
     * 根据名称执行搜索
     * @param dto 搜索请求参数
     * @return 搜索结果
     */
    @PostMapping("/search")
    public ApiResponse<SearchVo> search(@Valid @RequestBody SearchDto dto) {
        return ApiResponse.success(searchService.search(dto));
    }
}
