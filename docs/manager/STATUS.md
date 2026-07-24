# Manager Status

> 工作流、角色、门禁和状态规则以 [`docs/README.md`](../README.md) 为唯一权威说明。本文件仅维护状态图例、活跃工作项和归档索引，且仅由 Manager 修改。

## 状态图例

主状态序列：

`backlog` → `speccing` → `awaiting-spec-approval` → `designing` → `planning` → `awaiting-plan-approval` → `planned` → `developing` → `reviewing` → `qa` → `done`

旁支状态：`blocked`、`cancelled`。历史名 `awaiting-merge` 已废弃。

`done` = 工作流关闭（QA Pass + 合并/完成授权）；是否已合入目标分支以 git/PR 为准。详见 [`docs/README.md`](../README.md#状态机与回退)。

调度主键为 `(feature-id, sub-feature-id)`。未拆分时二者相同。同一 `feature-id` 的后续行可省略重复的 `feature-id`（及相同的「目录」）；空 `feature-id` 表示继承上一非空值。

## 活跃工作项

| feature-id | sub-feature-id | 描述 | 状态 | 路径 | Spec 门禁 | 后续步骤 | 目录 |
|---|---|---|---|---|---|---|---|
| ggtest-core | ggtest-core | 【总览】GGTEST；子 Spec 同目录；不对总览写 Plan | blocked（tracking） | full | required（总览） | 跟踪子切片；四切片均 done 后关闭 | [docs/features/ggtest-core/](../features/ggtest-core/) |
| | parser | 解析 `.test`/`.slt` → 记录模型；错误含文件+行号 | done | full | required | 工作流已关闭（可合入）；合入以 git/PR 为准 | |
| | normalize | I/T/R 规范化、排序、MD5、hash-threshold | awaiting-spec-approval | full | required | 用户确认 Spec → Plan（Design skipped） | |
| | runner-sqlite | Runner + 执行器抽象 + SQLite JDBC | awaiting-spec-approval | full | required | 用户确认 Spec；Design 依赖上游就绪 | |
| | cli-corpus | CLI、统计、退出码、官方语料硬验收 | awaiting-spec-approval | full | required | 用户确认 Spec；最后集成 | |

## 已归档

归档目录：[`docs/archive/YYYY/`](../archive/)。

| feature-id | sub-feature-id | 最终状态 | 目录 |
|---|---|---|---|
| | | | |
