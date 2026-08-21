# GGTEST

[English](README.md) | [中文](README.zh-CN.md)

基于 JDBC 运行 [sqllogictest](https://www.sqlite.org/sqllogictest) 格式用例的 Java CLI，支持 SQLite、PostgreSQL、MySQL、XuguDB（Maven, Java 17）。命令名：`ggtest`。

## 环境要求

- JDK 17+、Maven 3.8+
- PostgreSQL / MySQL / XuguDB（可选）：具备 `CREATE SCHEMA` / `DROP SCHEMA` 权限的账号

## 构建与测试

```bash
mvn -q clean test       # 编译 + 单元/集成测试
mvn -q clean package    # 生成 target/ggtest-*.jar（shaded uber-JAR）
```

PostgreSQL / MySQL / XuguDB 测试由环境变量门控（`GGTEST_PG_URL` / `GGTEST_MY_URL` / `GGTEST_XG_URL`）；XuguDB 还需要 `driver/` 下的专有 JDBC 驱动。未设置时跳过，默认套件仍通过。

## 运行（`ggtest`）

```bash
./bin/ggtest --url jdbc:sqlite::memory: examples/demo.slt
./bin/ggtest --url jdbc:postgresql://localhost:5432/db --engine postgres file.test
./bin/ggtest --url jdbc:mysql://localhost:3306 --engine mysql --user root file.test
./bin/ggtest --url jdbc:xugu://localhost:5138/SYSTEM --engine xugu --user SYSDBA file.test
```

或 `java -jar target/ggtest-*.jar --url jdbc:sqlite::memory: file.test`。

### 输出示例

**通过：**

```text
examples/demo.slt                                            .. [PASSED] in 5 ms

TOTAL: passed=1 failed=0 skipped=0
```

**失败**（内联 expected-vs-actual diff）：

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

**`--override` golden 更新**（用实际结果重写期望）：

```text
examples/demo.slt                                            .. [OVERRIDDEN] in 210 ms

TOTAL: passed=0 failed=0 skipped=0 overridden=1
```

## 命令行选项

```text
ggtest [options] <file-or-dir> [<file-or-dir> ...]
```

| 选项 | 默认 | 说明 |
|---|---|---|
| `--url <jdbc-url>` | 环境 `.env` | JDBC 连接 URL |
| `--user <user>` | 无 | 数据库用户 |
| `--password <pwd>` | 无 | 数据库密码；不会写入日志或报告 |
| `--engine <name>` | `sqlite` | `sqlite`、`postgres`、`mysql`、`xugu` — 须与 URL 协议一致 |
| `--override` | 关 | golden-update 模式：用实际结果覆盖 `.slt` 期望（即使已通过） |
| `--separator <s>` | 关 | `--override` 行式输出分隔符，不能为空；须配合 `--override` |
| `--halt` | 关 | 首个失败即停（文件内后续记录跳过） |
| `--parallel <N>` | 关 | 最多 N 个文件并发；不可与 `--override` 同时使用 |
| `--trace` | 关 | 执行时将每条 SQL 打印到 stderr |
| `--color <auto\|always\|never>` | `auto` | ANSI 颜色控制 |
| `--hash-threshold <N>` | `8` | 每个文件的初始 hash 阈值；结果值数超过非负 N 时启用 hash 比对。`0` 会禁用初始 hash，直到文件内 `hash-threshold` 指令重新设置 |
| `--env-file <path>` | `./.env` | 从指定 `.env` 加载配置（替换默认） |
| `--help`、`-h` | — | 打印用法并退出 |

退出码：`0` = 全部通过 · `1` = 断言失败 · `2` = 用法 / 配置 / 致命错误。

详细架构与工程知识见 [AGENTS.md](AGENTS.md)。
