# Java Web 项目规范（Spring Boot）

## 构建命令

- 编译：`mvn clean compile`
- 测试：`mvn test`
- 单测类：`mvn test -Dtest=SearchApplicationTest`
- 单测方法：`mvn test -Dtest=SearchApplicationTest#testSearch1`
- 打包：`mvn clean package`
- 启动：`mvn spring-boot:run`

## 目录结构

```text
demo-spring/
├─ src/main/java/com/example/demo/spring/
│  ├─ MainApplication.java
│  ├─ controller/
│  ├─ service/
│  ├─ client/
│  ├─ domain/
│  ├─ common/
│  └─ config/
├─ src/main/resources/
├─ src/test/java/
├─ docs/
├─ README.md
├─ AGENTS.md
├─ RECORDS.md
└─ pom.xml
```

## 分层职责

- `controller`：仅接收入参、校验、调用 `service`、封装 `ApiResponse<T>`。
- `service`：业务编排与回退策略（如 Redis 未命中回退 MySQL）。
- `client`：外部访问与数据映射，不做业务决策；方法必须限流和重试。
- `domain`：仅 DTO/VO/Entity 数据模型，禁止业务逻辑。
- `common`：通用组件，包括请求链路（Filter/AOP/MDC）、统一响应、统一异常处理。
- `config`：Bean 与基础组件配置（线程池、HTTP、Jackson 等）。

## 文件与命名规范

- 编码 UTF-8，换行 LF，缩进 4 空格。
- `*Controller.java`、`*Service.java`、`*ServiceImpl.java`、`*Client.java`、`*Config.java`、`*Test.java`。
- Domain 命名使用 `*Dto.java` / `*Vo.java` / `*Entity.java`，数据源需体现在 Entity 名称（如 `SearchMysqlEntity`）。
- 变量 `lowerCamelCase`，常量 `UPPER_SNAKE_CASE`，禁止 `obj`/`tmp` 等无语义命名。

## 代码规范

- Java 21；DTO/VO/Entity 优先使用 `record` 与不可变建模。
- Controller Body 参数必须使用 DTO + `@Valid`，避免 `Map`/`Object` 作为对外入参出参。
- 查询可空返回优先 `Optional<T>`；可空参数显式 `@Nullable`。
- 依赖注入优先构造器注入。
- 公开类与公开方法需有注释，复杂分支（回退/重试/并发）补充关键意图注释。

## 异常与响应

- 业务错误抛 `BusinessException`（`NestedRuntimeException` 体系）。
- `GlobalExceptionHandler` 统一处理业务异常与非业务异常。
- 所有接口统一返回 `ApiResponse<T>`，禁止用 `null` 表示失败。

## 日志与链路

- 日志统一使用 `@Slf4j`。
- Filter/AOP 注入并透传 `traceid`、`userid`（MDC）。
- Controller 记录入口参数与耗时（脱敏）；Service 记录关键分支；Client 记录外部调用目标、耗时、失败原因。

## 测试规范

- 集成测试使用 `@SpringBootTest(webEnvironment = RANDOM_PORT)`。
- 至少断言 HTTP 状态、`ApiResponse` 结构、业务字段。
- 覆盖成功、参数非法、业务异常三类核心分支。
- 用例独立可复现，不依赖执行顺序。

## 文档与变更

- 功能迭代先更新 `docs/*-requirements.md`，再更新 `docs/*-design.md`。
- 实现完成后同步更新 `README.md` 与 `RECORDS.md`。
- `RECORDS.md` 仅允许追加，不覆盖历史。
- 文档、代码、测试必须一致；当前示例以 `docs/search-requirements.md` 与 `docs/search-design.md` 为准。
