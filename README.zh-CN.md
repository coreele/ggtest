# GGTEST

[English](README.md) | [中文](README.zh-CN.md)

基于 JDBC 运行 [sqllogictest](https://www.sqlite.org/sqllogictest) 格式用例的 Java CLI。
命令名：`ggtest`。

当前内置 SQLite 与 PostgreSQL；更多引擎通过 `DatabaseExecutor` 扩展。

## 环境

- JDK 17+、Maven 3.8+
- PostgreSQL（可选）：具备 `CREATE` / `DROP SCHEMA … CASCADE` 权限的账号

## 安装

```bash
mvn -q clean package
# 功能展示（英文注释 demo.slt / 中文注释 demo_zh.slt，断言对等）
./bin/ggtest --engine sqlite --url jdbc:sqlite::memory: examples/demo.slt examples/demo_zh.slt
./bin/ggtest --url jdbc:sqlite::memory: path/to/file.slt
# 或：java -jar target/ggtest-*.jar --url jdbc:sqlite::memory: path/to/file.slt
```

```bash
mvn -q clean test   # 未设置 GGTEST_PG_URL 时跳过 PG 套件
```

## 用法

```text
ggtest [--url <jdbc>] [--user <u>] [--password <p>]
       [--engine sqlite|postgres] [--hash-threshold <N>]
       [--env-file <path>] [--color auto|always|never] [--halt] [--override]
       <file-or-dir> ...
```


| 选项                      | 默认          | 说明                     |
| ----------------------- | ----------- | ---------------------- |
| `--url`                 | 环境 / `.env` | 必填（`GGTEST_URL` 或 CLI） |
| `--user` / `--password` | —           | 不会出现在报告中               |
| `--engine`              | `sqlite`    | 须与 URL 协议一致            |
| `--hash-threshold`      | `8`         | 每文件初始阈值                |
| `--env-file`            | `./.env`    | 指定后**替换**当前目录 `.env`   |
| `--color`               | `auto`      | 按 TTY 探测；CI 常用 `never` |
| `--halt`                | 关           | 见首个错误即停（断言失败或硬错误）：文件内后续记录跳过不执行、不报失败；尚未开始的文件不打开、不计入 `TOTAL`。退出码优先级不变。语料 `halt` 记录语义不变（仅中止当前文件后续并 skipped，非错误）。 |
| `--override`            | 关           | golden-update 模式：用实际输出重写源 `.slt` 文件中范围内 mismatch 的 expected 区间（query 结果失配、`statement error` 消息失配）。被 override 的记录显示 `[OVERRIDDEN]` 且不计为失败；范围外 mismatch（label 冲突、执行失败、类型签名错、极性翻转）仍 `FAILED`。每文件至多一次原子写回（temp+rename）；无 in-scope mismatch 的文件不被改写。退出码优先级不变。不改 parser / 比较 / 规范化语义。 |


路径：任意文件（内容须是合法 sqllogictest），或递归收集 `*.test` / `*.slt` 的目录。

### 配置

优先级：**CLI > 进程环境 >** `.env`。


| 用途   | 变量                                                                                   |
| ---- | ------------------------------------------------------------------------------------ |
| 运行时  | `GGTEST_URL`、`GGTEST_USER`、`GGTEST_PASSWORD`、`GGTEST_ENGINE`、`GGTEST_HASH_THRESHOLD` |
| 彩色   | `GGTEST_COLOR`、`-Dggtest.color`（次于 `--color`）                                        |
| 测试门控 | `GGTEST_PG_*`、`GGTEST_CORPUS_DIR`                                                    |


参见 `[.env.example](.env.example)`。

### 引擎


| 引擎         | URL 前缀             | 隔离                                  |
| ---------- | ------------------ | ----------------------------------- |
| `sqlite`   | `jdbc:sqlite:`     | 每文件独立连接                             |
| `postgres` | `jdbc:postgresql:` | 唯一 schema + `DROP SCHEMA … CASCADE` |


引擎与 URL 不匹配 → 退出码 `2`。官方 PG 语料零失败不是硬验收。

### 期望结果

`query` 期望头恰好为 `----` 时，默认是**每值一行**；仅当 query 头声明
`separator <delim>` 时才是**行式**：

| 形态 | 触发 | 示例（`query III`） |
|---|---|---|
| **每值一行**（默认） | 恰 `----`，无 query 头 `separator` | 三行：`1` / `2` / `3` |
| **行式** | query 头 `separator <delim>` + 恰 `----` | 一行：`1 \| 1 \| hello world` |

每值一行时，每个物理行就是一个单元格（TEXT 中的空格整行保留）。不再按空格猜行式：
纯 `----` 下的 `1 2 3` 是单值 `"1 2 3"`。

行式分隔符写在 **query 头**（不要写在 `----` 上）：

```text
query IIT nosort separator |
SELECT 1, 1, 'hello world'
----
1 | 1 | hello world
```

`delim` 须为空白切分产生的单 token（允许多字符，不得含空白）。每行须拆成恰 `C`
个 token（类型串长度），否则比对可读失败（行号、实际 token 数、`C`）。token 两侧
trim；空 token → `(empty)`。行尾单独的 `separator`（无 delim）仍是 **label**。
已移除的 `---- separator …` 期望头会解析错误——请改用上方 query 头写法。单元格含当前
delim 时换分隔符或改每值一行（无引号层）。

哈希期望（`N values hashing to <md5>`）优先识别，口径不变。

### 语句断言

`statement ok` 断言 SQL 执行成功；`statement error` 断言执行失败。`statement error`
后可选地跟一段预期错误消息——即 `error` 关键字之后的剩余文本（原样保留空白；`#` 视为
字面字符，不按注释剥离）：

```text
statement error no such table
SELECT * FROM missing_table
```

给出消息时，语句必须失败**且**返回的错误摘要（`errorSummary`）**包含**该预期消息，匹配为
**大小写不敏感的子串包含**（纯子串包含——非正则、非精确相等）。消息为空或仅空格视为无消息。

| `statement error` 形态 | 结果 |
|---|---|
| 无消息 | 仅验证执行失败（向后兼容，行为不变） |
| `<message>`，但执行成功 | 失败：`statement expected to fail but succeeded` |
| `<message>`，执行失败但错误摘要不含 `<message>` | 失败：`statement error message mismatch`（diff 风格展示预期 vs 实际） |
| `<message>`，执行失败且错误摘要含 `<message>` | 通过 |

### 退出码


| 码   | 含义                       |
| --- | ------------------------ |
| `0` | 全部可断言记录通过                |
| `1` | 存在断言失败                   |
| `2` | 用法 / 配置 / 解析 / 连接 / 致命错误 |


硬错误文件计入 `TOTAL.failed`，退出码仍为 `2`。

### 报告

`TOTAL` 按**文件**计数（不是 query 数）。路径相对当前工作目录。

```text
examples/demo.slt                                            .. [PASSED] in 5 ms
examples/demo_zh.slt                                         .. [PASSED] in 6 ms

TOTAL: passed=2 failed=0 skipped=0
```

```text
examples/demo.slt                                            .. [FAILED] in 18 ms
    at examples/demo.slt:22 : query result mismatch
        (-expected|+actual)
        apple
    -   bananad
    +   banana
        cherry

Error: some test case failed:
[
    "examples/demo.slt",
]

TOTAL: passed=0 failed=1 skipped=0
```

```text
examples/multi.slt                                           .. [FAILED] in 40 ms
    at examples/multi.slt:480 : query execution failed: ... integer overflow ...
    at examples/multi.slt:484 : query execution failed: ... integer overflow ...
    at examples/multi.slt:491 : query result mismatch
        (-expected|+actual)
            ...
    -   ...
    +   ...

Error: some test case failed:
[
    "examples/multi.slt",
]

TOTAL: passed=0 failed=1 skipped=0
```



## 库 API

与 CLI 使用同一套包：

```java
var records = new SqlLogicTestParser().parse(Path.of("sample.test"));

var compared = ResultComparer.compare(
        List.of(ColumnType.INTEGER, ColumnType.TEXT),
        SortMode.ROWSORT,
        ResultComparer.DEFAULT_HASH_THRESHOLD,
        "1\n(empty)\n2\nx\n",
        List.of(List.of("2", "x"), List.of("1", "")));

try (Connection c = DriverManager.getConnection("jdbc:sqlite::memory:")) {
    FileRunResult run = new SqlLogicTestRunner(new SqliteJdbcExecutor(c)).run(records);
}
```

扩展数据库：实现 `com.ggtest.db.DatabaseExecutor`（参考 `PostgresJdbcExecutor`）。
