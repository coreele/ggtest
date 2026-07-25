# Manager Status

> 工作流、角色、门禁和状态规则以 [`docs/README.md`](../README.md) 为唯一权威说明。本文件仅维护状态图例、活跃工作项和归档索引，且仅由 Manager 修改。

## 状态图例

主状态序列：

`backlog` → `speccing` → `awaiting-spec-approval` → `designing` → `planning` → `awaiting-plan-approval` → `planned` → `developing` → `reviewing` → `qa` → `done`

旁支状态：`blocked`、`cancelled`。历史名 `awaiting-merge` 已废弃。

`done` = 工作流关闭（QA Pass + 合并/完成授权）；是否已合入目标分支以 git/PR 为准。QA Pass 待授权期间不单独提交 `review.md`/`qa-report.md`；授权后与 `done` 一次提交。详见 [`docs/README.md`](../README.md#状态机与回退)、[`standards/git.md`](../standards/git.md)。

调度主键为 `(feature-id, sub-feature-id)`。未拆分时二者相同。同一 `feature-id` 的后续行可省略重复的 `feature-id`；空 `feature-id` 表示继承上一非空值。已拆分时「目录」列须指向各子工作项目录，不得省略为继承总览根目录。

## 活跃工作项

| feature-id | sub-feature-id | 描述 | 状态 | 路径 | Spec 门禁 | 后续步骤 | 目录 |
|---|---|---|---|---|---|---|---|
| architecture-overview | architecture-overview | 项目介绍 + 既有系统架构设计文档与架构图（纯文档） | designing（Design 已完成） | standard | skipped | 待用户确认架构文档后关闭工作项（无代码合入） | [docs/features/architecture-overview/](../features/architecture-overview/) |
| ggtest-pg | ggtest-pg | PostgreSQL + CLI `.env` 配置（多库扩展；隔离；凭据安全） | done | full | required | 已授权关闭；合入 main（不归档，用户未要求） | [docs/features/ggtest-pg/](../features/ggtest-pg/) |

## 已归档

归档目录：[`docs/archive/YYYY/`](../archive/)。

| feature-id | sub-feature-id | 最终状态 | 目录 |
|---|---|---|---|
| ggtest-core | ggtest-core（含 parser/normalize/runner-sqlite/cli-corpus 四切片） | done（父项归档；四切片均 done 且已合入 `main`） | [docs/archive/2026/ggtest-core/](../archive/2026/ggtest-core/) |
| ggtest-cli-report | ggtest-cli-report | done（已合入 `main`；用户授权关闭并归档） | [docs/archive/2026/ggtest-cli-report/](../archive/2026/ggtest-cli-report/) |
