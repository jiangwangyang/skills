# demo-spring

基于 Spring Boot 的 Web 示例项目，包含统一请求链路、统一响应、统一异常处理、可复用配置和 Demo 业务流程。

## 快速开始

- 环境：JDK 21、Maven 3.9+
- 启动：`mvn spring-boot:run`
- 基础路径：`http://localhost:8080/demo/spring`
- Demo 接口：`POST /demo`
- 调用示例：
  `curl -X POST "http://localhost:8080/demo/spring/demo" -H "Content-Type: application/json" -H "Authorization: Bearer demo-token" -d "{\"name\":\"test\"}"`

## 目录结构

- `src/main/java/com/example/demo/spring/common`：通用能力（请求/响应/异常）
- `src/main/java/com/example/demo/spring/config`：配置类（线程池/HTTP/Jackson）
- `src/main/java/com/example/demo/spring/domain`：DTO/VO/Entity
- `src/main/java/com/example/demo/spring/controller`：接口层
- `src/main/java/com/example/demo/spring/service`：业务服务层
- `src/main/java/com/example/demo/spring/client`：外部访问层（MySQL/HTTP/等），必须进行限流和重试

## 通用模块

### request

- 使用方式：`RequestUtil` 获取上下文
- `TraceFilter` 注入 `traceid`，`UserFilter` 校验 `Authorization` 并注入 `userid`，`LogAspect` 统一记录请求日志
- 要点介绍：过滤器按顺序执行，日志自动携带链路与用户信息，避免业务代码重复埋点

### response

- 使用方式：Controller 统一返回 `ApiResponse<T>`，成功用 `success(...)`，失败用 `fail(...)`
- 要点介绍：统一响应结构，列表数据自动补充分页字段

### exception

- 使用方式：业务异常抛 `BusinessException`，由 `GlobalExceptionHandler` 统一转换响应（含业务异常与系统异常）
- 要点介绍：业务异常与系统异常分层处理，常见 4xx 与参数错误统一兜底

## 配置模块

#### ExecutorConfig

- 使用说明：提供 `virtualExecutor` 和 `threadExecutor` 两类异步执行器
- 要点介绍：异步任务透传并清理 `RequestAttributes/MDC`，避免上下文丢失和污染

#### HttpClientConfig

- 使用说明：统一注入 `RestClient` 与 `WebClient`，复用连接池和超时配置
- 要点介绍：集中管理连接、超时、默认请求头和请求日志，减少重复配置

#### JacksonConfig

- 使用说明：统一注册 `JsonMapper`，定义日期时间格式与时区
- 要点介绍：固定 `LocalDateTime` 序列化规则，降低前后端时间解析偏差

## 业务模块

### Demo 模块

- 代码要点介绍：`DemoController` 接口入口，`DemoServiceImpl` 执行业务编排，`DemoMysqlClient` 数据访问层
- 代码要点介绍：查询策略为直接查询 MySQL，未命中抛业务异常并走统一异常处理
- 代码要点介绍：返回统一封装 `ApiResponse<DemoVo>`，便于前端稳定消费
- ...：可按相同分层模式扩展推荐、详情、聚合查询等模块
