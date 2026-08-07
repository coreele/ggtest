# Review: fix-jdbc-executor-dedup

## 审阅范围

| 项 | 内容 |
|---|---|
| 工作项 | `fix-jdbc-executor-dedup`（未拆分；`standard`；Review **required**） |
| 依据 | [`plan.md`](./plan.md)、[`dev-notes.md`](./dev-notes.md)；`workflow/docs/manager/fix-jdbc-executor-dedup.md`；`workflow/docs/standards/{documentation,quality,security,git}.md` |
| 实现版本 | 分支 `fix-jdbc-executor-dedup`；commit **`46fbb56`**（相对 `main`）；目标 `main` |
| 审阅内容 | `AbstractJdbcExecutor` 抽取；`SqliteJdbcExecutor` / `PostgresJdbcExecutor` 迁移；`RunnerDependencyIsolationTest` 调整；Plan 验收 A1–A5；L2 测试有效性；文档与安全 |
| 未纳入 | 不改业务代码/测试；不进 QA；不合并；不 commit 本报告 |

## 结论

**Approve**

无阻塞项。共享 JDBC 逻辑已上提至 `AbstractJdbcExecutor`；两引擎 `ENGINE_NAME`、专属 `FATAL_MESSAGE_MARKERS` 与 fatal 展示文案与 `main` 一致；`DatabaseExecutor` / `CliSession` 未改；Reviewer 独立 `mvn -q clean test` 绿（196 run / 0 fail / 17 skip）。本环境无 `GGTEST_PG_URL`，PG 门控测 skip，与 Plan 预期一致，**不得**默示 PG Pass；QA 须在可达 PG 上补证或记 Blocked。

## 实现正确性

| 验收 | 要求 | 证据 | 结果 |
|---|---|---|---|
| A1 | 共享 execute/query/readRows/fatal/summarize 仅一处 | `AbstractJdbcExecutor` 含全部同构方法；子类各 ~42–44 行，仅常量 + 构造 + `engineName()` | 通过 |
| A2 | `ENGINE_NAME` 不变 | 源码 `"sqlite"` / `"postgres"`；单测 `engineNameIs*` | 通过 |
| A3 | 各引擎 `FATAL_MESSAGE_MARKERS` 仍为专属列表 | 与 `main` diff 对照：SQLite 7 项（含 `database has been closed`、`out of memory` 等）；PG 5 项（含 `this connection has been closed`、`connection has been terminated`） | 通过 |
| A4 | 业务失败 vs `FatalDatabaseException` 分流不变 | `08*` / `isClosed()` / marker 匹配 → fatal；其余 → `StatementResult`/`QueryResult`；单测 closed-connection、business-failure；`RunnerAcceptanceTest` | 通过 |
| A5 | 全量构建绿 | 见「验证证据摘要」 | 通过 |

Plan 技术方案偏差（已文档化）：Plan 拟 `package-private` 抽象类，因子类位于 `sqlite`/`postgres` 子包改为 `public abstract` + `protected` 构造；仍非用户扩展点，可接受。

不变量核对：`requireUsableConnection` → `"<engineDisplayName> connection is not usable"`；`describeFatal` → `"<engineDisplayName> connection failure: …"`；`summarize` 仍仅 message/class 名，不含连接串或凭据。

## 测试有效性

| 要求 | 证据 | 结果 |
|---|---|---|
| SQLite executor 关键路径 | `SqliteJdbcExecutorTest` 10/0（engine、行读取、NULL、业务失败、closed fatal） | 通过 |
| Runner SQLite 路径 | `RunnerAcceptanceTest` 10/0 | 通过 |
| Runner 依赖隔离 | `RunnerDependencyIsolationTest` 3/0；`AbstractJdbcExecutor.java` 已排除 JDBC-free 检查 | 通过 |
| PG executor（门控） | `PostgresJdbcExecutorTest` 8 skip（无 `GGTEST_PG_URL`） | **本环境 skip**；结构可证伪 |
| L2 全量 | `mvn -q clean test` → BUILD SUCCESS；196/0/17 | 通过 |
| 可证伪性 | 既有单测覆盖 fatal/业务分流；改回重复实现或错 marker 会导致 closed/fatal 或 engine 名断言失败 | 通过 |

## 文档影响核对

| Plan 声明 | 实现是否一致 | 备注 |
|---|---|---|
| 开发文档 | 是 | `dev-notes.md` 含变更路径、L2 命令与计数；`AbstractJdbcExecutor` Javadoc 简述职责 |
| 用户文档 | N/A | 与 Plan 一致；无 CLI/Runner 行为变更 |
| 运维文档 | N/A | 与 Plan 一致 |

## 安全影响核对

| 检查项 | 结果 | 备注 |
|---|---|---|
| 敏感信息 | 通过 | 无硬编码凭据；`summarize` 注释与实现仍不含连接串 |
| 认证与授权 | N/A | 无变更 |
| 输入与外部访问 | N/A | 无新外部访问；SQL 执行路径未变 |
| 依赖变更 | 通过 | 无新增/升级依赖 |

无未解决安全问题。

## Git 合规

| 检查 | 结果 |
|---|---|
| 工作分支 | `fix-jdbc-executor-dedup` |
| 提交 `46fbb56` | 6 文件：抽象类 + 两 executor + 隔离测 + `plan.md` + `dev-notes.md`；无 `.env`/凭据/构建产物 |
| 禁止提交项 | 通过 |

## 必修项

| ID | 位置 | 问题 | 状态 |
|---|---|---|---|
| — | — | 无 | — |

## 非阻塞建议

| ID | 严重度 | 位置 | 说明 |
|---|---|---|---|
| N1 | low | `AbstractJdbcExecutor` 可见性 | Plan 写 package-private，实现为 public abstract；`dev-notes` 已说明 Java 包访问限制。合入后可在 Plan 或审计登记中同步措辞。 |
| N2 | low | CA-001 登记 | `workflow/docs/standards/code-audit-register.md` 标 `resolved` 宜在 QA Pass 后由 Manager 执行（与 Plan 一致）。 |

## 验证证据摘要（审阅者独立）

| 命令 | 结果 |
|---|---|
| `mvn -q clean test` | BUILD SUCCESS；**196** run / **0** fail / **0** error / **17** skip |
| `mvn -q test -Dtest=SqliteJdbcExecutorTest,RunnerAcceptanceTest,RunnerDependencyIsolationTest` | exit **0** |

### 未验证缺口

| 未验证项 | 原因 | 风险 | 恢复条件 | 复测范围 |
|---|---|---|---|---|
| `PostgresJdbcExecutorTest`（8 项） | 无 `GGTEST_PG_URL` | PG executor 路径未在本 Review 实跑 | 设置 `GGTEST_PG_*` | T3 单测 + 全量回归 |

## 后续动作

1. Manager：Review 门禁满足 → 可调度 **QA**（本报告 ≠ QA Pass）。
2. QA：按 Plan 验证命令独立执行；有 PG 时必跑 `PostgresJdbcExecutorTest`；对照 A1–A5；无 PG 不得默示 PG Pass。
3. QA Pass 后：CA-001 可标 `resolved`（N2）。
4. 本 `review.md` 不由 Reviewer 提交（待合并授权前留工作区）。
