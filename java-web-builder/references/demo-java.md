# 示例代码

## Controller

```java
/**
 * Demo 控制器
 */
@RestController
@Validated
@Slf4j
public class DemoController {
    // Demo 服务
    private final DemoService demoService;

    /**
     * Demo 控制器构造
     * @param demoService Demo 服务
     */
    public DemoController(DemoService demoService) {
        this.demoService = demoService;
    }

    /**
     * 根据名称执行 Demo 查询
     * @param dto Demo 请求参数
     * @return Demo 结果
     */
    @PostMapping("/demo")
    public ApiResponse<DemoVo> demo(@Valid @RequestBody DemoDto dto) {
        return ApiResponse.success(demoService.query(dto));
    }
}
```

## Service

```java
/**
 * Demo 业务服务接口
 */
public interface DemoService {

    /**
     * 根据请求参数执行查询
     * @param dto 请求参数
     * @return 返回对象
     */
    DemoVo query(DemoDto dto);
}
```

```java
/**
 * Demo 业务服务实现
 */
@Service
@Slf4j
public class DemoServiceImpl implements DemoService {
    // MySQL 客户端
    private final DemoMysqlClient demoMysqlClient;

    /**
     * Demo 服务构造
     * @param demoMysqlClient MySQL 客户端
     */
    public DemoServiceImpl(DemoMysqlClient demoMysqlClient) {
        this.demoMysqlClient = demoMysqlClient;
    }

    /**
     * 直接查询 MySQL 返回结果
     * @param dto 请求参数
     * @return 返回对象
     */
    @Override
    public DemoVo query(DemoDto dto) {
        DemoMysqlEntity mysqlEntity = demoMysqlClient.findByName(dto.name())
                .orElseThrow(() -> new BusinessException("未查询到数据"));
        return new DemoVo(mysqlEntity.id(), mysqlEntity.name(), mysqlEntity.description());
    }
}
```

## Client

```java
/**
 * MySQL Demo 数据客户端
 * 访问外部数据必须进行限流和重试
 */
@Repository
@Slf4j
public class DemoMysqlClient {

    /**
     * 根据名称查询 MySQL 数据
     * @param name 名称
     * @return MySQL 查询结果
     */
    @ConcurrencyLimit(100)
    @Retryable
    public Optional<DemoMysqlEntity> findByName(String name) {
        log.info("DemoMysqlClient.findByName: {}", name);
        if (name != null && !name.isBlank()) {
            DemoMysqlEntity entity = new DemoMysqlEntity(
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
```

## Domain

```java
/**
 * Demo 请求数据传输对象
 * @param name 名称
 */
public record DemoDto(
                // 名称
                @NotBlank(message = "name不能为空")
                @Size(max = 64, message = "name长度不能超过64")
                String name
        ) {
}
```

```java
/**
 * Demo 返回视图对象
 * @param id          主键ID
 * @param name        名称
 * @param description 描述
 */
public record DemoVo(
                // 主键ID
                Long id,
                // 名称
                String name,
                // 描述
                String description
        ) {
}
```

```java
/**
 * MySQL Demo 实体对象
 * @param id          主键ID
 * @param name        名称
 * @param description 描述
 * @param createTime  创建时间
 * @param updateTime  更新时间
 */
public record DemoMysqlEntity(
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
```
