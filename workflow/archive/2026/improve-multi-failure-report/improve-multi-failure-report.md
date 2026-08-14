# 工作项: improve-multi-failure-report

描述: 提升单文件多条失败记录时 CLI 报告可读性。当前 `FileRunner` 将多条 `[WHY]/[SQL]/[Diff]/at` 细节行直接拼接，仅在整文件块后空一行，导致连续失败块难扫读。本项只改**呈现/UX**，不修复 `slt_lang_aggfunc` 溢出/精度等产品语义（见 `fix-aggfunc-sum-overflow` / `fix-aggfunc-total-precision`）。
目标分支: main
源分支: improve-multi-failure-report
基线提交: a6c8719bc48099cf772a6bd1807876dd4577259c
文档影响: `workflow/archive/2026/improve-multi-failure-report/`（spec/plan 等）；可能增量修订归档 [`ggtest-cli-report`](../ggtest-cli-report/spec.md) 失败块呈现合同；必要时 README CLI 报告小节

> **本文件须保存为 `workflow/archive/2026/improve-multi-failure-report/improve-multi-failure-report.md`**，文件名与目录同名。
> 流程定义见 `workflow/WORKFLOW.md`；看板见 `workflow/STATUS.md`。
> 本工作项的全部产物平铺在 `workflow/archive/2026/improve-multi-failure-report/`，无子目录、无版本后缀。
> 表内只填枚举、短标签或路径；理由与长说明写进「进度笔记」。

## 门禁

| 路径等级 | Spec | Spec 用户确认 | Design | Review |
|---|---|---|---|---|
| standard | skipped | not-required | skipped | required |

## 状态

| 状态 | 下一步 | 阻塞原因 | 恢复条件 | 恢复后目标 |
|---|---|---|---|---|
| archived | — |  |  |  |

## 子项（仅 tracking 项填写）

| 子项 id | 状态 |
|---|---|
| — | |

## 进度笔记

- 2026-08-06：登记 → Analyst 初稿 Spec（复杂推荐）→ `awaiting-spec-approval`。
- 2026-08-06：用户否决复杂方案并给出精确样例。Analyst 重写 Spec（refine-docs）。Spec **approved**；状态 → **`planning`**。
- 2026-08-06：Planner 产出 [plan.md](plan.md)（T0–T4；L3；开放问题 none）。状态 → **`awaiting-plan-approval`**。
- 2026-08-06：用户确认 Plan（「ok」）。Plan **approved**；状态 → **`planned`** → **`developing`**。调度 **Developer**：自 `main` 创建/检出 `improve-multi-failure-report`，按 Plan T0–T4 TDD 实施；写 `dev-notes.md`；更新 README 报告样例；**不要** commit/push/merge；禁止改 `workflow/docs/manager/*`、WI-2/WI-3、`pom.xml` 无关改动、入库 `sqllogictest/`。
- 2026-08-06：Developer 交接。T0–T4 **done**（分支 `improve-multi-failure-report`）。改动：`ReportWriter.java`、`FileRunner.java`、相关测 + `multi-fail.test`、README 中英、`dev-notes.md`。验证：定点 Failures=0；`mvn -q clean test` **233/0/0/18**；package SUCCESS。未 commit。§6：多段纯硬错误块间空行无独立 fixture（共享路径已覆盖）。状态 → **`reviewing`**。调度 **Reviewer**。
- 2026-08-06：Reviewer **Request changes**（阻塞 **R1**：工作区 `pom.xml` 含无关 `maven-compiler-plugin`，Plan 禁止纳入本项）。报告 `review.md`（未提交）。状态 `reviewing` → **`developing`**。调度 **Developer**：在源分支将 `pom.xml` 还原为与 `main`/`HEAD` 干净版本一致（`git checkout -- pom.xml` 或等价）；更新 `dev-notes.md` 说明 R1；勿 commit；然后复审。
- 2026-08-06：Developer 修 R1：`pom.xml` 已还原，工作区 diff 不再含该文件；定点测 exit 0；notes 已记。状态 → **`reviewing`**。调度 **Reviewer** 复审。
- 2026-08-06：Reviewer 复审 **Approve**（R1 closed；无阻塞项）。报告 `review.md`（未提交）。状态 → **`qa`**。调度 **QA**；Pass 后停 merge-auth（报告保持未提交）。
- 2026-08-06：QA **Pass**（无缺陷）。独立复跑：定点 **35/0/0/1**；全量等价 **233/0/0/18**；package SUCCESS；`pom.xml` ≡ main。字面 `mvn clean test` 因 main 基线 compiler 3.1 需 `-Dmaven.compiler.source/target=17`（§6，非本项）。报告 `qa-report.md`（未提交）。状态维持 **`qa`**。**门禁到达 merge-auth**；授权后置 `done` 并一次提交再合入。WI-2/WI-3 未推进；未 commit/push/merge。
- 2026-08-06：用户「允许合入」= **明确合并授权**。状态 → **`done`**。源分支一次关闭提交（含实现/测试/README/feature 文档含未入库 review+qa-report、STATUS/工作项记录、`multi-fail.test`；**排除** `pom.xml`、`sqllogictest/`、`.env`）。调度 **QA（Merge Executor）**：本地将 `improve-multi-failure-report` 合入 `main`（**不 push**）。

## 合并授权

- 2026-08-06：用户原话「ok 允许合入」= **明确合并授权**（源 `improve-multi-failure-report` → 目标 `main`；不 push）。
- 2026-08-14：按 ggnote `WORKFLOW.md` 标准迁移工作流目录（记录与产物合并为同一目录；权威文件改为 `workflow/WORKFLOW.md`）。
