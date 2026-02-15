# 搜索功能详细设计文档

## 1. 文档信息

- 对应需求文档: `docs/search-requirements.md`
- 设计目标: 基于当前分层结构实现可维护的名称搜索能力，并满足“Redis优先、MySQL兜底”的业务要求。

## 2. 总体设计

采用三层结构:

1. Controller 层: 接收请求、参数校验、输出统一响应。
2. Service 层: 编排业务流程与回退策略。
3. Client 层: 抽象数据访问（Redis/MySQL），并进行限流和重试。

## 3. 方案对比与选型

### 方案A: 顺序回退（Redis -> MySQL）

- 描述: 先查 Redis，未命中再查 MySQL。
- 优点: 逻辑简单、实现成本低、和现有代码一致。
- 缺点: Redis 未命中时延迟为两段串行调用。

### 方案B: 并行查询（Redis || MySQL）

- 描述: 并发查询两个数据源，按优先级选择返回值。
- 优点: 在 Redis 未命中场景下有潜在时延收益。
- 缺点: 复杂度高，增加线程与超时控制成本。

### 方案C: MySQL 主查 + Redis 回填

- 描述: 先查 MySQL，再异步回填 Redis。
- 优点: 数据一致性更容易控制。
- 缺点: 无法体现缓存优先策略，读路径压力落在 MySQL。

### 选型结论

选择方案A（顺序回退）。

- 原因1: 满足当前需求“Redis优先”。
- 原因2: 与现有实现一致，改动最小。
- 原因3: 后续可平滑演进为并行查询或异步回填。

## 4. 接口设计

- Method: `POST`
- Path: `/search`
- 请求参数:
    - `name` | `String` | 搜索名称，作为查询关键字。
- 响应参数:
    - `success` | `Boolean` | 请求是否成功。
    - `message` | `String` | 响应说明信息，成功或失败原因。
    - `data` | `Object` | 搜索结果对象，成功时返回。
        - `id` | `Long` | 搜索结果主键标识。
        - `name` | `String` | 搜索结果名称。
        - `description` | `String` | 搜索结果描述信息。

## 5. 数据流转设计

1. 客户端调用 `POST /search` 并提交 `name`。
2. `SearchController` 完成参数校验并调用 `SearchService.search(dto)`。
3. `SearchServiceImpl` 调用 `SearchRedisClient.findByName(name)`。
4. Redis 命中:
    - 转换 `SearchRedisEntity -> SearchVo`。
    - 返回 `ApiResponse.success(SearchVo)`。
5. Redis 未命中:
    - 调用 `SearchMysqlClient.findByName(name)`。
    - 命中时转换 `SearchMysqlEntity -> SearchVo` 并返回成功。
    - 未命中时抛出 `BusinessException("未查询到数据")`。

## 6. 核心时序

1. `SearchController.search()`
2. `SearchController -> SearchServiceImpl.search()`
3. `SearchServiceImpl -> SearchRedisClient.findByName()`
4. `SearchServiceImpl -> SearchMysqlClient.findByName()`（仅 Redis 未命中时）
5. `SearchServiceImpl` 返回 `SearchVo`
6. `SearchController` 返回 `ApiResponse<SearchVo>`

## 7. 可扩展点

1. 将 Client 层替换为真实 Repository/DAO。
2. 增加缓存回填与过期策略。
3. 增加查询埋点（命中率、回退次数、耗时）。
4. 增加分页与多条件检索接口。
