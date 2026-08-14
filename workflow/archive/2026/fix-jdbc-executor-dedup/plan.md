# Plan: fix-jdbc-executor-dedup

> 依据：[工作项记录](../../manager/fix-jdbc-executor-dedup.md)、审计 CA-001（§4 JDBC 执行器重复）。
> **对象**：Developer / Reviewer / QA / Manager。**Plan 确认**：用户 2026-07-26 已授权。

## 元信息

- 工作项标识: fix-jdbc-executor-dedup（sub-feature-id 同左，未拆分）
- 依据 Spec: N/A（Spec 门禁 skipped；内部重构，无对外合同变更）
- 依据 Design: N/A（Design 门禁 skipped；抽取边界清晰，见「技术方案」）
- 路径等级: standard
- Review 门禁: **required**（进入 QA 前须 Reviewer `Approve`）
- 最低验证层: **L2**（单元测试 + 构建；两引擎 executor 既有测 + 全量回归）
- 验证命令:
  - `mvn -q clean test`（主命令；无 `GGTEST_PG_URL` 时 PG 测 assume 跳过，不得失败）
  - 可选（有 PG 时补跑）：`GGTEST_PG_URL='jdbc:postgresql://…' [GGTEST_PG_USER=…] [GGTEST_PG_PASSWORD=…] mvn -q test -Dtest=PostgresJdbcExecutorTest`
- 源分支: `fix-jdbc-executor-dedup` → 目标分支 `main`

## 适用工程规范

- [文档工程](../../standards/documentation.md)
- [Git 协作](../../standards/git.md)
- [质量与验证](../../standards/quality.md)
- [安全](../../standards/security.md)

## 目标摘要

- 消除 `SqliteJdbcExecutor` 与 `PostgresJdbcExecutor` 在 `executeStatement` / `executeQuery` / `readRows` / fatal 判定 / `summarize` 等路径上的同构重复（审计 CA-001）。
- 在 `com.ggtest.db` 引入 package-private `AbstractJdbcExecutor`（备选：`JdbcExecutorSupport` 静态委托）；两子类保持 `public final` 与构造签名不变。
- **保留**各引擎 `public static final String ENGINE_NAME`、引擎专属 `FATAL_MESSAGE_MARKERS`、以及 fatal 消息中的引擎展示名（如 `"SQLite connection is not usable"` / `"PostgreSQL connection failure: …"`）。
- **不修改** `DatabaseExecutor` 接口、`CliSession` 装配方式及 runner 可见行为。

## 技术方案

| 项 | 决策 |
|---|---|
| 共享层位置 | `src/main/java/com/ggtest/db/AbstractJdbcExecutor.java`（package-private `abstract class`，实现 `DatabaseExecutor`） |
| 引擎差异注入 | 子类构造调用 `super(connection, FATAL_MESSAGE_MARKERS, engineDisplayName)`；`engineDisplayName` 用于 unusable / fatal 前缀文案 |
| 子类职责 | 仅保留 `ENGINE_NAME` 常量、marker 列表、`engineName()`、公开构造器 |
| 不变量 | SQL state `08*` → fatal；`connection.isClosed()` → fatal；marker 子串匹配（`Locale.ROOT`）；业务 SQL 错误 → 结果对象；`summarize` 不含凭据 |

## 任务拆解

TDD：每步先确认相关测试状态（红/绿），再改生产代码至绿；**不新增对外 API**。

1. **T0 — 基线验证**  
   - 运行 `mvn -q clean test`，记录通过摘要至 `dev-notes.md`。  
   - **完成条件**：SQLite 与全量非 PG 门控测绿；PG 测 skip 或 pass。

2. **T1 — 引入共享抽象**  
   - 新增 `AbstractJdbcExecutor`，搬移两执行器同构方法；构造参数注入 `fatalMessageMarkers` 与 `engineDisplayName`。  
   - **完成条件**：编译通过；子类未接线前 `mvn -q clean test` 仍绿。

3. **T2 — 迁移 SqliteJdbcExecutor（TDD）**  
   - 将 `SqliteJdbcExecutor` 改为 `extends AbstractJdbcExecutor`；删除已上提的重复方法；保留 `ENGINE_NAME`、`FATAL_MESSAGE_MARKERS` 及现有 Javadoc 引擎描述。  
   - 跑 `mvn -q test -Dtest=SqliteJdbcExecutorTest,RunnerAcceptanceTest`。  
   - **完成条件**：上述测试全绿；`engineName()` 仍为 `"sqlite"`；closed-connection / business-failure 行为与 T0 一致。

4. **T3 — 迁移 PostgresJdbcExecutor（TDD）**  
   - 同上迁移 `PostgresJdbcExecutor`；保留 PG 专属 marker（含 `"this connection has been closed"`、`"connection has been terminated"`）。  
   - 跑 `mvn -q test -Dtest=PostgresJdbcExecutorTest`（无 PG 时 skip 即可）。  
   - **完成条件**：有门控时 PG 测绿；无门控时 skip 且全量 `mvn -q clean test` 绿。

5. **T4 — 全量回归与债务登记**  
   - `mvn -q clean test`；确认两执行器文件行数显著减少、共享逻辑仅一处维护。  
   - `dev-notes.md` 记录验证证据；建议 Reviewer 核对 CA-001 是否可标 `resolved`（登记册更新可在 QA Pass 后由 Manager/Developer 执行）。  
   - **完成条件**：L2 验证通过；Plan 触碰路径无遗漏变更。

## 依赖与顺序

```text
T0 → T1 → T2 → T3 → T4
         └─ T2、T3 均依赖 T1，须顺序执行（先 SQLite 再 Postgres，降低并行冲突）
```

## 触碰路径

| 路径 | 变更 |
|---|---|
| `src/main/java/com/ggtest/db/AbstractJdbcExecutor.java` | **新增** |
| `src/main/java/com/ggtest/db/sqlite/SqliteJdbcExecutor.java` | 改：继承共享抽象，删重复 |
| `src/main/java/com/ggtest/db/postgres/PostgresJdbcExecutor.java` | 改：继承共享抽象，删重复 |
| `workflow/archive/2026/fix-jdbc-executor-dedup/dev-notes.md` | **新增**：Developer 验证记录 |
| `workflow/audit/register.md` | 可选：CA-001 → `resolved`（QA Pass 后） |

**不触碰**：`DatabaseExecutor.java`、`CliSession.java`、parser/runner/normalize、用户 README（无行为变更）。

## 验证

| 命令 | 预期证据 |
|---|---|
| `mvn -q clean test` | `BUILD SUCCESS`；失败/错误计数 0；PG 测在无 `GGTEST_PG_URL` 时 reported skipped，非 failed |
| `mvn -q test -Dtest=SqliteJdbcExecutorTest` | 全部 pass（含 fatal / business failure / NULL 行） |
| `mvn -q test -Dtest=PostgresJdbcExecutorTest` | 有 PG 时 pass；无 PG 时 skip |
| `mvn -q test -Dtest=RunnerAcceptanceTest` | pass（SQLite runner 路径未退化） |

**最低验证层 L2 理由**：纯内部 dedup，无新对外合同；既有 executor 单测 + 全量构建已覆盖 fatal 分流、行读取与引擎名；无需新增 L3 CLI 语料验收。

## 验收

| 条目 | 要求 | 证据 |
|---|---|---|
| A1 | 共享 execute/query/readRows/fatal/summarize 逻辑仅一处 | Reviewer 代码审阅 + diff |
| A2 | `SqliteJdbcExecutor.ENGINE_NAME` / `PostgresJdbcExecutor.ENGINE_NAME` 不变 | 单测 `engineNameIs*` |
| A3 | 各引擎 `FATAL_MESSAGE_MARKERS` 仍为引擎专属列表 | 源码对照 + 既有 fatal 测 |
| A4 | 业务失败 vs `FatalDatabaseException` 分流不变 | T2/T3 单测 + RunnerAcceptanceTest |
| A5 | 全量 `mvn -q clean test` 绿 | dev-notes 命令输出摘要 |

## 无法执行验证时的处理

| 场景 | 原因 | 风险 | 恢复条件 |
|---|---|---|---|
| 无 `GGTEST_PG_URL` | 环境未配 PG | T3 PG 专属路径未本地执行 | 设置 `GGTEST_PG_*` 后补跑 `PostgresJdbcExecutorTest`；dev-notes 标注 |
| Maven 依赖下载失败 | 网络/代理 | 无法确认构建 | 配置代理或离线仓库后重跑 T0/T4 |

## 文档影响

| 类别 | 更新路径或 N/A 理由 |
|---|---|
| 开发文档 | `workflow/archive/2026/fix-jdbc-executor-dedup/dev-notes.md`（验证命令与结果）；共享类 Javadoc 简述职责 |
| 用户文档 | N/A（无 CLI/Runner 对外行为或用法变更） |
| 运维文档 | N/A（无部署/配置变更） |

## 交接顺序

1. **Developer**：按 T0→T4 实施，dev-notes 记录 L2 证据 → 提交 Review。
2. **Reviewer**：检查 A1–A5、测试有效性与文档影响 → `Approve`（Review 门禁 required）。
3. **QA**：独立执行 Plan 验证命令，对照验收表 → `qa-report.md` 结论 Pass/Fail。
4. **Manager**：Plan 确认已授权；QA Pass 后更新工作项状态；**合入 main 须用户合并授权**（非本 Plan 范围）。

## 修订记录

| 日期 | 摘要 |
|---|---|
| 2026-07-26 | 初稿；用户已授权 Plan 确认 |
