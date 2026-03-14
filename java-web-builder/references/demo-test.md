# 示例测试代码

## DemoApplicationTest

```java
/**
 * Demo 接口集成测试
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class DemoApplicationTest {
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
    void testDemo1() {
        String dto = """
                {
                    "name": "test"
                }
                """;
        String vo = """
                {
                    "id": 2001,
                    "name": "test",
                    "description": "MySQL模拟描述"
                }
                """;
        ApiResponse<Object> apiResponse = restClient
                .post()
                .uri("http://localhost:%d%s/demo".formatted(port, contextPath))
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
     * 验证参数不合法时返回失败
     */
    @Test
    void testDemo2() {
        String dto = """
                {
                    "name": ""
                }
                """;
        ApiResponse<Object> apiResponse = restClient
                .post()
                .uri("http://localhost:%d%s/demo".formatted(port, contextPath))
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
```