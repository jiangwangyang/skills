package com.example.demo.spring.common.response;

import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * API接口响应封装类
 * @param <T> 响应数据类型
 */
@Data
public class ApiResponse<T> {
    private Boolean success;
    private String message;
    private Long current;
    private Long size;
    private Long total;
    private T data;
    private Map<String, Object> extra;

    /**
     * 返回成功响应（无数据）
     * @param <T> 响应数据类型
     * @return 成功响应
     */
    public static <T> ApiResponse<T> success() {
        ApiResponse<T> apiResponse = new ApiResponse<>();
        apiResponse.setSuccess(true);
        apiResponse.setMessage("success");
        return apiResponse;
    }

    /**
     * 返回成功响应（带数据）
     * @param data 响应数据
     * @param <T> 响应数据类型
     * @return 成功响应
     */
    public static <T> ApiResponse<T> success(T data) {
        ApiResponse<T> apiResponse = success();
        apiResponse.setData(data);
        if (data instanceof List<?> list) {
            apiResponse.setCurrent(1L);
            apiResponse.setSize((long) list.size());
            apiResponse.setTotal((long) list.size());
        }
        return apiResponse;
    }

    /**
     * 返回成功响应（带分页信息）
     * @param data 响应数据
     * @param current 当前页
     * @param size 每页大小
     * @param total 总数
     * @param <T> 响应数据类型
     * @return 成功响应
     */
    public static <T> ApiResponse<T> success(T data, Long current, Long size, Long total) {
        ApiResponse<T> apiResponse = success();
        apiResponse.setData(data);
        apiResponse.setCurrent(current);
        apiResponse.setSize(size);
        apiResponse.setTotal(total);
        return apiResponse;
    }

    /**
     * 返回失败响应
     * @param message 失败原因
     * @param <T> 响应数据类型
     * @return 失败响应
     */
    public static <T> ApiResponse<T> fail(String message) {
        ApiResponse<T> apiResponse = new ApiResponse<>();
        apiResponse.setSuccess(false);
        apiResponse.setMessage(message);
        return apiResponse;
    }
}
