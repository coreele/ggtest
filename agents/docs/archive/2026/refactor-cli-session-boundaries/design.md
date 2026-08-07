# Design: refactor-cli-session-boundaries

> **文档性质**：内部边界重构；对外 CLI 报告格式、退出码与 `Main.run` 合同不变（Spec 门禁 skipped）。
>
> **feature-id / sub-feature-id**：`refactor-cli-session-boundaries` / `refactor-cli-session-boundaries`（未拆分）
>
> **适用对象**：Planner、Developer、Reviewer。
>
> **前置条件**：依赖项 `fix-cli-credential-redaction`（@ `1ea25fc`）与 `fix-pg-teardown-once`（@ `393f712`）已 QA Pass；Developer 实施前须将二者合入源分支 `refactor-cli-session-boundaries`（勿等 `main`）。
>
> **阅读顺序**：背景 → 方案对比 → 决策 → 模块边界 → 依赖方向 → 类型归属 → 风险 → Plan/Developer 要点。
>
> **预期结果**：读者能说明 `CliSession` / `FileRunner` / `ReportWriter` 各自职责、`DatabaseExecutor` 隔离点，以及 PG schema 与脱敏控制点落在何处。
>
> **失败处理**：若实现与本文冲突，以 Plan 验收条目与既有 `CliReportAcceptanceTest` / `MainOrchestrationTest` 为准；结构争议回退 Planner 修订 Design。

## 背景

审计 **CA-003**（`agents/docs/audit/2026-07-26-src.md`）：`CliSession.java`（约 420 行）同时承担多文件编排、JDBC 连接生命周期、PostgreSQL schema 隔离编排、`SqlLogicTestRunner` 调用、失败块/状态行/汇总报告渲染，以及错误消息脱敏。改报告或换引擎时牵动面大，单测需整条 CLI 编排。

依赖项已落地（worktree 对照）：

| 依赖 | 变更摘要 | 对本 Design 的约束 |
|---|---|---|
| `fix-cli-credential-redaction` | `CredentialRedaction` + `CliSession.sanitize` 实例方法委托 `redactMessage` | 脱敏逻辑保留在 `com.ggtest.cli`；`FileRunner` 经 `sanitize` 或共享 helper 调用，不复制规则 |
| `fix-pg-teardown-once` | `runPostgresFile` 仅在 `finally` 调用一次 `PostgresSchemaIsolation.teardown` | `FileRunner` 必须原样继承 finally-only 控制流，禁止回退双路径 teardown |

既有分层（`architecture-overview`）：`runner` 只依赖 `DatabaseExecutor` 端口；`cli` 为组合根，可组装 parser、runner 与具体 JDBC 适配器。

## 方案对比与决策

| 方案 | 概要 | 优点 | 缺点 |
|---|---|---|---|
| **A — `FileRunner` + `ReportWriter`（cli 包）** | 执行与报告拆成两个 package-private 类；`CliSession` 只编排循环与退出码 | 与审计建议一致；报告可单测；执行路径可 sqlite 内存或 fake 回归；不破坏 runner/db 边界 | 需定义 `FileOutcome` 等 cli 内 DTO；一次中等规模搬移 |
| B — 将 per-file 执行下沉到 `runner` | 新建 `runner.FileRunner`，cli 只打印 | runner 可复用 | runner 须感知 JDBC、`Connection`、PG schema — **违反**「runner 不依赖 java.sql / 具体引擎」 |
| C — 仅抽 `ReportFormatter`，保留巨型 `CliSession` | 只移格式化方法 | 改动最小 | 连接/PG/parse 仍耦于会话类；CA-003 仅部分缓解 |

**决策：方案 A。** 在 `com.ggtest.cli` 内新增 package-private 的 `FileRunner` 与 `ReportWriter`；`CliSession` 变薄为编排 façade。不新增 public API；`Main` 仍 `new CliSession(...).execute(files)`。

## 模块边界

### 职责划分

| 组件 | 职责 | 禁止 |
|---|---|---|
| **`CliSession`** | 多文件循环；相对路径列宽预计算；计时；调用 `FileRunner` / `ReportWriter`；累计 passed/failed/skipped 与 `hardError`；计算退出码 0/1/2 | 直接 parse/JDBC/runner 调用；内联报告格式化 |
| **`FileRunner`** | 单文件：`SqlLogicTestParser.parse`；`openConnection` + try-with-resources；按 engine 分支 SQLite / PG；PG 下 `PostgresSchemaIsolation.prepare` + finally-only `teardown`；构造 `SqliteJdbcExecutor` / `PostgresJdbcExecutor` 并调用 `SqlLogicTestRunner`；将 runner/parse/连接/schema 结果映射为 **`FileOutcome`**（含 `hardError`、预格式化的 `detailLines`）；连接/schema 失败写 **stderr**（经 `sanitize`） | 直接写 stdout 报告行；修改 `DatabaseExecutor` / runner 语义 |
| **`ReportWriter`** | 状态行、失败块行、末尾 `Error:` 列表、`TOTAL:` 行；`formatFailure` / `detailLines` / diff 着色 / `relativePath` 显示辅助；依赖 `ReportStyle` 与 `PrintStream out` | 打开 JDBC；调用 `SqlLogicTestRunner`；感知 PG schema |
| **`CredentialRedaction`**（既有） | URL userinfo 与 password 字面量脱敏 | — |
| **`SqlLogicTestRunner`**（既有） | 经 **`DatabaseExecutor`** 执行记录 | 不迁入 cli 执行路径 |

### `DatabaseExecutor` 隔离

- **端口**：`FileRunner#runWithExecutor(DatabaseExecutor executor, ...)` 参数类型为 `com.ggtest.db.DatabaseExecutor`；仅在此方法体内 `new SqliteJdbcExecutor(connection)` / `new PostgresJdbcExecutor(connection)`。
- **runner 包**：继续只接受 `DatabaseExecutor`；不 import `cli`、`java.sql.Connection` 或具体适配器。
- **测试**：`FakeDatabaseExecutor` 仍仅用于 `runner` 包单测；cli 层对 `runWithExecutor` 的映射可用 sqlite `:memory:` 或保留既有 `Main.run` 验收测作 L2 回归。

### 共享类型（`com.ggtest.cli`，package-private）

| 类型 | 归属 | 说明 |
|---|---|---|
| `FileOutcome` / `FileBucket` | 自 `CliSession` 抽出为独立 package-private 类型（同文件或 `FileOutcome.java`） | `FileRunner` 产出，`CliSession` + `ReportWriter` 消费 |
| `CliSession.STATUS_PATH_COLUMN_WIDTH` | 保留在 `CliSession` 或移至 `ReportWriter` 常量 | 与冻结样本对齐的最小列宽 60 |

`ReportWriter` 提供 **无副作用** 的格式化方法（如 `formatFailureDetailLines(...)` → `List<String>`），供 `FileRunner` 构建 `FileOutcome.detailLines`，避免格式化逻辑重复。

### 依赖方向（重构后）

```text
Main → CliSession → FileRunner → parser, runner, db.*, CredentialRedaction
                 → ReportWriter → ReportStyle, PrintStream
FileRunner → ReportWriter（仅格式化方法，不打印）
```

禁止：`runner` / `db` / `parser` / `normalize` 依赖 `FileRunner` 或 `ReportWriter`。

## 模块影响

| 路径 | 影响 |
|---|---|
| `src/main/java/com/ggtest/cli/CliSession.java` | 大幅瘦身，保留 `execute` 编排 |
| `src/main/java/com/ggtest/cli/FileRunner.java` | **新建** |
| `src/main/java/com/ggtest/cli/ReportWriter.java` | **新建** |
| `src/main/java/com/ggtest/cli/Main.java` | 无签名变更（可选：构造注入不变） |
| `src/main/java/com/ggtest/runner/**` | **不修改** |
| `src/main/java/com/ggtest/db/**` | **不修改** |
| `src/test/java/com/ggtest/cli/*` | 新增 `ReportWriterTest`、`FileRunnerTest`；既有验收测作回归 |

## 风险

| 风险 | 影响 | 缓解 |
|---|---|---|
| 与依赖分支合入冲突 | 重复改 `CliSession` | Plan T0：先 merge 脱敏 + teardown 到源分支再拆分 |
| 报告格式漂移 | 破坏 P0 冻结样本 | TDD 先锁 `ReportWriter` 格式化；全量 `CliReportAcceptanceTest` 回归 |
| PG teardown 回退 | 双 DROP / 日志重复 | `FileRunner#runPostgresFile` 代码审查 + 既有 PG 门控测 |
| 脱敏回归 | 凭据泄露 | 保留 `CredentialRedactionTest`；`FileRunner` 不绕过 `sanitize` |
| 过度抽象 | 新接口泛滥 | 仅两个 package-private 类 + 既有 DTO；不引入 ConnectionFactory 除非 TDD 证明必要 |

## 对 Plan 与 Developer 的要点

### Plan

见 [plan.md](./plan.md)：T0 merge 基线 → TDD（`ReportWriter` → `FileRunner` → `CliSession`）→ L2 `mvn -q clean test`；Review required；CA-003 → `resolved`。

### Developer

- **禁止**：改 runner/db 合同；public 新 API；回退 finally-only PG teardown 或 strip-only 脱敏。
- **必须**：`runWithExecutor(DatabaseExecutor, ...)` 语义不变；stderr 经 `sanitize`；T0 先于拆分。
- **完成定义**：三职责可单测；`CliSession` 仅编排（约 ≤120 行，非 KPI）。

## 修订记录

| 日期 | 摘要 |
|---|---|
| 2026-07-26 | 初稿：`FileRunner` + `ReportWriter`；DatabaseExecutor 隔离；依赖合入基线 |
