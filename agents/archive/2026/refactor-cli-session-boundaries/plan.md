# Plan: refactor-cli-session-boundaries

> 实施与验证计划。Spec 门禁 skipped（对外 CLI 合同不变）；依据 [design.md](./design.md)。
>
> **适用对象**：Developer、Reviewer、QA、Manager。
> **前置条件**：路径 `standard`；源分支 `refactor-cli-session-boundaries` → `main`；Java 17 + Maven；**T0 完成**（依赖 merge 到源分支）。
> **阅读顺序**：元信息 → 目标摘要 → T0 基线 → 任务拆解（TDD）→ 依赖 → 触碰路径 → 验证 → 验收 → 文档影响 → 交接。
> **预期结果**：`CliSession` 变薄编排；`FileRunner` / `ReportWriter` 职责分离；`DatabaseExecutor` 隔离不变；`mvn -q clean test` 绿；CA-003 resolved。
> **失败处理**：按「验证」节定位；无法执行时记录原因/风险/恢复条件。
>
> **Plan 确认**：用户已授权 Manager 覆盖确认本 Plan 与 Design；Manager 持久化确认后可将状态设为 `planned` 并调度 Developer。Planner 不自行改状态。

## 元信息

- 工作项标识: refactor-cli-session-boundaries（未拆分；sub-feature-id = feature-id）
- 依据 Spec: N/A（Spec 门禁 skipped；对外 CLI 行为以既有验收测与 `architecture-overview` 为准）
- 依据 Design: [agents/features/refactor-cli-session-boundaries/design.md](./design.md)
- 路径等级: standard
- Review 门禁: **required**（进入 QA 前须 Reviewer **Approve**）
- 最低验证层: **L2**（新增 cli 层单测 + 全量 `mvn test` 回归；无新对外合同）
- 验证命令: `mvn -q clean test`
- 源分支 / 目标: `refactor-cli-session-boundaries` → `main`
- 审计依据: `agents/audit/2026-07-26-src.md` Tech Debt Medium **CA-003**

## 适用工程规范

- [文档工程](../../standards/documentation.md)
- [Git 协作](../../standards/git.md)
- [质量与验证](../../standards/quality.md)
- [安全](../../standards/security.md)

## 目标摘要

- **问题（CA-003）**：`CliSession` 混合编排、JDBC/PG 生命周期、runner 调用与报告渲染（约 420 行）。
- **方案（Design A）**：抽出 package-private **`FileRunner`**（单文件执行 + `DatabaseExecutor` 适配）与 **`ReportWriter`**（格式化 + 打印）；`CliSession` 仅多文件循环、计时、汇总与退出码。
- **约束**：保留依赖项行为——`CredentialRedaction` 脱敏、PG **finally-only** 单次 teardown；**不**改 `runner` / `db` 包。
- **基线**：Developer **先**将 `fix-cli-credential-redaction` @ `1ea25fc` 与 `fix-pg-teardown-once` @ `393f712` merge 到 `refactor-cli-session-boundaries`，**勿等**合入 `main`。

## T0 — 本地基线（merge 依赖，再拆分）

**禁止**在未含脱敏 + finally-only teardown 的 `CliSession` 上拆分。合入内容：`CredentialRedaction.java`、实例 `sanitize`、`runPostgresFile` finally-only teardown。

```bash
git checkout refactor-cli-session-boundaries
git merge 1ea25fc   # fix-cli-credential-redaction
git merge 393f712   # fix-pg-teardown-once
# 冲突：保留脱敏 + finally-only teardown + 双方测试意图
mvn -q clean test   # 绿后再 T1
```

**完成条件**：源分支含两依赖；`mvn -q clean test` BUILD SUCCESS。

## 任务拆解（TDD）

### T1 — 红：`ReportWriter` 格式化单测

1. **新建** `src/test/java/com/ggtest/cli/ReportWriterTest.java`：
   - 断言 `formatFailure` / `detailLines` 产出与当前 `CliSession` 行为一致（`[WHY]`/`[SQL]`/`[Diff]`/`at path:line`、diff 前缀 `-`/`+` 着色逻辑可用 strip-ansi 或禁用 ansi 构造）。
   - 覆盖：`result mismatch:` 分支、git-diff 形态、仅 hard-error 文案、skip timing 无关项。
2. 测试引用 **尚未存在** 的 `ReportWriter` → 编译失败或测试类占位后 fail。
3. **完成条件**：T2 前有明确失败/未实现证据；用例列表与 `CliReportAcceptanceTest` P0-2 字段对齐。

### T2 — 绿：抽出 `ReportWriter`

1. **新建** `src/main/java/com/ggtest/cli/ReportWriter.java`（package-private）：
   - 移入：`printStatusLine`、`formatFailure`、`detailLines`、`colorDiffLine`、`relativePath`（若仅报告用）、静态文本 helper（`sqlFirstLine`、`firstLine`、`afterFirstLine`、`looksLikeGitDiff`）。
   - 构造：`ReportWriter(PrintStream out, ReportStyle style)`；格式化方法无副作用。
   - 提供 `List<String> formatFailureDetailLines(String displayPath, RecordResult rr)` 等供 `FileRunner` 调用。
2. **完成条件**：T1 绿；`CliSession` 仍可通过委托编译（临时双写允许，T5 删除旧实现）。

### T3 — 红：`FileRunner` 执行映射单测

1. **新建** `src/test/java/com/ggtest/cli/FileRunnerTest.java`：
   - **parse 硬错误**：非法语料 → `FileOutcome` `hardError=true`，detail 含 `parse error:`。
   - **sqlite 断言失败**：`:memory:` + 既有 fail fixture → `hardError=false`、bucket FAILED、detail 非空（可文件级集成，不必 mock Connection）。
   - **PG teardown 路径（可选门控）**：若有 `GGTEST_PG_*`，断言 teardown 失败仍 `hardFailure`；无 env 则 `@EnabledIf` skip，不得 fail。
2. **完成条件**：`FileRunner` 未实现时测试红/编译失败。

### T4 — 绿：抽出 `FileRunner`

1. **新建** `src/main/java/com/ggtest/cli/FileRunner.java`（package-private）：
   - 移入：`runOneFile`、`runSqliteFile`、`runPostgresFile`、`runWithExecutor`、`openConnection`。
   - 构造：`FileRunner(CliOptions options, PrintStream err, ReportWriter reportWriter)`；`sanitize` 保留实例方法或委托 `CredentialRedaction`（与 merge 后基线一致）。
   - `runWithExecutor(DatabaseExecutor executor, ...)` **签名不变**；PG 路径 **仅 finally** 一次 `PostgresSchemaIsolation.teardown`。
   - 将 `FileOutcome` / `FileBucket` 抽为 package-private 类型（独立文件或同文件）。
2. **完成条件**：T3 绿；`runWithExecutor` 仅通过 `DatabaseExecutor` 与 runner 交互。

### T5 — 绿：瘦身 `CliSession`

1. `CliSession.execute`：预计算 pathWidth/displays → 循环调用 `fileRunner.run(parser, file, display)` → `reportWriter.print*` 输出 → 汇总 `Error:` / `TOTAL:` / 退出码。
2. 删除已搬迁的私有方法与重复类型；`CliSession` 仅保留编排与 wiring（构造 `FileRunner`、`ReportWriter`）。
3. **禁止**：改 `Main` public 签名；改报告字符串合同。
4. **完成条件**：T1–T4 仍绿；`CliSession` 不再含 `openConnection` / `runPostgresFile` / `formatFailure`。

### T6 — 回归、登记册与 dev-notes

1. `mvn -q clean test` 全绿（含 `CliReportAcceptanceTest`、`MainOrchestrationTest`、`CredentialRedactionTest`、PG 门控测）。
2. **新建** `agents/features/refactor-cli-session-boundaries/dev-notes.md`：T0 merge 摘要、验证命令与结果。
3. `agents/standards/code-audit-register.md` **CA-003** → `resolved`。
4. **完成条件**：L2 证据齐全；登记册已更新。

## 依赖与顺序

```text
T0（merge 基线）
  → T1 → T2（ReportWriter）
  → T3 → T4（FileRunner）
  → T5（CliSession）
  → T6（回归 + 文档）
```

T1/T2 与 T3/T4 可在 T2 完成后由不同开发者并行，但 T5 依赖 T2+T4。

## 触碰路径

| 路径 | 变更 |
|---|---|
| `src/main/java/com/ggtest/cli/CliSession.java` | 瘦身编排 |
| `src/main/java/com/ggtest/cli/FileRunner.java` | **新建** |
| `src/main/java/com/ggtest/cli/ReportWriter.java` | **新建** |
| `src/main/java/com/ggtest/cli/FileOutcome.java`（或内嵌） | **新建**（package-private DTO） |
| `src/test/java/com/ggtest/cli/ReportWriterTest.java` | **新建** |
| `src/test/java/com/ggtest/cli/FileRunnerTest.java` | **新建** |
| `agents/features/refactor-cli-session-boundaries/dev-notes.md` | Developer 验证回执 |
| `agents/standards/code-audit-register.md` | CA-003 → `resolved` |

**禁止触碰**：`com.ggtest.runner/**`、`com.ggtest.db/**`、`parser` / `normalize`（除非编译强制 import 调整，应无）。

## 验证

| 项 | 内容 |
|---|---|
| 最低验证层 | **L2**：结构重构 + 对外合同不变 → 新 cli 单测 + 全量单元/集成套件 |
| 命令 | `mvn -q clean test` |
| 预期证据 | BUILD SUCCESS；Failures/Errors = 0；`ReportWriterTest` / `FileRunnerTest` 通过；既有 cli 验收测无 diff 失败 |

### 无法执行验证时

| 未验证项 | 原因 | 风险 | 恢复条件 | 复测范围 |
|---|---|---|---|---|
| `mvn -q clean test` | 缺 JDK/Maven 或依赖拉取失败 | 边界拆分回归未证实 | 恢复工具链 | 全量 test |
| T0 merge | 分支/提交不可达 | 在陈旧 `CliSession` 上拆分导致冲突返工 | 取得 `1ea25fc` / `393f712` 并 merge | T0 + 全量 test |
| PG 门控 | 无 `GGTEST_PG_*` | PG teardown 路径未实跑 | 提供 PG 后重跑 | `PostgresCliIntegrationTest` 等 |

**禁止**静默跳过未记录的验证缺口。

## 验收

- **P0-A（结构）**：存在 `FileRunner`、`ReportWriter`；`CliSession` 不含 JDBC 连接/`SqlLogicTestRunner` 直接调用。
- **P0-B（DatabaseExecutor）**：`FileRunner` 内仅 `runWithExecutor(DatabaseExecutor, ...)` 调用 runner；具体 executor 构造限于此方法调用链。
- **P0-C（行为）**：`CliReportAcceptanceTest`、`MainOrchestrationTest` 无需改预期即可通过（报告与退出码不变）。
- **P0-D（依赖语义）**：脱敏与 PG finally-only teardown 行为与 merge 后基线一致。
- **P0-E**：`mvn -q clean test` 通过。

## 文档影响

| 类别 | 更新路径或 N/A 理由 |
|---|---|
| 开发文档 | `dev-notes.md`（验证回执）；`code-audit-register.md` CA-003 → `resolved`；可选：`architecture-overview/design.md` 中 cli 子组件一句补充（**非必须**，Reviewer 可 N/A） |
| 用户文档 | N/A — 无 CLI 用法/输出合同变更 |
| 运维文档 | N/A — 无部署/排障变更 |

## Review 门禁与进入 QA

- Review 门禁：**required**。
- Reviewer 检查：Design 边界是否落地；测试有效性；文档影响；脱敏/PG 路径未回退。
- 进入 QA 条件：T0–T6 完成；`dev-notes.md` 含 L2 证据；Reviewer **Approve**。
- 合入：须用户合并授权（工作项已注明合入前停合并授权）。

## 交接顺序

1. **Developer**：T0 → T1…T6；`dev-notes.md`；CA-003 → `resolved`。
2. **Reviewer**：P0-A…E + 文档影响 → `review.md` **Approve**（required）。
3. **QA**：独立跑 Plan 验证命令 + P0 条目 → `qa-report.md` Pass/Fail/Blocked。
4. **Manager**：用户合并授权后按 git/quality 流转 `done`。

## 修订记录

| 日期 | 摘要 |
|---|---|
| 2026-07-26 | 初稿：T0 merge 基线；TDD FileRunner/ReportWriter；L2 + Review required |
