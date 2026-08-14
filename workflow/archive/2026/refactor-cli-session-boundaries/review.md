# Review: refactor-cli-session-boundaries

## 审阅范围

| 项 | 值 |
|---|---|
| 工作项 | `refactor-cli-session-boundaries`（standard，Review required） |
| 切片 | 未拆分；`workflow/archive/2026/refactor-cli-session-boundaries/` |
| 实现版本 | `86d94d0`（refactor）；基线含 T0 merge `9e01654`、docs `3e14256` |
| 审阅依据 | [design.md](./design.md)、[plan.md](./plan.md)、[dev-notes.md](./dev-notes.md)；`workflow/agents/standards/{documentation,quality,security,git}.md` |
| 代码范围 | worktree `/Users/zhougangjie/Space/ggtest/.worktrees/refactor-cli-session-boundaries` |
| 独立验证 | Reviewer 执行 `mvn -q clean test` → **BUILD SUCCESS**（210 tests，0 failures，0 errors，18 skipped） |

## 实现正确性

对照 Design 方案 A 与 Plan P0-A…E：

| 验收项 | 要求 | 证据 | 结果 |
|---|---|---|---|
| P0-A 结构 | `FileRunner` / `ReportWriter` 存在；`CliSession` 仅编排 | `CliSession`（101 行）仅循环、`fileRunner.run`、`reportWriter.print*`、退出码；无 `openConnection` / `runPostgresFile` / `SqlLogicTestRunner` | **通过** |
| P0-B DatabaseExecutor | runner 经 `DatabaseExecutor` 端口；适配器构造限 `FileRunner` | `runWithExecutor(DatabaseExecutor, …)`；`SqliteJdbcExecutor` / `PostgresJdbcExecutor` 仅在 `runSqliteFile` / `runPostgresFile` 链 | **通过** |
| P0-C 行为 | 报告与退出码不变 | `CliReportAcceptanceTest`、`MainOrchestrationTest` 等全量回归绿，预期未改 | **通过** |
| P0-D 依赖语义 | 脱敏 + PG finally-only teardown | `FileRunner#sanitize` → `CredentialRedaction.redactMessage`；stderr 经 sanitize；`runPostgresFile` 仅 `finally` 一次 `PostgresSchemaIsolation.teardown`（grep 单处调用） | **通过** |
| P0-E 验证 | L2 全绿 | 独立 `mvn -q clean test` BUILD SUCCESS | **通过** |

**边界核对：** `CliSession` → `FileRunner` + `ReportWriter` 与 design 一致；`runner` / `db` 无对 cli 新类引用。`ReportWriter` 负责格式化与 stdout；`FileRunner` 仅 stderr + `FileOutcome`。`Main` 签名未变；`86d94d0` 未触碰 `runner` / `db`。

## 测试有效性

| 测试 | 覆盖 | 有效性 |
|---|---|---|
| `ReportWriterTest` | P0-2 字段、result mismatch、git-diff、skip timing、ANSI | 错误实现会改变断言 |
| `FileRunnerTest` | parse 硬错误、sqlite 断言失败 | bucket / hardError / detailLines 映射 |
| `FileRunnerTest.postgresTeardownFailureIsHardErrorWhenPgConfigured` | 有 `GGTEST_PG_URL` 时 PG basic 冒烟 | **命名与 Plan T3「teardown 失败 → hardError」不一致**；无 env 时 skip |
| `CredentialRedactionTest` | URL userinfo、password、plain/null/trim | sanitize 迁至 `FileRunner`，行为保留 |
| 回归 | `CliReportAcceptanceTest`、`MainOrchestrationTest`、`PostgresCliIntegrationTest` | 全绿 |

PG teardown **失败**路径仍依赖代码审查 + `dev-notes` env 缺口记录；不因缺失该单测阻塞 Approve。

## 文档影响核对

| Plan 声明 | 实现是否一致 | 备注 |
|---|---|---|
| 开发文档 | 是 | `dev-notes.md`；CA-003 → `resolved` |
| 用户文档 | N/A | 无 CLI 合同变更 |
| 运维文档 | N/A | 无部署/排障变更 |

## 安全影响核对

| 检查项 | 结果 | 备注 |
|---|---|---|
| 敏感信息 | 通过 | 无硬编码凭据；stderr/detail 经 `FileRunner#sanitize` |
| 认证与授权 | N/A | 无变更 |
| 输入与外部访问 | N/A | JDBC 逻辑自基线迁入 |
| 依赖变更 | N/A | 无 pom 改动 |

## Git 合规

`86d94d0` 限于 `com.ggtest.cli` 与 cli 测试；T0 merge / docs 分离。无敏感 diff；Reviewer 未 commit `review.md`。

## 发现项

### 非阻塞建议

| ID | 位置 | 说明 |
|---|---|---|
| S-1 | `FileRunnerTest.postgresTeardownFailureIsHardErrorWhenPgConfigured` | 名暗示 teardown 失败 → hardError，实为 PG 通过路径；建议后续重命名或补用例 |
| S-2 | `FileRunner#sanitize` package 可见 | 为单测暴露；可接受 |

### 必修项

无。

## 结论

**Approve**

`CliSession` 变薄；`FileRunner` / `ReportWriter` 边界清晰；`DatabaseExecutor` 隔离、脱敏、PG finally-only teardown 未回退；L2 独立通过。

## 后续动作

| 角色 | 动作 |
|---|---|
| Manager | 调度 **QA**（`mvn -q clean test` + P0 → `qa-report.md`） |
| QA | 可选：有 `GGTEST_PG_*` 时确认 PG 集成 |
| Developer | 非必须：S-1 后续小修 |

**复审范围：** 变更触及 `FileRunner.runPostgresFile`、脱敏或报告格式化时须重新 Review。
