# 工作项: architecture-overview

描述: 项目介绍 + 既有系统架构设计文档与架构图（文档交付，无业务代码变更）
目标分支: main
源分支: architecture-overview
基线提交: a6c8719bc48099cf772a6bd1807876dd4577259c
文档影响: 原计划新增 design.md；**已取消**，草稿已删除，无合入

> **本文件须保存为 `workflow/archive/2026/architecture-overview/architecture-overview.md`**，文件名与目录同名。
> 流程定义见 `workflow/WORKFLOW.md`；看板见 `workflow/STATUS.md`。
> 本工作项的全部产物平铺在 `workflow/archive/2026/architecture-overview/`，无子目录、无版本后缀。
> 表内只填枚举、短标签或路径；理由与长说明写进「进度笔记」。

## 门禁

| 路径等级 | Spec | Spec 用户确认 | Design | Review |
|---|---|---|---|---|
| standard | skipped | not-required | skipped | required |

## 状态

| 状态 | 下一步 | 阻塞原因 | 恢复条件 | 恢复后目标 |
|---|---|---|---|---|
| cancelled | — |  |  |  |

## 子项（仅 tracking 项填写）

| 子项 id | 状态 |
|---|---|
| — | |

## 进度笔记

- 2026-07-25：用户请求「介绍项目并画好项目架构设计文档和架构图」。登记本项；Spec/Plan/Review 跳过理由见上；调度 Planner + `design-architecture` + `refine-docs`。
- 仓库现状：Java 17 / Maven CLI `ggtest`；包 `cli` / `parser` / `model` / `normalize` / `runner` / `db`（sqlite、postgres）；归档 `ggtest-core`，活跃 `ggtest-pg`（done）、`ggtest-cli-report`（awaiting-spec-approval）。
- 2026-07-25：Planner 完成 `design.md`（含项目介绍、四幅 Mermaid、模块边界与执行流）并 refine-docs；Design 门禁通过。待用户确认后关闭（无 Plan/实现/合入）。
- 2026-07-26：用户对 design.md（已删除） 不满意，明确舍弃本 feature、不再继续。Manager 将状态置 `cancelled`；物理删除未入库草稿目录 `workflow/archive/2026/architecture-overview/`；无合入。
- 2026-07-26：用户授权提交并归档；工作流约定更新为「features + manager 工作项记录一并迁入 `workflow/docs/archive/YYYY/<feature-id>/`」。本记录迁至 本记录。
- 2026-08-14：按 ggnote `WORKFLOW.md` 标准迁移工作流目录（记录与产物合并为同一目录；权威文件改为 `workflow/WORKFLOW.md`）。
