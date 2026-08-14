# Spec: rust-rewrite

> **feature-id**：`rust-rewrite` · 路径等级 `full`（Spec 门禁 required + 用户确认 required）

## 背景与目标

ggtest 当前是 Java 17 + Maven + JDBC 项目（55 个生产文件、~5,000 行）。本工作项使用 **Rust** 从零重写，达成功能等价：

- **单二进制部署**：无需 JVM / Maven，`cargo build --release` 产出单一可执行文件。
- **ODBC 统一 DB 层**：用 `odbc` crate 对接 SQLite / PostgreSQL / MySQL，替代 Java JDBC，一套 executor 通吃。
- **取消 XuguDB**：不移植专有驱动与相关 profile。
- **功能等价**：sqllogictest 解析、I/T/R 归一化、结果比对、`--override` golden-update、`--separator`、`--halt`、`--parallel`、`--trace`、报告输出、退出码 —— 全部对齐现有行为。
- **测试等价**：现有测试 fixtures（`.slt` / `.test`）在 Rust 版下产出相同结论。

## 非目标

- 不做语言层面的「Java → Rust 翻译器」（手写 Rust 惯用代码）。
- 不保留 Java 项目（Rust 版完成后归档 Java 版，或作为 reference）。
- 不支持 XuguDB。
- 不引入 async runtime（ODBC 为同步阻塞 API，全同步最简）。
- 不引入 ORM / query builder。
- 不改变 sqllogictest 格式语义（I/T/R 规则、hash、separator 等不变）。

## 范围与可见行为

### 1. CLI 对等

Rust 版 `ggtest` 须支持与 Java 版完全相同的命令行接口：

```
ggtest [--url <jdbc-equivalent>] [--user <u>] [--password <p>]
       [--engine sqlite|postgres|mysql] [--hash-threshold <N>]
       [--env-file <path>] [--color auto|always|never]
       [--halt] [--parallel <N>] [--override] [--separator <s>]
       [--trace] [--help] <file-or-dir> ...
```

- 退出码：`0` = 全通过 · `1` = 断言失败 · `2` = 用法/配置/致命错误。
- `--url`：使用 ODBC 连接串或引擎原生 URL（待 Design 决定格式）。
- `--override` + `--separator` 行为与 Java 版一致（一律覆盖、类型签名推断、执行失败转 `statement error`、separator 覆盖 header、单列省略）。
- `--env-file` / 环境变量合并优先级：CLI > env > `.env`。
- `--parallel`：文件级并发，与 `--halt` 的确定性交互不变。
- 报告格式（`[PASSED]` / `[FAILED]` / `[OVERRIDDEN]` / `[SKIPPED]` + `TOTAL:` 行）与 Java 版逐字节对齐（或文档说明差异）。

### 2. 解析与归一化对等

- sqllogictest 格式解析（statement / query / skipif / onlyif / hash-threshold / halt / `#` 注释 / `----` 期望块 / `separator=` / `timeout=` / `conn=`）。
- I/T/R 归一化规则（`ValueNormalizer` 等价实现）。
- 结果比对（`ResultComparer` 等价：sort / hash / value-per-line / row-wise）。
- 类型推断（`TypeSignatureInferer` 等价：值驱动 I/R/T 推断）。
- `--override` 时的签名对齐 + 错误转 statement error。

### 3. 数据库执行

- ODBC 统一 executor（`trait DatabaseExecutor` + `OdbcExecutor` impl）。
- 引擎：SQLite（默认）、PostgreSQL、MySQL。
- Schema 隔离：PG / MySQL 按文件唯一 schema（`prepare → run → teardown`），`SchemaNames` 等价。
- 连接管理：单连接串行（默认）/ 多 `conn=<name>` 连接 / `--parallel` 文件级独立连接。

### 4. 工具脚本

- `sltsql` / `sqlslt` / `sltmd` / `mdslt`：保留 Python 脚本或 Rust 重写（后者更一致，但非阻塞）。

### 5. 测试

- 现有 fixtures（`.slt` / `.test`）直接复用，在 Rust 版下产出相同结论。
- Rust 原生测试（`#[test]`）覆盖 parser / normalize / runner / override / CLI。
- 架构守护测试等价（`RunnerDependencyIsolationTest` → Rust 模块可见性 + `cfg(test)` 检查）。

## 合同

- **CLI 契约**：命令行接口与 Java 版逐选项等价；退出码相同。
- **输出契约**：报告 stdout 格式对齐（允许 ANSI 色码实现差异）；密码不输出。
- **.slt 契约**：读写的 `.slt` 文件格式与 Java 版兼容（EOL 保留、`----` 区间替换语义不变）。
- **归一化契约**：I/T/R 归一化输出逐字节等价（含 `%.3f`、NULL → `NULL`、空串 → `(empty)`、非法 → `0` / `0.000`）。
- **ODBC 契约**：运行时需安装 unixODBC + 对应引擎 ODBC 驱动（文档说明）。

## 验收

P0（必须可验证）：
- P0-1 SQLite 端到端：`examples/demo.slt` 在 Rust 版下 `exit=0`，报告含 `[PASSED]`。
- P0-2 归一化等价：现有 normalize 测试 fixtures 在 Rust 版下逐值一致。
- P0-3 `--override`：类型签名对齐 + 执行失败转 err + separator（多列行式/单列省略），与 Java 版行为等价。
- P0-4 `--halt` + `--parallel`：确定性交互（排队文件跳过、输入序输出）。
- P0-5 退出码：0/1/2 映射正确。
- P0-6 `cargo build --release` 产出单二进制；`cargo test` 全绿。

P1（重要，非阻塞）：
- P1-1 PostgreSQL 端到端（schema 隔离 + teardown）。
- P1-2 MySQL 端到端。
- P1-3 官方 sqllogictest 语料 SQLite 零失败。
- P1-4 bin/ 脚本 Rust 重写。

## 技术选型（Design 细化，此处给出方向）

| 领域 | Java 现状 | Rust 方向 |
|---|---|---|
| 语言 | Java 17 | Rust (edition 2021/2024) |
| 构建 | Maven | Cargo |
| DB 统一 | JDBC | ODBC (`odbc` crate) |
| 测试 | JUnit 5 | `#[test]` + `assert_eq!` |
| CLI 解析 | 手写 `CliArgumentParser` | `clap` (derive) |
| 并发 | `ExecutorCompletionService` | `std::thread` + channel 或 `rayon` |
| 文件原子写 | `Files.move(ATOMIC_MOVE)` | `std::fs::rename`（同文件系统原子） |
| 正则 | `Pattern` / `Matcher` | `regex` crate |
| 打包 | shade uber-JAR | 单二进制（无需打包） |

## 工作量预估（AI 辅助）

| 阶段 | 内容 | 预估 |
|---|---|---|
| P0 骨架 | Cargo 项目 + 模块 + CI | 0.5 天 |
| P1 model + parser | 类型 + 解析 | 1 天 |
| P2 normalize | I/T/R + hash + sort + compare + 推断 | 1 天 |
| P3 db（ODBC executor） | SQLite + PG + MySQL + schema 隔离 | 1 天 |
| P4 cli | clap + 配置 + FileRunner + 报告 | 1.5 天 |
| P5 override | OverrideWriter + 签名推断 + 转换 + separator | 1 天 |
| P6 并发 + halt | `--parallel` + `--halt` | 1 天 |
| P7 测试迁移 | fixtures + 原生测试 | 2 天 |
| P8 脚本 + 打磨 | bin/ + README + 边界 | 1 天 |
| **合计** | | **~10 天** |

## 开放问题

1. ODBC 连接串格式：是否保持 `jdbc:` URL 并内部转换，还是改为 ODBC DSN / 连接串？（Design 决定）
2. Rust 项目放在同仓库新目录（`rust/`）还是独立仓库？（用户决定）
3. Java 版何时归档？（用户决定）
