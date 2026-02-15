package com.example.demo.spring;

import com.example.demo.spring.common.response.ApiResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.json.JsonMapper;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 搜索接口集成测试
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class SearchApplicationTest {
    @Autowired
    private JsonMapper jsonMapper;
    @Autowired
    private RestClient restClient;
    @Value("${server.servlet.context-path}")
    private String contextPath;
    @LocalServerPort
    private int port;

    /**
     * 验证 MySQL 分支
     */
    @Test
    void testSearch1() {
        String dto = """
                {
                    "name": "mysql"
                }
                """;
        String vo = """
                {
                    "id": 2001,
                    "name": "mysql",
                    "description": "MySQL模拟描述"
                }
                """;
        ApiResponse<Object> apiResponse = restClient
                .post()
                .uri("http://localhost:%d%s/search".formatted(port, contextPath))
                .contentType(MediaType.APPLICATION_JSON)
                .header("X-Trace-Id", UUID.randomUUID().toString().replace("-", ""))
                .header("Authorization", "Bearer " + UUID.randomUUID().toString().replace("-", ""))
                .body(dto)
                .retrieve()
                .body(new ParameterizedTypeReference<>() {
                });
        System.out.println(jsonMapper.writerWithDefaultPrettyPrinter().writeValueAsString(apiResponse));
        assertThat(apiResponse).isNotNull()
                .hasFieldOrPropertyWithValue("success", true)
                .hasFieldOrPropertyWithValue("message", "success")
                .hasFieldOrPropertyWithValue("data", jsonMapper.readValue(vo, Object.class));
    }

    /**
     * 验证 Redis 分支
     */
    @Test
    void testSearch2() {
        String dto = """
                {
                    "name": "redis"
                }
                """;
        String vo = """
                {
                    "id": 1001,
                    "name": "redis",
                    "description": "Redis模拟描述"
                }
                """;
        ApiResponse<Object> apiResponse = restClient
                .post()
                .uri("http://localhost:%d%s/search".formatted(port, contextPath))
                .contentType(MediaType.APPLICATION_JSON)
                .header("X-Trace-Id", UUID.randomUUID().toString().replace("-", ""))
                .header("Authorization", "Bearer " + UUID.randomUUID().toString().replace("-", ""))
                .body(dto)
                .retrieve()
                .body(new ParameterizedTypeReference<>() {
                });
        System.out.println(jsonMapper.writerWithDefaultPrettyPrinter().writeValueAsString(apiResponse));
        assertThat(apiResponse).isNotNull()
                .hasFieldOrPropertyWithValue("success", true)
                .hasFieldOrPropertyWithValue("message", "success")
                .hasFieldOrPropertyWithValue("data", jsonMapper.readValue(vo, Object.class));
    }

    /**
     * 验证 MySQL 未命中时返回失败
     */
    @Test
    void testSearch3() {
        String dto = """
                {
                    "name": "missing-data"
                }
                """;
        ApiResponse<Object> apiResponse = restClient
                .post()
                .uri("http://localhost:%d%s/search".formatted(port, contextPath))
                .contentType(MediaType.APPLICATION_JSON)
                .header("X-Trace-Id", UUID.randomUUID().toString().replace("-", ""))
                .header("Authorization", "Bearer " + UUID.randomUUID().toString().replace("-", ""))
                .body(dto)
                .retrieve()
                .body(new ParameterizedTypeReference<>() {
                });
        System.out.println(jsonMapper.writerWithDefaultPrettyPrinter().writeValueAsString(apiResponse));
        assertThat(apiResponse).isNotNull()
                .hasFieldOrPropertyWithValue("success", false)
                .hasFieldOrPropertyWithValue("message", "未查询到数据");
    }

    /**
     * 验证参数不合法时返回失败
     */
    @Test
    void testSearch4() {
        String dto = """
                {
                    "name": ""
                }
                """;
        ApiResponse<Object> apiResponse = restClient
                .post()
                .uri("http://localhost:%d%s/search".formatted(port, contextPath))
                .contentType(MediaType.APPLICATION_JSON)
                .header("X-Trace-Id", UUID.randomUUID().toString().replace("-", ""))
                .header("Authorization", "Bearer " + UUID.randomUUID().toString().replace("-", ""))
                .body(dto)
                .retrieve()
                .body(new ParameterizedTypeReference<>() {
                });
        System.out.println(jsonMapper.writerWithDefaultPrettyPrinter().writeValueAsString(apiResponse));
        assertThat(apiResponse).isNotNull()
                .hasFieldOrPropertyWithValue("success", false)
                .hasFieldOrPropertyWithValue("message", "Bad Request");
    }
}
