# Manager Status

> 工作流、角色、门禁和状态规则以 [`workflow/README.md`](../../README.md) 为唯一权威说明。本文件仅维护状态图例、活跃工作项和归档索引，且仅由 Manager 修改。
>
> `workflow/docs/manager/` 下除本文件外仅保留**活跃**工作项记录；已归档项的工作项记录位于 `workflow/docs/archive/YYYY/<feature-id>/manager.md`。

## 状态图例

主状态序列：

`backlog` → `speccing` → `awaiting-spec-approval` → `designing` → `planning` → `awaiting-plan-approval` → `planned` → `developing` → `reviewing` → `qa` → `done`

旁支状态：`blocked`、`cancelled`。历史名 `awaiting-merge` 已废弃。

`done` = 工作流关闭（QA Pass + 合并/完成授权）；是否已合入目标分支以 git/PR 为准。QA Pass 待授权期间不单独提交 `review.md`/`qa-report.md`；授权后与 `done` 一次提交。详见 [`workflow/README.md`](../../README.md#状态机与回退)、[`standards/git.md`](../standards/git.md)。

调度主键为 `(feature-id, sub-feature-id)`。未拆分时二者相同。同一 `feature-id` 的后续行可省略重复的 `feature-id`；空 `feature-id` 表示继承上一非空值。已拆分时「目录」列须指向各子工作项目录，不得省略为继承总览根目录。

## 活跃工作项

| feature-id | sub-feature-id | 描述 | 状态 | 路径 | Spec 门禁 | 后续步骤 | 目录 |
|---|---|---|---|---|---|---|---|
| sync-vs-slt-grammar | sync-vs-slt-grammar | vs-slt 插件高亮同步当前 SLT 语法 | planned | standard | skipped | Developer | [workflow/docs/features/sync-vs-slt-grammar/](../features/sync-vs-slt-grammar/) |
| add-report-format | add-report-format | `--format junit-xml\|json` 测试报告输出 | blocked | standard | required | 恢复后 → Planner | [workflow/docs/features/add-report-format/](../features/add-report-format/) |
| add-parallel-execution | add-parallel-execution | `--parallel <N>` 并行执行独立文件 | done | standard | required | — | [workflow/docs/features/add-parallel-execution/](../features/add-parallel-execution/) |
| fix-sql-hash-comments | fix-sql-hash-comments | SQL 体内 `#` 注释裁剪 | backlog | fast | skipped | Planner | [workflow/docs/features/fix-sql-hash-comments/](../features/fix-sql-hash-comments/) |
| add-lcs-diff-guard | add-lcs-diff-guard | LCS diff O(n*m) 加大小门限 | backlog | fast | skipped | Planner | [workflow/docs/features/add-lcs-diff-guard/](../features/add-lcs-diff-guard/) |
| ggtest-cli-help | ggtest-cli-help | --help / -h 用法信息 | done | fast | skipped | — | [workflow/docs/features/ggtest-cli-help/](../features/ggtest-cli-help/) |
| query-header-kv-attrs | query-header-kv-attrs | separator 改为 key=value 形式 | done | standard | required | — | [workflow/docs/features/query-header-kv-attrs/](../features/query-header-kv-attrs/) |
| fix-override-rowwise | fix-override-rowwise | --override row-wise 写入格式 bug + 拼写提示 | done | fast | skipped | — | [workflow/docs/features/fix-override-rowwise/](../features/fix-override-rowwise/) |
| add-statement-query-timeout | add-statement-query-timeout | timeout=<ms> 属性 | done | standard | required | — | [workflow/docs/features/add-statement-query-timeout/](../features/add-statement-query-timeout/) |
| add-conn-attribute | add-conn-attribute | conn=<name> 多连接并发 | done | standard | required | — | [workflow/docs/features/add-conn-attribute/](../features/add-conn-attribute/) |
| fix-ca015-dead-adapters | fix-ca015-dead-adapters | 删除死代码适配器（CA-015） | done | fast | skipped | — | [workflow/docs/features/fix-ca015-dead-adapters/](../features/fix-ca015-dead-adapters/) |
| fix-ca016-stmt-error-conn | fix-ca016-stmt-error-conn | statement error 消息提取遗漏 conn=（CA-016） | done | fast | skipped | — | [workflow/docs/features/fix-ca016-stmt-error-conn/](../features/fix-ca016-stmt-error-conn/) |
| refactor-filerunner-responsibilities | refactor-filerunner-responsibilities | 拆分 FileRunner 职责（CA-010） | done | standard | skipped | — | [workflow/docs/features/refactor-filerunner-responsibilities/](../features/refactor-filerunner-responsibilities/) |
| sync-readme | sync-readme | README 同步 separator=/timeout=/conn=/--help | done | fast | skipped | — | [workflow/docs/features/sync-readme/](../features/sync-readme/) |
| add-build-plugins | add-build-plugins | jacoco + spotbugs + dependency-check | done | fast | skipped | — | [workflow/docs/features/add-build-plugins/](../features/add-build-plugins/) |

## 已归档

归档根目录：[`workflow/docs/archive/YYYY/`](../archive/)。「目录」列直接链接各工作项的 `workflow/docs/archive/YYYY/<feature-id>/manager.md`（同目录另含原 features 产物，若有）。

| feature-id | sub-feature-id | 最终状态 | 目录 |
|---|---|---|---|
| add-jacoco-coverage | add-jacoco-coverage | done（经 `add-build-plugins` 合并交付，commit `257b7d8`） | [workflow/docs/archive/2026/add-jacoco-coverage/manager.md](../archive/2026/add-jacoco-coverage/manager.md) |
| add-spotbugs-analysis | add-spotbugs-analysis | done（经 `add-build-plugins` 合并交付，commit `257b7d8`） | [workflow/docs/archive/2026/add-spotbugs-analysis/manager.md](../archive/2026/add-spotbugs-analysis/manager.md) |
| add-dependency-check | add-dependency-check | done（经 `add-build-plugins` 合并交付，commit `257b7d8`） | [workflow/docs/archive/2026/add-dependency-check/manager.md](../archive/2026/add-dependency-check/manager.md) |
| add-ci-workflow | add-ci-workflow | done（已合入 `main`；已归档） | [workflow/docs/archive/2026/add-ci-workflow/manager.md](../archive/2026/add-ci-workflow/manager.md) |
| fix-normalize-integer-float | fix-normalize-integer-float | done（已合入 `main`；已归档） | [workflow/docs/archive/2026/fix-normalize-integer-float/manager.md](../archive/2026/fix-normalize-integer-float/manager.md) |
| fix-onlyif-skipif-hash-comments | fix-onlyif-skipif-hash-comments | done（已合入 `main`；已归档） | [workflow/docs/archive/2026/fix-onlyif-skipif-hash-comments/manager.md](../archive/2026/fix-onlyif-skipif-hash-comments/manager.md) |
| architecture-overview | architecture-overview | cancelled（用户舍弃；草稿已删；工作项记录已归档） | [workflow/docs/archive/2026/architecture-overview/manager.md](../archive/2026/architecture-overview/manager.md) |
| fix-aggfunc-sum-overflow | fix-aggfunc-sum-overflow | cancelled（非 harness 设计缺陷；SQLite sum 溢出语义 vs JDBC/引擎错误形态；无需修复；已归档） | [workflow/docs/archive/2026/fix-aggfunc-sum-overflow/manager.md](../archive/2026/fix-aggfunc-sum-overflow/manager.md) |
| fix-aggfunc-total-precision | fix-aggfunc-total-precision | cancelled / wontfix（已知限制：极端量级 R 的 String.format vs sqlite3_snprintf；不移植 float printf、不改语料；已归档） | [workflow/docs/archive/2026/fix-aggfunc-total-precision/manager.md](../archive/2026/fix-aggfunc-total-precision/manager.md) |
| fix-rowwise-value-per-line-compat | fix-rowwise-value-per-line-compat | done（已合入 `main`；已归档） | [workflow/docs/archive/2026/fix-rowwise-value-per-line-compat/manager.md](../archive/2026/fix-rowwise-value-per-line-compat/manager.md) |
| ggtest-pg | ggtest-pg | done（已合入 `main`；已归档） | [workflow/docs/archive/2026/ggtest-pg/manager.md](../archive/2026/ggtest-pg/manager.md) |
| fix-shared-defaults | fix-shared-defaults | done（已合入 `main`；已归档） | [workflow/docs/archive/2026/fix-shared-defaults/manager.md](../archive/2026/fix-shared-defaults/manager.md) |
| fix-jdbc-executor-dedup | fix-jdbc-executor-dedup | done（已合入 `main`；已归档） | [workflow/docs/archive/2026/fix-jdbc-executor-dedup/manager.md](../archive/2026/fix-jdbc-executor-dedup/manager.md) |
| fix-cli-credential-redaction | fix-cli-credential-redaction | done（已合入 `main`；已归档） | [workflow/docs/archive/2026/fix-cli-credential-redaction/manager.md](../archive/2026/fix-cli-credential-redaction/manager.md) |
| fix-pg-teardown-once | fix-pg-teardown-once | done（已合入 `main`；已归档） | [workflow/docs/archive/2026/fix-pg-teardown-once/manager.md](../archive/2026/fix-pg-teardown-once/manager.md) |
| refactor-cli-session-boundaries | refactor-cli-session-boundaries | done（已合入 `main`；已归档） | [workflow/docs/archive/2026/refactor-cli-session-boundaries/manager.md](../archive/2026/refactor-cli-session-boundaries/manager.md) |
| ggtest-core | ggtest-core（含 parser/normalize/runner-sqlite/cli-corpus 四切片） | done（父项归档；四切片均 done 且已合入 `main`） | [workflow/docs/archive/2026/ggtest-core/manager.md](../archive/2026/ggtest-core/manager.md) |
| ggtest-cli-report | ggtest-cli-report | done（已合入 `main`；已归档） | [workflow/docs/archive/2026/ggtest-cli-report/manager.md](../archive/2026/ggtest-cli-report/manager.md) |
| ggtest-rowwise-expected | ggtest-rowwise-expected | done（已合入 `main`；已归档） | [workflow/docs/archive/2026/ggtest-rowwise-expected/manager.md](../archive/2026/ggtest-rowwise-expected/manager.md) |
| feat-override-results | feat-override-results | done（已合入 `main`；已归档） | [workflow/docs/archive/2026/feat-override-results/manager.md](../archive/2026/feat-override-results/manager.md) |
| chore-maven-compiler-release | chore-maven-compiler-release | done（已合入 `main`；已归档） | [workflow/docs/archive/2026/chore-maven-compiler-release/manager.md](../archive/2026/chore-maven-compiler-release/manager.md) |
| improve-multi-failure-report | improve-multi-failure-report | done（已合入 `main`；已归档） | [workflow/docs/archive/2026/improve-multi-failure-report/manager.md](../archive/2026/improve-multi-failure-report/manager.md) |
| chore-audit-tails | chore-audit-tails | done（已合入 `main`；已归档） | [workflow/docs/archive/2026/chore-audit-tails/manager.md](../archive/2026/chore-audit-tails/manager.md) |
| feat-cli-halt | feat-cli-halt | done（已合入 `main`；已归档） | [workflow/docs/archive/2026/feat-cli-halt/manager.md](../archive/2026/feat-cli-halt/manager.md) |
| improve-failure-output-format | improve-failure-output-format | done（已合入 `main`；已归档） | [workflow/docs/archive/2026/improve-failure-output-format/manager.md](../archive/2026/improve-failure-output-format/manager.md) |
| feat-statement-error-msg | feat-statement-error-msg | done（已合入 `main`；已归档） | [workflow/docs/archive/2026/feat-statement-error-msg/manager.md](../archive/2026/feat-statement-error-msg/manager.md) |
