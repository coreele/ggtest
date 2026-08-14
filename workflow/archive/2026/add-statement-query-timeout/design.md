# Design: add-statement-query-timeout

## 背景

query header 已有 separator 属性，扩展属性系统支持 `timeout=<ms>`。statement header 同样支持。

## 方案决策

| 方案 | 说明 | 选择 |
|---|---|---|
| A: 修改 DatabaseExecutor 接口签名 | `executeStatement(sql, timeoutMs)` | **选中** — 最直接，JDBC 原生支持 |
| B: runner 层设置 timeout | runner 直接访问 JDBC Connection | 破坏分层 |
| C: Timeout 作为 Thread 外部中断 | Future + ExecutorService | 过度设计 |

**决策：** A。`DatabaseExecutor` 新增 `executeStatement(sql, timeoutMs)` / `executeQuery(sql, timeoutMs)` 默认方法。`AbstractJdbcExecutor` 覆写，通过 `Statement.setQueryTimeout()` 设置。

## 模块影响

| 模块 | 变更 |
|---|---|
| `model/StatementRecord` | 新增 `int timeoutMs` 字段 |
| `model/QueryRecord` | 新增 `int timeoutMs` 字段 |
| `parser/SqlLogicTestParser` | statement 头解析 key=value；query 头解析 `timeout` key |
| `db/DatabaseExecutor` | 新增 `executeStatement(sql, timeoutMs)` / `executeQuery(sql, timeoutMs)` 默认方法 |
| `db/AbstractJdbcExecutor` | 覆写方法，`setQueryTimeout((timeoutMs+999)/1000)` |
| `runner/SqlLogicTestRunner` | 调用新方法传入 timeout |
| `cli/OverrideWriter` | statement error 覆写须保留 `timeout=` |
