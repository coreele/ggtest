# 工作项: feat-cli-halt

描述: 在 ggtest Java CLI 实现与官方 `sqllogictest --halt` 对齐的选项：见到首个错误即停止继续执行后续记录；报告与退出码行为须与现有 CLI 兼容并在 Spec/Plan 写清。对照 `sqllogictest/src/sqllogictest.c`（`haltOnError` / `while (nErr==0 || !haltOnError)`）。
目标分支: main
源分支: feat-cli-halt
基线提交: a6c8719bc48099cf772a6bd1807876dd4577259c
文档影响: `workflow/archive/2026/feat-cli-halt/`（spec.md、plan.md；按需 design.md；实现后 review.md / qa-report.md / dev-notes.md）

> **本文件须保存为 `workflow/archive/2026/feat-cli-halt/feat-cli-halt.md`**，文件名与目录同名。
> 流程定义见 `workflow/WORKFLOW.md`；看板见 `workflow/STATUS.md`。
> 本工作项的全部产物平铺在 `workflow/archive/2026/feat-cli-halt/`，无子目录、无版本后缀。
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

- 2026-08-06：Manager 登记；状态 `speccing`；调度 Analyst。
- 2026-08-06：Analyst Spec 完成（`workflow/archive/2026/feat-cli-halt/spec.md`）；Spec 用户确认 not-required；Design skipped → 进入 `planning`；调度 Planner。
- 2026-08-07：Planner Plan 完成（`workflow/archive/2026/feat-cli-halt/plan.md`，已 refine-docs 自检；L2 验证层；触碰路径覆盖 cli/runner；与 Spec 无冲突）；进入 `awaiting-plan-approval`，待用户确认 Plan 后方可置 `planned` 并调度 Developer。
- 2026-08-07：用户确认 Plan（已持久化）；状态 `planned`；调度 Developer（先切源分支 `feat-cli-halt`，按 T1→T8 实施）。
- 2026-08-07：Developer 实施完成（feat-cli-halt 分支 4 commits：plan/status → feat 实现 → README → dev-notes；定向 100 通过，打包 BUILD SUCCESS）。Manager 核验：改动范围仅 cli/runner，normalize/parser 零改动；全量 1 个 pre-existing Error（NormalizeAcceptanceTest，Windows CRLF，已证明与本工作项无关，建议独立 chore）。状态 `reviewing`；调度 Reviewer（standard 门禁 required）。
- 2026-08-07：Reviewer 结论 `Approve`（合同 P0-1…P0-6/P1 逐项满足；测试非恒真达 L2，独立复跑 86 通过；NormalizeAcceptanceTest 判定 pre-existing 无关；安全/Git 合规；2 个非阻塞发现项）。standard Review 门禁满足。状态 `qa`；调度 QA 独立验收。
- 2026-08-07：QA 第 1 轮结论 `Pass`（P0-1…P0-6/P1-1/P1-2 逐项独立取证通过；定向 100 通过 + 打包 BUILD SUCCESS；默认关闭不变性/退出码/报告/文档/安全全通过；无缺陷；NormalizeAcceptanceTest 判定 pre-existing 无关）。review.md/qa-report.md 留工作区未提交。**到达合并授权门禁，等待用户授权**——授权后置 `done` 并与 review.md/qa-report.md 一次提交（源分支 feat-cli-halt），再合入 main（默认 rebase+FF）。
- 2026-08-07：用户授权合并。状态 `done`。在源分支 `feat-cli-halt` 与 review.md/qa-report.md 一次提交，rebase main 后 fast-forward 合入 main（不 push）。归档待用户要求。
- 2026-08-14：按 ggnote `WORKFLOW.md` 标准迁移工作流目录（记录与产物合并为同一目录；权威文件改为 `workflow/WORKFLOW.md`）。
