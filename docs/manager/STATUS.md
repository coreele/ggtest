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

| feature-id | sub-feature-id | 描述 | 状态 | 路径 | Spec 门禁 | 后续步骤 | 目录 |
|---|---|---|---|---|---|---|---|
| chore-maven-compiler-release | chore-maven-compiler-release | pom.xml 显式 maven-compiler-plugin + release=17，修复 Maven 3.6.3 Source/Target 5 | done | fast | skipped | 已授权合入 main（合入以 git 为准；不 push）；归档待用户要求关闭父项 | [docs/features/chore-maven-compiler-release/](../features/chore-maven-compiler-release/) |
| improve-multi-failure-report | improve-multi-failure-report | 单文件多失败 CLI 报告：块间空行 + 无缩进 at | done | standard | required | 已授权合入 main（合入以 git 为准；不 push）；归档待用户要求关闭父项 | [docs/features/improve-multi-failure-report/](../features/improve-multi-failure-report/) |
| fix-aggfunc-sum-overflow | fix-aggfunc-sum-overflow | sum() 整数溢出：SQLite overflow vs ggtest 执行失败 / TBD 语料 | backlog | standard | required | WI-1 done；待用户指示再调度 Analyst | [docs/features/fix-aggfunc-sum-overflow/](../features/fix-aggfunc-sum-overflow/) |
| fix-aggfunc-total-precision | fix-aggfunc-total-precision | total() 大浮点 %.3f 精度漂移（~491） | backlog | standard | required | WI-1 done；待用户指示再推进 | [docs/features/fix-aggfunc-total-precision/](../features/fix-aggfunc-total-precision/) |
| chore-audit-tails | chore-audit-tails | 审计尾巴收口（CA-008 Javadoc；TTY color；PG 非空密码；select1–5 硬验收；排除 CA-007） | done | fast | skipped | 已授权合入 main（合入以 git 为准；不 push）；归档待用户要求关闭父项 | [docs/features/chore-audit-tails/](../features/chore-audit-tails/) |

## 已归档

归档根目录：[`docs/archive/YYYY/`](../archive/)。「目录」列直接链接各工作项的 `docs/archive/YYYY/<feature-id>/manager.md`（同目录另含原 features 产物，若有）。

| feature-id | sub-feature-id | 最终状态 | 目录 |
|---|---|---|---|
| fix-normalize-integer-float | fix-normalize-integer-float | done（已合入 `main`；已归档） | [docs/archive/2026/fix-normalize-integer-float/manager.md](../archive/2026/fix-normalize-integer-float/manager.md) |
| fix-onlyif-skipif-hash-comments | fix-onlyif-skipif-hash-comments | done（已合入 `main`；已归档） | [docs/archive/2026/fix-onlyif-skipif-hash-comments/manager.md](../archive/2026/fix-onlyif-skipif-hash-comments/manager.md) |
| architecture-overview | architecture-overview | cancelled（用户舍弃；草稿已删；工作项记录已归档） | [docs/archive/2026/architecture-overview/manager.md](../archive/2026/architecture-overview/manager.md) |
| fix-rowwise-value-per-line-compat | fix-rowwise-value-per-line-compat | done（已合入 `main`；已归档） | [docs/archive/2026/fix-rowwise-value-per-line-compat/manager.md](../archive/2026/fix-rowwise-value-per-line-compat/manager.md) |
| ggtest-pg | ggtest-pg | done（已合入 `main`；已归档） | [docs/archive/2026/ggtest-pg/manager.md](../archive/2026/ggtest-pg/manager.md) |
| fix-shared-defaults | fix-shared-defaults | done（已合入 `main`；已归档） | [docs/archive/2026/fix-shared-defaults/manager.md](../archive/2026/fix-shared-defaults/manager.md) |
| fix-jdbc-executor-dedup | fix-jdbc-executor-dedup | done（已合入 `main`；已归档） | [docs/archive/2026/fix-jdbc-executor-dedup/manager.md](../archive/2026/fix-jdbc-executor-dedup/manager.md) |
| fix-cli-credential-redaction | fix-cli-credential-redaction | done（已合入 `main`；已归档） | [docs/archive/2026/fix-cli-credential-redaction/manager.md](../archive/2026/fix-cli-credential-redaction/manager.md) |
| fix-pg-teardown-once | fix-pg-teardown-once | done（已合入 `main`；已归档） | [docs/archive/2026/fix-pg-teardown-once/manager.md](../archive/2026/fix-pg-teardown-once/manager.md) |
| refactor-cli-session-boundaries | refactor-cli-session-boundaries | done（已合入 `main`；已归档） | [docs/archive/2026/refactor-cli-session-boundaries/manager.md](../archive/2026/refactor-cli-session-boundaries/manager.md) |
| ggtest-core | ggtest-core（含 parser/normalize/runner-sqlite/cli-corpus 四切片） | done（父项归档；四切片均 done 且已合入 `main`） | [docs/archive/2026/ggtest-core/manager.md](../archive/2026/ggtest-core/manager.md) |
| ggtest-cli-report | ggtest-cli-report | done（已合入 `main`；已归档） | [docs/archive/2026/ggtest-cli-report/manager.md](../archive/2026/ggtest-cli-report/manager.md) |
| ggtest-rowwise-expected | ggtest-rowwise-expected | done（已合入 `main`；已归档） | [docs/archive/2026/ggtest-rowwise-expected/manager.md](../archive/2026/ggtest-rowwise-expected/manager.md) |
