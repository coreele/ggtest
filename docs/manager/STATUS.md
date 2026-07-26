# Manager Status

> 工作流、角色、门禁和状态规则以 [`docs/README.md`](../README.md) 为唯一权威说明。本文件仅维护状态图例、活跃工作项和归档索引，且仅由 Manager 修改。
>
> `docs/manager/` 下除本文件外仅保留**活跃**工作项记录；已归档项的工作项记录位于 `docs/archive/YYYY/<feature-id>/manager.md`。

## 状态图例

主状态序列：

`backlog` → `speccing` → `awaiting-spec-approval` → `designing` → `planning` → `awaiting-plan-approval` → `planned` → `developing` → `reviewing` → `qa` → `done`

旁支状态：`blocked`、`cancelled`。历史名 `awaiting-merge` 已废弃。

`done` = 工作流关闭（QA Pass + 合并/完成授权）；是否已合入目标分支以 git/PR 为准。QA Pass 待授权期间不单独提交 `review.md`/`qa-report.md`；授权后与 `done` 一次提交。详见 [`docs/README.md`](../README.md#状态机与回退)、[`standards/git.md`](../standards/git.md)。

调度主键为 `(feature-id, sub-feature-id)`。未拆分时二者相同。同一 `feature-id` 的后续行可省略重复的 `feature-id`；空 `feature-id` 表示继承上一非空值。已拆分时「目录」列须指向各子工作项目录，不得省略为继承总览根目录。

## 活跃工作项

（无）

| feature-id | sub-feature-id | 描述 | 状态 | 路径 | Spec 门禁 | 后续步骤 | 目录 |
|---|---|---|---|---|---|---|---|

## 已归档

归档目录：[`docs/archive/YYYY/`](../archive/)。每个归档目录含原 `docs/features/<feature-id>/` 产物（若有）与工作项记录 [`manager.md`](../archive/2026/)。

| feature-id | sub-feature-id | 最终状态 | 目录 |
|---|---|---|---|
| architecture-overview | architecture-overview | cancelled（用户舍弃；草稿已删；工作项记录已归档） | [docs/archive/2026/architecture-overview/](../archive/2026/architecture-overview/) |
| fix-rowwise-value-per-line-compat | fix-rowwise-value-per-line-compat | done（已合入 `main`；已归档） | [docs/archive/2026/fix-rowwise-value-per-line-compat/](../archive/2026/fix-rowwise-value-per-line-compat/) |
| ggtest-pg | ggtest-pg | done（已合入 `main`；已归档） | [docs/archive/2026/ggtest-pg/](../archive/2026/ggtest-pg/) |
| fix-shared-defaults | fix-shared-defaults | done（已合入 `main`；已归档） | [docs/archive/2026/fix-shared-defaults/](../archive/2026/fix-shared-defaults/) |
| fix-jdbc-executor-dedup | fix-jdbc-executor-dedup | done（已合入 `main`；已归档） | [docs/archive/2026/fix-jdbc-executor-dedup/](../archive/2026/fix-jdbc-executor-dedup/) |
| fix-cli-credential-redaction | fix-cli-credential-redaction | done（已合入 `main`；已归档） | [docs/archive/2026/fix-cli-credential-redaction/](../archive/2026/fix-cli-credential-redaction/) |
| fix-pg-teardown-once | fix-pg-teardown-once | done（已合入 `main`；已归档） | [docs/archive/2026/fix-pg-teardown-once/](../archive/2026/fix-pg-teardown-once/) |
| refactor-cli-session-boundaries | refactor-cli-session-boundaries | done（已合入 `main`；已归档） | [docs/archive/2026/refactor-cli-session-boundaries/](../archive/2026/refactor-cli-session-boundaries/) |
| ggtest-core | ggtest-core（含 parser/normalize/runner-sqlite/cli-corpus 四切片） | done（父项归档；四切片均 done 且已合入 `main`） | [docs/archive/2026/ggtest-core/](../archive/2026/ggtest-core/) |
| ggtest-cli-report | ggtest-cli-report | done（已合入 `main`；已归档） | [docs/archive/2026/ggtest-cli-report/](../archive/2026/ggtest-cli-report/) |
| ggtest-rowwise-expected | ggtest-rowwise-expected | done（已合入 `main`；已归档） | [docs/archive/2026/ggtest-rowwise-expected/](../archive/2026/ggtest-rowwise-expected/) |
