# Dev Notes: add-conn-attribute

## 实施摘要

statement 和 query 支持 `conn=<name>` 属性，多连接并发测试。

### 变更

| 层 | 文件 | 说明 |
|---|---|---|
| Model | `StatementRecord`, `QueryRecord` | 新增 `String conn`（null = 默认） |
| Parser | `SqlLogicTestParser` | statement/query 解析 `conn=<name>`；`parseConnName()` 校验 |
| Runner | `SqlLogicTestRunner` | 新增 `Function<String, DatabaseExecutor>` 构造函数；`executorFor()` 惰性缓存 |
| CLI | `FileRunner` | 连接工厂 lambda；PG schema 按连接管理；finally 关闭所有连接 |
| CLI | `OverrideWriter` | statement error 覆写追加 ` conn=<name>` |

### 连接生命周期

```
parsed records → factory(named conn) → ConnectionFactory.open → [PG: prepare schema] → executor
               → factory(default)    → ConnectionFactory.open → [PG: prepare schema] → executor
runner.run(records, factory)
→ close all connections + teardown PG schemas
```

### 验证

- `mvn test`: 321 tests, 0 failures
- demo2.slt: [PASSED] in 2170ms (含 2000ms timeout)
