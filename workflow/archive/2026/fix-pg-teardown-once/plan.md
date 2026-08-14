# Plan: fix-pg-teardown-once

> 实施与验证计划。Spec/Design 门禁均 `skipped`；范围见工作项记录。
>
> **适用对象**：Developer、QA、Manager。
> **前置条件**：工作项路径 `fast`；源分支 `fix-pg-teardown-once`；目标 `main`；Java 17 + Maven。
> **阅读顺序**：元信息 → 目标摘要 → 任务拆解 → 依赖 → 触碰路径 → 验证 → 验收 → 冲突协调 → 文档影响 → 交接。
> **预期结果**：`CliSession.runPostgresFile` 每个 PG 文件生命周期对 schema 至多一次 `teardown`；既有测试绿。
> **失败处理**：按「验证」节定位；无法执行时记录原因/风险/恢复条件。
>
> **Plan 确认**：用户已授权 Manager 覆盖确认本 Plan；Manager 持久化确认后可将状态设为 `planned` 并调度 Developer。Planner 不自行改状态。

## 元信息

- 工作项标识: fix-pg-teardown-once（未拆分；sub-feature-id = feature-id）
- 依据 Spec: N/A（Spec 门禁 skipped）
- 依据 Design: N/A（Design 门禁 skipped）
- 路径等级: fast
- Review 门禁: skipped（fast；进入 QA 不要求 Reviewer Approve）
- 最低验证层: **L2**（构建 + 全量单元/集成套件；控制流整理，无新对外合同）
- 验证命令: `mvn -q clean test`
- 源分支 / 目标: `fix-pg-teardown-once` → `main`
- 审计依据: `workflow/audit/2026-07-26-src.md` Tech Debt Low；登记册 **CA-006**

## 适用工程规范

- [文档工程](../../standards/documentation.md)
- [Git 协作](../../standards/git.md)
- [质量与验证](../../standards/quality.md)
- [安全](../../standards/security.md)

## 目标摘要

- 问题：`runPostgresFile` 在 try 内 `teardown` 失败并 `return hardFailure` 时未将 `schema` 置空，`finally` 再 `DROP` 一次 → 可能重复错误日志。
- 方案（与 Manager 决议一致）：**统一仅在 `finally` 执行 teardown**；try 只负责 `prepare` + `runWithExecutor`；teardown 失败仍映射为 `hardFailure`（保留现有「合并 outcome.detailLines / 兜底文案」语义与 `sanitize`）。
- 禁止：扩大到 `refactor-cli-session-boundaries`；改 `PostgresSchemaIsolation` 合同；无关格式化/移动方法。

## 任务拆解

1. **T1 — 统一 teardown 到 finally**  
   - 改 `CliSession#runPostgresFile`：去掉 try 内 `PostgresSchemaIsolation.teardown`；`schema != null` 时仅在 `finally` 调用一次。  
   - 用局部变量保存 `FileOutcome` / teardown `SQLException`；try-finally 之后再决定返回值（teardown 失败 → `hardFailure`；否则沿用现有 hardError / assertion / pass / skip 映射）。  
   - `prepare` 失败路径保持硬错误；此时 `schema` 仍为 null，finally 不 teardown。  
   - **完成条件**：方法内对 `PostgresSchemaIsolation.teardown` 仅一处调用（finally）；任一返回路径不会二次 DROP。

2. **T2 — 回归验证与登记册**  
   - 执行 `mvn -q clean test`；有 `GGTEST_PG_*` 时既有 PG 门控测作回归证据（无门控则 skip，不得失败）。  
   - 写 `dev-notes.md`（命令与结果摘要）。  
   - 将 `workflow/audit/register.md` **CA-006** 标为 `resolved`。  
   - **完成条件**：套件绿；CA-006 已更新。

## 依赖与顺序

```text
T1 → T2
```

无跨工作项代码依赖；与 `fix-cli-credential-redaction` 为同文件并行冲突风险（见下）。

## 触碰路径

| 路径 | 变更 |
|---|---|
| `src/main/java/com/ggtest/cli/CliSession.java` | **仅** `runPostgresFile`（约 187–226 行）控制流 |
| `workflow/archive/2026/fix-pg-teardown-once/dev-notes.md` | Developer 验证回执（新建） |
| `workflow/audit/register.md` | CA-006 → `resolved` |

禁止触碰：`sanitize` / 报告格式 / `CliOptions` / 执行器 / 其它方法（留给脱敏项或边界重构）。

## 与 fix-cli-credential-redaction 的冲突 / 协调

| 项 | 策略 |
|---|---|
| 重叠文件 | 二者均改 `CliSession.java` |
| 本项改动面 | **局部**：只动 `runPostgresFile`；不重排类成员、不改 import（除非必需）、不做全文件格式化 |
| 对方改动面 | 预期在 `sanitize`、相关调用点 / `CliOptions.toString` 与测试 |
| 合并策略 | 可并行；**优先脱敏项先合 `main`，本分支 rebase**；或同批合入时人工解决。冲突时保留双方意图（单次 teardown + 脱敏）互不覆盖 |
| 后置 | `refactor-cli-session-boundaries` 等本项与脱敏落地后再拆 |

## 验证

| 项 | 内容 |
|---|---|
| 最低验证层 | L2：内部控制流整理，对外 CLI 合同不变，全量 `test` 即可 |
| 命令 | `mvn -q clean test` |
| 预期证据 | BUILD SUCCESS；Failures/Errors = 0；无 `GGTEST_PG_*` 时 PG 门控 skip 非 fail |

### 无法执行验证时

| 未验证项 | 原因 | 风险 | 恢复条件 | 复测范围 |
|---|---|---|---|---|
| `mvn -q clean test` | 本机缺 JDK/Maven 或依赖拉取失败 | 双 teardown / 硬错误映射回归未证实 | 恢复工具链或代理后重跑 | 全量 `mvn -q clean test` |
| PG 门控集成 | 无 `GGTEST_PG_*` | 实库 DROP 路径未实跑（控制流仍可由代码审查确认） | 提供可达 PG 后重跑门控测 | 既有 `Postgres*` 门控测试 |

**禁止**静默跳过未记录的验证缺口。

## 验收（fast；无可测 Spec 条目）

- P0-A：同一 PG 文件在 prepare 成功后，无论 runner 结果或 teardown 成败，`PostgresSchemaIsolation.teardown` 对该 schema **至多调用一次**。
- P0-B：teardown 失败仍产生 `hardFailure`（会话层硬错误语义不变）；成功路径 outcome 映射不变。
- P0-C：`mvn -q clean test` 通过。

## 文档影响

| 类别 | 更新路径或 N/A 理由 |
|---|---|
| 开发文档 | `workflow/archive/2026/fix-pg-teardown-once/dev-notes.md`（验证回执）；`workflow/audit/register.md` CA-006 → `resolved` |
| 用户文档 | N/A — 无用户可见行为/用法变更 |
| 运维文档 | N/A — 无部署/排障步骤变更 |

## Review 门禁与进入 QA

- Review 门禁：**skipped**（fast）。
- 进入 QA 条件：T1–T2 完成；`mvn -q clean test` 证据写入 `dev-notes.md`；无需 Reviewer Approve。
- 合入：须用户合并授权（工作项已注明合入前停合并授权）。

## 交接顺序

1. **Developer**：T1→T2；`dev-notes.md`；CA-006 → `resolved`。
2. **Review**：跳过。
3. **QA**：P0-A…C → `qa-report.md`（Pass/Fail/Blocked）。
4. **Manager**：用户合并授权后按 git/quality 流转 `done`。

## 修订记录

| 日期 | 摘要 |
|---|---|
| 2026-07-26 | finally 单次 teardown；与脱敏项协调；L2 + `mvn -q clean test` |
