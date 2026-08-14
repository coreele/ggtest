# Spec: add-statement-query-timeout

## P0 — 行为合同

### P0-1: timeout 属性语法

`statement` 和 `query` header 均支持 `timeout=<ms>` 作为 key=value 属性：

```
statement ok timeout=2000
query I nosort timeout=500
query IIT separator=| timeout=1000
statement error timeout=3000
```

- `timeout` key 区分大小写
- value 为正整数毫秒数
- value 为 0 或负数 → 解析错误
- value 非整数 → 解析错误
- 同一 header 内 `timeout` 只能出现一次（重复 → 解析错误）

### P0-2: timeout 语义

- `timeout` 为 0 或未指定 → 无超时限制（默认行为）
- `timeout > 0` → 数据库执行超过该毫秒数后，JDBC 驱动应中断执行
- 超时导致的失败 → `RecordOutcome.FAILED`，原因包含 "timed out" 字样
- 超时不视为 fatal（不中止文件）

### P0-3: JDBC 实现

- 通过 `Statement.setQueryTimeout(int seconds)` 设置，ms → seconds 向上取整（`(timeoutMs + 999) / 1000`）
- SQLite JDBC 不支持 `setQueryTimeout`，仍接受语法但超时可能无效（文档说明）

## P1 — 边界

- `statement error timeout=1000` → 正确，超时阻止正常执行
- `statement ok timeout=text` → 解析错误
- `query I timeout=0` → 解析错误（必须 > 0）
