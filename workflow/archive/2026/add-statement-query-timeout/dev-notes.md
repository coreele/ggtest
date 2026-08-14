# Dev Notes: add-statement-query-timeout

## 实施摘要

statement 和 query header 支持 `timeout=<ms>` 属性。

### 改动

| 层 | 文件 | 变更 |
|---|---|---|
| Model | `StatementRecord`, `QueryRecord` | 新增 `int timeoutMs` 字段（默认 0） |
| Parser | `SqlLogicTestParser` | statement 解析 key=value 属性；query 属性 switch 加 `timeout` case；新增 `parseTimeoutMs()` |
| DB | `DatabaseExecutor` | 新增 `executeStatement(sql, timeoutMs)` / `executeQuery(sql, timeoutMs)` 默认方法 |
| DB | `AbstractJdbcExecutor` | 覆写，`setQueryTimeout((ms+999)/1000)` |
| Runner | `SqlLogicTestRunner` | 调用 executor 时传入 `record.timeoutMs()` |
| CLI | `OverrideWriter` | statement error 覆写保留 ` timeout=<ms>` |

### 语法

```
statement ok timeout=2000
statement error timeout=3000 division by zero
query I nosort timeout=500
query IIT separator=| timeout=1000
```

### 验证

- `mvn test`: 321 tests, 0 failures
