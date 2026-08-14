# 工作项: fix-ca017-override-atomic-move

描述: 修复 `OverrideWriter.writeAtomically` 原子移动回退为死代码（CA-017）
目标分支: main
源分支: fix-ca017-override-atomic-move
基线提交: a6c8719bc48099cf772a6bd1807876dd4577259c
文档影响: 

> **本文件须保存为 `workflow/archive/2026/fix-ca017-override-atomic-move/fix-ca017-override-atomic-move.md`**，文件名与目录同名。
> 流程定义见 `workflow/WORKFLOW.md`；看板见 `workflow/STATUS.md`。
> 本工作项的全部产物平铺在 `workflow/archive/2026/fix-ca017-override-atomic-move/`，无子目录、无版本后缀。
> 表内只填枚举、短标签或路径；理由与长说明写进「进度笔记」。

## 门禁

| 路径等级 | Spec | Spec 用户确认 | Design | Review |
|---|---|---|---|---|
| fast | skipped | not-required | skipped | required |

## 状态

| 状态 | 下一步 | 阻塞原因 | 恢复条件 | 恢复后目标 |
|---|---|---|---|---|
| archived | — |  |  |  |

## 子项（仅 tracking 项填写）

| 子项 id | 状态 |
|---|---|
| — | |

## 进度笔记

- 来源：`workflow/audit/2026-08-13-src.md` Findings CA-017。
- 根因与修复方向见 [plan.md](plan.md)。
- 2026-08-13：用户会话确认 Plan（approve）；状态 `awaiting-plan-approval → planned`，源分支 `fix-ca017-override-atomic-move` 已创建并提交 Plan/记录。待调度 Developer。
- 2026-08-13：Developer 实施完成 —— `catch` 改为 `AtomicMoveNotSupportedException` + `FileMover` 注入缝 + 新增回退分支测试；`mvn -Dtest=OverrideWriterTest test`（15/0/0）、`mvn clean test`（360/0/0，34 既有 skip）。状态 `planned → developing`，待调度 Reviewer。
- 2026-08-13：Reviewer Approve（`review.md`，证据提交 `151d0f5`；报告暂未入库，待 `done` 一次提交）。状态 `developing → reviewing → qa`，待 QA 验收。
- 2026-08-13：QA Pass r1（`qa-report.md`，V1–V5 全 Pass，360/0/0）。用户授权合并；状态 `qa → done`，与本轮未入库的 `review.md` / `qa-report.md` 一次提交，随后合入 `main`。
- 2026-08-14：按 ggnote `WORKFLOW.md` 标准迁移工作流目录（记录与产物合并为同一目录；权威文件改为 `workflow/WORKFLOW.md`）。
