# 工作项记录: feat-cli-halt

工作项标识: feat-cli-halt
描述: 在 ggtest Java CLI 实现与官方 `sqllogictest --halt` 对齐的选项：见到首个错误即停止继续执行后续记录；报告与退出码行为须与现有 CLI 兼容并在 Spec/Plan 写清。对照 `sqllogictest/src/sqllogictest.c`（`haltOnError` / `while (nErr==0 || !haltOnError)`）。
路径等级: standard
源分支: feat-cli-halt
目标分支: main
文档影响: `agents/features/feat-cli-halt/`（spec.md、plan.md；按需 design.md；实现后 review.md / qa-report.md / dev-notes.md）

> 权威工作流、门禁与状态说明见 [agents/README.md](../README.md)。
> 活跃状态见 [STATUS.md](STATUS.md)。
>
> 文档路径：未拆分时 Spec 为 `agents/features/<feature-id>/spec.md`（无子目录）；已拆分时根目录仅总览 Spec，各切片为 `agents/features/<feature-id>/<feature-id>-<sub>/spec.md`。
> 归档后本文件迁至 `agents/archive/YYYY/<feature-id>/manager.md`，相对链接须同步修正；`agents/manager/` 仅保留活跃工作项记录与 `STATUS.md`。

## 切片（未拆分时仅一行，sub-feature-id = feature-id）

| sub-feature-id | Spec | Spec 门禁 | Spec 用户确认 | Design 门禁 | Review 门禁 | 状态 | 后续步骤 |
|---|---|---|---|---|---|---|---|
| feat-cli-halt | [spec.md](../features/feat-cli-halt/spec.md) [plan.md](../features/feat-cli-halt/plan.md) | required（新增公开 CLI 选项与首错停跑合同；含错误定义、报告与退出码） | not-required（官方语义明确；用户已要求对齐实现，无业务歧义） | skipped（沿用现有 CLI/runner 边界；无新模块选型） | required（standard） | done | 已授权合并（rebase+FF 入 main；不 push）；归档待用户要求 |

阻塞原因:
恢复条件:
恢复后的目标状态:

## Manager 门禁判定（2026-08-06）

- **路径**：`standard` — 新增公开 CLI 行为与失败停跑合同，非琐碎文案修复。
- **Spec**：`required`；**Spec 用户确认**：`not-required`（对齐官方 `--halt`，合同由 Spec 固化即可）。
- **Design**：`skipped`。
- **Review**：`required`。
- **分支**：源 `feat-cli-halt` → 目标 `main`（调度 Developer 前已预填）。
- **对照**：官方 `sqllogictest.c` 中 `haltOnError`；注意与语料内 `halt` 记录语义区分。

## 用户授权记录

- 2026-08-06：用户要求完整工作流推进实现 `--halt`；先完成 cancelled 归档 commit；实现后待合并授权，不擅自 merge/push；勿归档 3 个 done 父项。
- 2026-08-07：用户确认 Plan；授权完整流程推进（连续调度至合并授权门禁前）。

## 进度笔记

- 2026-08-06：Manager 登记；状态 `speccing`；调度 Analyst。
- 2026-08-06：Analyst Spec 完成（`agents/features/feat-cli-halt/spec.md`）；Spec 用户确认 not-required；Design skipped → 进入 `planning`；调度 Planner。
- 2026-08-07：Planner Plan 完成（`agents/features/feat-cli-halt/plan.md`，已 refine-docs 自检；L2 验证层；触碰路径覆盖 cli/runner；与 Spec 无冲突）；进入 `awaiting-plan-approval`，待用户确认 Plan 后方可置 `planned` 并调度 Developer。
- 2026-08-07：用户确认 Plan（已持久化）；状态 `planned`；调度 Developer（先切源分支 `feat-cli-halt`，按 T1→T8 实施）。
- 2026-08-07：Developer 实施完成（feat-cli-halt 分支 4 commits：plan/status → feat 实现 → README → dev-notes；定向 100 通过，打包 BUILD SUCCESS）。Manager 核验：改动范围仅 cli/runner，normalize/parser 零改动；全量 1 个 pre-existing Error（NormalizeAcceptanceTest，Windows CRLF，已证明与本工作项无关，建议独立 chore）。状态 `reviewing`；调度 Reviewer（standard 门禁 required）。
- 2026-08-07：Reviewer 结论 `Approve`（合同 P0-1…P0-6/P1 逐项满足；测试非恒真达 L2，独立复跑 86 通过；NormalizeAcceptanceTest 判定 pre-existing 无关；安全/Git 合规；2 个非阻塞发现项）。standard Review 门禁满足。状态 `qa`；调度 QA 独立验收。
- 2026-08-07：QA 第 1 轮结论 `Pass`（P0-1…P0-6/P1-1/P1-2 逐项独立取证通过；定向 100 通过 + 打包 BUILD SUCCESS；默认关闭不变性/退出码/报告/文档/安全全通过；无缺陷；NormalizeAcceptanceTest 判定 pre-existing 无关）。review.md/qa-report.md 留工作区未提交。**到达合并授权门禁，等待用户授权**——授权后置 `done` 并与 review.md/qa-report.md 一次提交（源分支 feat-cli-halt），再合入 main（默认 rebase+FF）。
- 2026-08-07：用户授权合并。状态 `done`。在源分支 `feat-cli-halt` 与 review.md/qa-report.md 一次提交，rebase main 后 fast-forward 合入 main（不 push）。归档待用户要求。
