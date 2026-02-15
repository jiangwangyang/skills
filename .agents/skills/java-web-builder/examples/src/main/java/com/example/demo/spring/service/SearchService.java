package com.example.demo.spring.service;

import com.example.demo.spring.domain.SearchDto;
import com.example.demo.spring.domain.SearchVo;

/**
 * 搜索业务服务接口
 */
public interface SearchService {

    /**
     * 根据请求参数执行搜索
     * @param dto 搜索请求参数
     * @return 搜索返回对象
     */
    SearchVo search(SearchDto dto);
}
