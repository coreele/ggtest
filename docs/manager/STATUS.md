# Manager Status

> 工作流、角色、门禁和状态规则以 [`docs/README.md`](../README.md) 为唯一权威说明。本文件仅维护状态图例、活跃工作项和归档索引，且仅由 Manager 修改。

## 状态图例

主状态序列：

`backlog` → `speccing` → `awaiting-spec-approval` → `designing` → `planning` → `awaiting-plan-approval` → `planned` → `developing` → `reviewing` → `qa` → `awaiting-merge` → `done`

旁支状态：`blocked`、`cancelled`。

状态可按门禁跳过不适用的阶段；具体转换、恢复和归档规则见 [`docs/README.md`](../README.md#状态机与回退)。

## 活跃工作项

| feature-id | 描述 | 状态 | 路径 | Spec 门禁 | 后续步骤 | 目录 |
|---|---|---|---|---|---|---|
| ggtest-core | 【Epic】GGTEST 总览；已拆为 4 子项，父项不写 Plan | blocked（decomposed） | full | required（总览） | 跟踪子项；四子项均 done 后关闭 | [docs/features/ggtest-core/](../features/ggtest-core/) |
| ggtest-parser | 解析 `.test`/`.slt` → 记录模型；错误含文件+行号 | awaiting-spec-approval | full | required | **建议优先**：用户确认 Spec → Design/Plan | [docs/features/ggtest-parser/](../features/ggtest-parser/) |
| ggtest-normalize | I/T/R 规范化、排序、MD5、hash-threshold | awaiting-spec-approval | full | required | 用户确认 Spec → Plan（Design skipped） | [docs/features/ggtest-normalize/](../features/ggtest-normalize/) |
| ggtest-runner-sqlite | Runner + 执行器抽象 + SQLite JDBC | awaiting-spec-approval | full | required | 用户确认 Spec；Design 依赖上游就绪 | [docs/features/ggtest-runner-sqlite/](../features/ggtest-runner-sqlite/) |
| ggtest-cli-corpus | CLI、统计、退出码、官方语料硬验收 | awaiting-spec-approval | full | required | 用户确认 Spec；最后集成 | [docs/features/ggtest-cli-corpus/](../features/ggtest-cli-corpus/) |

## 已归档

归档目录：[`docs/archive/YYYY/`](../archive/)。

| feature-id | 最终状态 | 目录 |
|---|---|---|
| | | |
