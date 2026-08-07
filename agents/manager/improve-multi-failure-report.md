# 工作项记录: improve-multi-failure-report

工作项标识: improve-multi-failure-report
描述: 提升单文件多条失败记录时 CLI 报告可读性。当前 `FileRunner` 将多条 `[WHY]/[SQL]/[Diff]/at` 细节行直接拼接，仅在整文件块后空一行，导致连续失败块难扫读。本项只改**呈现/UX**，不修复 `slt_lang_aggfunc` 溢出/精度等产品语义（见 `fix-aggfunc-sum-overflow` / `fix-aggfunc-total-precision`）。
路径等级: standard
源分支: improve-multi-failure-report
目标分支: main
文档影响: `agents/features/improve-multi-failure-report/`（spec/plan 等）；可能增量修订归档 [`ggtest-cli-report`](../archive/2026/ggtest-cli-report/spec.md) 失败块呈现合同；必要时 README CLI 报告小节

> 权威工作流、门禁与状态说明见 [agents/README.md](../README.md)。
> 活跃状态见 [STATUS.md](STATUS.md)。

## 切片（未拆分时仅一行，sub-feature-id = feature-id）

| sub-feature-id | Spec | Spec 门禁 | Spec 用户确认 | Design 门禁 | Review 门禁 | 状态 | 后续步骤 |
|---|---|---|---|---|---|---|---|
| improve-multi-failure-report | [spec.md](../features/improve-multi-failure-report/spec.md) | required（CLI 失败块呈现属归档 ggtest-cli-report 合同；多失败分隔与 `at` 缩进为公开报告格式变更） | approved（2026-08-06；用户贴出精确期望样例并冻结决策） | skipped（在既有 `cli` ReportWriter/FileRunner/CliSession 内；无模块边界/选型决策） | required（standard） | done | 已授权合入 main（合入以 git 为准；不 push）；归档待用户要求关闭父项 |

阻塞原因:
恢复条件:
恢复后的目标状态:

## Manager 门禁判定（2026-08-06）

- **路径**：`standard` — 报告 UX/合同增量。
- **Spec**：`required`；用户已拍板简化方案。
- **Spec 用户确认**：`approved` — 用户否决 S1/S3/S4/S5，冻结「块间空行 + 无缩进 `at`」。
- **Design**：`skipped`。
- **Review**：`required`。
- **分支**：源 `improve-multi-failure-report` → 目标 `main`。
- **排除**：不改 WI-2/WI-3；不改 TOTAL 文件级计数；不新增 CLI 标志。

## 用户冻结决策（2026-08-06，权威）

1. 禁止 `[i/N]`；禁止 `N failures in file`；禁止折叠 / 行号优先标题 / 新 CLI 标志。
2. 必须：相邻失败块之间空行；`at <file>:<line>` **无前导缩进**；`[WHY]`/`[SQL]`/`[Diff]` 保持现有缩进。
3. 单失败同样无缩进 `at`；空行分隔仅 N≥2 有意义。
4. `TOTAL.failed` 文件计数；退出码不变。

## Spec 确认

- Spec 路径: [agents/features/improve-multi-failure-report/spec.md](../features/improve-multi-failure-report/spec.md)
- 确认结果: **approved**（2026-08-06）
- 确认依据: 用户否决复杂 S1+S2+S3 推荐，贴出精确期望布局并列出冻结决策；Analyst 已按该样例重写 Spec；Manager 将此视为对该精确形态的 Spec 确认。

## 用户授权记录

- 2026-08-06：登记推进可读性项；非实施/merge 授权。
- 2026-08-06：用户贴出简化布局 = **Spec 拍板**；授权进入 Plan；**仍非**实施/merge 授权；勿 commit 除非另说。

## Plan 确认

- Plan 路径: [agents/features/improve-multi-failure-report/plan.md](../features/improve-multi-failure-report/plan.md)
- 确认结果: **approved**（2026-08-06）
- 确认依据: 当前用户会话对 Plan 回复「ok」。

## 用户授权记录（续）

- 2026-08-06：用户对 Plan 回复「ok」= **Plan 确认**；授权 Developer → Reviewer → QA；**仍非** merge 授权；**不要** commit/push/merge，除非用户另说。不启动 WI-2/WI-3。

## 进度笔记

- 2026-08-06：登记 → Analyst 初稿 Spec（复杂推荐）→ `awaiting-spec-approval`。
- 2026-08-06：用户否决复杂方案并给出精确样例。Analyst 重写 Spec（refine-docs）。Spec **approved**；状态 → **`planning`**。
- 2026-08-06：Planner 产出 [plan.md](../features/improve-multi-failure-report/plan.md)（T0–T4；L3；开放问题 none）。状态 → **`awaiting-plan-approval`**。
- 2026-08-06：用户确认 Plan（「ok」）。Plan **approved**；状态 → **`planned`** → **`developing`**。调度 **Developer**：自 `main` 创建/检出 `improve-multi-failure-report`，按 Plan T0–T4 TDD 实施；写 `dev-notes.md`；更新 README 报告样例；**不要** commit/push/merge；禁止改 `agents/manager/*`、WI-2/WI-3、`pom.xml` 无关改动、入库 `sqllogictest/`。
- 2026-08-06：Developer 交接。T0–T4 **done**（分支 `improve-multi-failure-report`）。改动：`ReportWriter.java`、`FileRunner.java`、相关测 + `multi-fail.test`、README 中英、`dev-notes.md`。验证：定点 Failures=0；`mvn -q clean test` **233/0/0/18**；package SUCCESS。未 commit。§6：多段纯硬错误块间空行无独立 fixture（共享路径已覆盖）。状态 → **`reviewing`**。调度 **Reviewer**。
- 2026-08-06：Reviewer **Request changes**（阻塞 **R1**：工作区 `pom.xml` 含无关 `maven-compiler-plugin`，Plan 禁止纳入本项）。报告 `review.md`（未提交）。状态 `reviewing` → **`developing`**。调度 **Developer**：在源分支将 `pom.xml` 还原为与 `main`/`HEAD` 干净版本一致（`git checkout -- pom.xml` 或等价）；更新 `dev-notes.md` 说明 R1；勿 commit；然后复审。
- 2026-08-06：Developer 修 R1：`pom.xml` 已还原，工作区 diff 不再含该文件；定点测 exit 0；notes 已记。状态 → **`reviewing`**。调度 **Reviewer** 复审。
- 2026-08-06：Reviewer 复审 **Approve**（R1 closed；无阻塞项）。报告 `review.md`（未提交）。状态 → **`qa`**。调度 **QA**；Pass 后停 merge-auth（报告保持未提交）。
- 2026-08-06：QA **Pass**（无缺陷）。独立复跑：定点 **35/0/0/1**；全量等价 **233/0/0/18**；package SUCCESS；`pom.xml` ≡ main。字面 `mvn clean test` 因 main 基线 compiler 3.1 需 `-Dmaven.compiler.source/target=17`（§6，非本项）。报告 `qa-report.md`（未提交）。状态维持 **`qa`**。**门禁到达 merge-auth**；授权后置 `done` 并一次提交再合入。WI-2/WI-3 未推进；未 commit/push/merge。
- 2026-08-06：用户「允许合入」= **明确合并授权**。状态 → **`done`**。源分支一次关闭提交（含实现/测试/README/feature 文档含未入库 review+qa-report、STATUS/工作项记录、`multi-fail.test`；**排除** `pom.xml`、`sqllogictest/`、`.env`）。调度 **QA（Merge Executor）**：本地将 `improve-multi-failure-report` 合入 `main`（**不 push**）。

## 合并授权

- 2026-08-06：用户原话「ok 允许合入」= **明确合并授权**（源 `improve-multi-failure-report` → 目标 `main`；不 push）。
