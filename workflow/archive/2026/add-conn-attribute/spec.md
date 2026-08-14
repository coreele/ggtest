# Spec: add-conn-attribute

## P0 — 行为合同

### P0-1: conn 属性语法

statement 和 query header 均支持 `conn=<name>` 属性：

```
statement ok conn=c1
query I nosort conn=c2 timeout=1000
statement error conn=c1
SELECT 1
----
1
```

- `conn` 值非空，不含空白字符
- 无 `conn` 属性的记录使用默认连接（无名）
- 同一 header 内 `conn` 只能出现一次

### P0-2: 连接语义

- 每个唯一的 conn name 对应一个独立的数据库连接（同一个 JDBC URL）
- 不同 conn name 的连接互相独立（独立事务、独立 session 状态）
- 默认连接（无名）也是一个独立连接
- 所有连接共享同一个 `CliOptions`（URL、user、password）

### P0-3: 连接生命周期

- 连接按需创建：首次遇到 `conn=<name>` 时打开连接
- 文件执行结束后，所有连接关闭
- 连接创建失败 → 文件级别 FatalDatabaseException，中止该文件

## P1 — 边界

- `conn=` 空值 → 解析错误
- `conn` 值含空白 → 解析错误
- 重复 `conn` key → 解析错误
