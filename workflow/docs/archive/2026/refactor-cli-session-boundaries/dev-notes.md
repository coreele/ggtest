# dev-notes: refactor-cli-session-boundaries

> Developer 验证回执。工作项 `refactor-cli-session-boundaries`（未拆分）。

## T0 — 依赖 merge

| 依赖 | 方式 | 提交 |
|---|---|---|
| `fix-cli-credential-redaction` | fast-forward merge | `1ea25fc` |
| `fix-pg-teardown-once` | merge commit | `393f712` → `9e01654` |

merge 后 `mvn -q clean test`：**BUILD SUCCESS**（Failures/Errors = 0）。

## 实现摘要

| 组件 | 职责 | 行数 |
|---|---|---|
| `CliSession` | 多文件循环、计时、汇总、退出码 | 101 |
| `FileRunner` | 单文件 parse/JDBC/PG schema/runner → `FileOutcome` | 165 |
| `ReportWriter` | 状态行、失败块格式化、Error/TOTAL 输出 | 179 |
| `FileOutcome` / `FileBucket` | package-private DTO | 独立文件 |

- `runWithExecutor(DatabaseExecutor, …)` 保留于 `FileRunner`；`SqliteJdbcExecutor` / `PostgresJdbcExecutor` 仅在此调用链构造。
- PG teardown：**finally-only** 单次 `PostgresSchemaIsolation.teardown`（自 `fix-pg-teardown-once` 基线原样迁入）。
- 脱敏：`FileRunner#sanitize` 委托 `CredentialRedaction.redactMessage`；stderr 经 sanitize。
- `CredentialRedactionTest` 改为断言 `FileRunner.sanitize`（sanitize 自 `CliSession` 迁出）。

## 变更路径

| 路径 | 变更 |
|---|---|
| `src/main/java/com/ggtest/cli/CliSession.java` | 瘦身编排 |
| `src/main/java/com/ggtest/cli/FileRunner.java` | **新建** |
| `src/main/java/com/ggtest/cli/ReportWriter.java` | **新建** |
| `src/main/java/com/ggtest/cli/FileOutcome.java` | **新建** |
| `src/test/java/com/ggtest/cli/ReportWriterTest.java` | **新建** |
| `src/test/java/com/ggtest/cli/FileRunnerTest.java` | **新建** |
| `src/test/java/com/ggtest/cli/CredentialRedactionTest.java` | sanitize 入口改为 `FileRunner` |
| `workflow/docs/standards/code-audit-register.md` | CA-003 → `resolved` |

**未触碰**：`com.ggtest.runner/**`、`com.ggtest.db/**`、`parser` / `normalize`。

## 验证（L2）

| 命令 | 结果 |
|---|---|
| `mvn -q clean test` | **BUILD SUCCESS** |

新增单测：

- `ReportWriterTest` — `[WHY]`/`[SQL]`/`[Diff]`/`at path:line`、result mismatch 分支、skip timing、ANSI diff。
- `FileRunnerTest` — parse 硬错误、sqlite 断言失败；PG 路径 `@EnabledIf` 等价（无 `GGTEST_PG_URL` 时 skip）。

回归：`CliReportAcceptanceTest`、`MainOrchestrationTest`、`CredentialRedactionTest`、`PostgresCliIntegrationTest`（门控）均通过，预期未改。

## 登记册

- **CA-003** → `resolved`（`workflow/docs/standards/code-audit-register.md`）。

## 未验证 / 风险

| 项 | 说明 |
|---|---|
| PG teardown 失败 → hardError | 无 `GGTEST_PG_*` 时 `FileRunnerTest.postgresTeardownFailureIsHardErrorWhenPgConfigured` skip；`PostgresCliIntegrationTest` 门控测覆盖正常 PG 路径 |

## 建议后续

- **Reviewer**：P0-A…E + 文档影响 → `review.md` **Approve**（required）。
