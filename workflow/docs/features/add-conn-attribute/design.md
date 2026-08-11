# Design: add-conn-attribute

## 背景

当前 runner 使用单一 `DatabaseExecutor` 执行整个文件。`conn=<name>` 要求不同连接，需要多连接管理。

## 方案决策

| 方案 | 说明 | 选择 |
|---|---|---|
| A: runner 内建连接工厂 | `SqlLogicTestRunner` 接收 `Function<String, DatabaseExecutor>` | **选中** — 最灵活，连接管理在 runner 外部 |
| B: FileRunner 管理连接 map | FileRunner 维护 `Map<String, Connection>`，每种 conn 创建新 executor | 需要修改更多接口，runner 不感知连接创建 |
| C: ConnectionPool 模式 | 新增连接池管理器 | 过度设计 |

**决策：** A。`SqlLogicTestRunner` 新增构造函数接收 `Function<String, DatabaseExecutor>`。无 `conn` 的记录使用 `""`（空串）作为 key 查询工厂。`FileRunner` 在调用 runner 前打开所有需要的连接或惰性创建。

## 模块影响

| 模块 | 变更 |
|---|---|
| `model/StatementRecord` | 新增 `String conn`（null = 默认） |
| `model/QueryRecord` | 新增 `String conn`（null = 默认） |
| `parser/SqlLogicTestParser` | statement/query 解析 `conn=<name>` |
| `runner/SqlLogicTestRunner` | 多连接模式：`Function<String, DatabaseExecutor>` |
| `cli/FileRunner` | 惰性连接创建 + 文件结束后关闭所有连接 |
| `cli/OverrideWriter` | statement error 覆写保留 `conn=<name>` |
