# Demo 功能详细设计文档

## 1. 文档信息

- 对应需求文档: `docs/demo-requirements.md`
- 设计目标: 基于当前分层结构实现可维护的 Demo 查询能力，直接查询 MySQL 返回结果。

## 2. 总体设计

采用三层结构:

1. Controller 层: 接收请求、参数校验、输出统一响应。
2. Service 层: 编排业务流程。
3. Client 层: 抽象数据访问（MySQL），并进行限流和重试。

## 3. 方案对比与选型

### 方案A: 直接查询 MySQL

- 描述: 直接查询 MySQL 返回结果。
- 优点: 逻辑简单、实现成本低、无需缓存层。
- 缺点: 每次请求都访问数据库。

### 方案B: 增加缓存层

- 描述: 先查缓存，未命中再查 MySQL。
- 优点: 减少数据库访问压力。
- 缺点: 需要维护缓存一致性，复杂度增加。

### 方案C: 异步查询

- 描述: 使用异步方式查询 MySQL。
- 优点: 可处理并发请求。
- 缺点: 对于简单查询场景过于复杂。

### 选型结论

选择方案A（直接查询 MySQL）。

- 原因1: 满足当前需求"直接查询"。
- 原因2: 与需求保持一致，改动最小。
- 原因3: 后续可平滑演进为增加缓存层。

## 4. 接口设计

- Method: `POST`
- Path: `/demo`
- 请求参数:
    - `name` | `String` | 查询名称，作为查询关键字。
- 响应参数:
    - `success` | `Boolean` | 请求是否成功。
    - `message` | `String` | 响应说明信息，成功或失败原因。
    - `data` | `Object` | 查询结果对象，成功时返回。
        - `id` | `Long` | 查询结果主键标识。
        - `name` | `String` | 查询结果名称。
        - `description` | `String` | 查询结果描述信息。

## 5. 数据流转设计

1. 客户端调用 `POST /demo` 并提交 `name`。
2. `DemoController` 完成参数校验并调用 `DemoService.query(dto)`。
3. `DemoServiceImpl` 调用 `DemoMysqlClient.findByName(name)`。
4. MySQL 命中时转换 `DemoMysqlEntity -> DemoVo` 并返回成功。
5. MySQL 未命中时抛出 `BusinessException("未查询到数据")`。

## 6. 核心时序

1. `DemoController.demo()`
2. `DemoController -> DemoServiceImpl.query()`
3. `DemoServiceImpl -> DemoMysqlClient.findByName()`
4. `DemoServiceImpl` 返回 `DemoVo`
5. `DemoController` 返回 `ApiResponse<DemoVo>`

## 7. 可扩展点

1. 将 Client 层替换为真实 Repository/DAO。
2. 增加缓存层提升性能。
3. 增加查询埋点（耗时、成功率）。
4. 增加分页与多条件检索接口。
