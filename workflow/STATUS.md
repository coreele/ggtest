# 看板

> 由 Manager 唯一维护。本文件是**当前 Git 分支 / 工作树的看板视图**，不是跨分支全局数据库；并行工作项使用独立 `git worktree`。流程与状态定义见 [WORKFLOW.md](WORKFLOW.md)，本文件不重复定义规则。
> 活跃工作项记录 `workflow/workspace/<id>/<id>.md`，归档记录 `workflow/archive/<年>/<id>/<id>.md`。

## 概览

| 泳道 | 数量 | 工作项 |
|---|---|---|
| 等待用户 | 1 | rust-rewrite |
| 进行中 | 0 | — |
| 待办 | 2 | add-lcs-diff-guard, fix-sql-hash-comments |
| 阻塞 | 1 | add-report-format |
| 待归档 | 1 | execute-markdown-slt |
| 已归档 | 54 | — |

## 等待用户

> 流程已停住，需要你回话才能继续。

| 工作项 | 等待什么 | 停在此处自 | 摘要 |
|---|---|---|---|
| rust-rewrite | Spec 确认 | 2026-08-14 | Rust 重写 ggtest（ODBC，去 XuguDB） |

## 进行中

| 工作项 | 状态 | 路径 | 下一步 | 摘要 |
|---|---|---|---|---|
| — | | | | |

## 待办

| 工作项 | 路径 | 下一步 | 摘要 |
|---|---|---|---|
| add-lcs-diff-guard | fast | Planner | LCS diff 大小门限（CA-007） |
| fix-sql-hash-comments | fast | Planner | SQL 体内 `#` 注释裁剪 |

## 阻塞

| 工作项 | 阻塞原因 | 恢复条件 | 恢复后目标 |
|---|---|---|---|
| add-report-format | 用户优先级调整，暂停 P2 | 用户要求恢复 | designing |

## 待归档

> `done` 但尚未确认合入。确认后移入归档索引。

| 工作项 | 源分支 → 目标分支 | 待确认事项 |
|---|---|---|
| execute-markdown-slt | execute-markdown-slt → main | 已获用户合并授权，待合入并归档 |

## 归档索引

| 工作项 | 结果 | 记录 |
|---|---|---|
| add-build-plugins | done | [add-build-plugins.md](archive/2026/add-build-plugins/add-build-plugins.md) |
| add-ci-workflow | done | [add-ci-workflow.md](archive/2026/add-ci-workflow/add-ci-workflow.md) |
| add-conn-attribute | done | [add-conn-attribute.md](archive/2026/add-conn-attribute/add-conn-attribute.md) |
| add-dependency-check | done（经 add-build-plugins 合并交付） | [add-dependency-check.md](archive/2026/add-dependency-check/add-dependency-check.md) |
| add-jacoco-coverage | done（经 add-build-plugins 合并交付） | [add-jacoco-coverage.md](archive/2026/add-jacoco-coverage/add-jacoco-coverage.md) |
| add-parallel-execution | done | [add-parallel-execution.md](archive/2026/add-parallel-execution/add-parallel-execution.md) |
| add-spotbugs-analysis | done（经 add-build-plugins 合并交付） | [add-spotbugs-analysis.md](archive/2026/add-spotbugs-analysis/add-spotbugs-analysis.md) |
| add-statement-query-timeout | done | [add-statement-query-timeout.md](archive/2026/add-statement-query-timeout/add-statement-query-timeout.md) |
| add-trace-flag | done | [add-trace-flag.md](archive/2026/add-trace-flag/add-trace-flag.md) |
| architecture-overview | cancelled | [architecture-overview.md](archive/2026/architecture-overview/architecture-overview.md) |
| chore-audit-tails | done | [chore-audit-tails.md](archive/2026/chore-audit-tails/chore-audit-tails.md) |
| chore-maven-compiler-release | done | [chore-maven-compiler-release.md](archive/2026/chore-maven-compiler-release/chore-maven-compiler-release.md) |
| enhance-override | done | [enhance-override.md](archive/2026/enhance-override/enhance-override.md) |
| feat-cli-halt | done | [feat-cli-halt.md](archive/2026/feat-cli-halt/feat-cli-halt.md) |
| feat-override-results | done | [feat-override-results.md](archive/2026/feat-override-results/feat-override-results.md) |
| feat-statement-error-msg | done | [feat-statement-error-msg.md](archive/2026/feat-statement-error-msg/feat-statement-error-msg.md) |
| fix-aggfunc-sum-overflow | cancelled | [fix-aggfunc-sum-overflow.md](archive/2026/fix-aggfunc-sum-overflow/fix-aggfunc-sum-overflow.md) |
| fix-aggfunc-total-precision | cancelled | [fix-aggfunc-total-precision.md](archive/2026/fix-aggfunc-total-precision/fix-aggfunc-total-precision.md) |
| fix-ca015-dead-adapters | done | [fix-ca015-dead-adapters.md](archive/2026/fix-ca015-dead-adapters/fix-ca015-dead-adapters.md) |
| fix-ca016-stmt-error-conn | done | [fix-ca016-stmt-error-conn.md](archive/2026/fix-ca016-stmt-error-conn/fix-ca016-stmt-error-conn.md) |
| fix-ca017-override-atomic-move | done | [fix-ca017-override-atomic-move.md](archive/2026/fix-ca017-override-atomic-move/fix-ca017-override-atomic-move.md) |
| fix-ca018-search-path-validation | done | [fix-ca018-search-path-validation.md](archive/2026/fix-ca018-search-path-validation/fix-ca018-search-path-validation.md) |
| fix-ca019-cli-dash-values | done | [fix-ca019-cli-dash-values.md](archive/2026/fix-ca019-cli-dash-values/fix-ca019-cli-dash-values.md) |
| fix-ca020-main-fatal-catch | done | [fix-ca020-main-fatal-catch.md](archive/2026/fix-ca020-main-fatal-catch/fix-ca020-main-fatal-catch.md) |
| fix-ca023-prepare-conn-leak | done | [fix-ca023-prepare-conn-leak.md](archive/2026/fix-ca023-prepare-conn-leak/fix-ca023-prepare-conn-leak.md) |
| fix-cli-credential-redaction | done | [fix-cli-credential-redaction.md](archive/2026/fix-cli-credential-redaction/fix-cli-credential-redaction.md) |
| fix-jdbc-executor-dedup | done | [fix-jdbc-executor-dedup.md](archive/2026/fix-jdbc-executor-dedup/fix-jdbc-executor-dedup.md) |
| fix-normalize-integer-float | done | [fix-normalize-integer-float.md](archive/2026/fix-normalize-integer-float/fix-normalize-integer-float.md) |
| fix-onlyif-skipif-hash-comments | done | [fix-onlyif-skipif-hash-comments.md](archive/2026/fix-onlyif-skipif-hash-comments/fix-onlyif-skipif-hash-comments.md) |
| fix-override-direct-write | done | [fix-override-direct-write.md](archive/2026/fix-override-direct-write/fix-override-direct-write.md) |
| fix-override-rowwise | done | [fix-override-rowwise.md](archive/2026/fix-override-rowwise/fix-override-rowwise.md) |
| fix-parallel-halt-race | done | [fix-parallel-halt-race.md](archive/2026/fix-parallel-halt-race/fix-parallel-halt-race.md) |
| fix-pg-teardown-once | done | [fix-pg-teardown-once.md](archive/2026/fix-pg-teardown-once/fix-pg-teardown-once.md) |
| fix-rowwise-value-per-line-compat | done | [fix-rowwise-value-per-line-compat.md](archive/2026/fix-rowwise-value-per-line-compat/fix-rowwise-value-per-line-compat.md) |
| fix-shared-defaults | done | [fix-shared-defaults.md](archive/2026/fix-shared-defaults/fix-shared-defaults.md) |
| ggtest-cli-help | done | [ggtest-cli-help.md](archive/2026/ggtest-cli-help/ggtest-cli-help.md) |
| ggtest-cli-report | done | [ggtest-cli-report.md](archive/2026/ggtest-cli-report/ggtest-cli-report.md) |
| ggtest-core | cancelled | [ggtest-core.md](archive/2026/ggtest-core/ggtest-core.md) |
| ggtest-core-cli-corpus | done | [ggtest-core-cli-corpus.md](archive/2026/ggtest-core-cli-corpus/ggtest-core-cli-corpus.md) |
| ggtest-core-normalize | done | [ggtest-core-normalize.md](archive/2026/ggtest-core-normalize/ggtest-core-normalize.md) |
| ggtest-core-parser | done | [ggtest-core-parser.md](archive/2026/ggtest-core-parser/ggtest-core-parser.md) |
| ggtest-core-runner-sqlite | done | [ggtest-core-runner-sqlite.md](archive/2026/ggtest-core-runner-sqlite/ggtest-core-runner-sqlite.md) |
| ggtest-pg | done | [ggtest-pg.md](archive/2026/ggtest-pg/ggtest-pg.md) |
| ggtest-rowwise-expected | done | [ggtest-rowwise-expected.md](archive/2026/ggtest-rowwise-expected/ggtest-rowwise-expected.md) |
| improve-failure-output-format | done | [improve-failure-output-format.md](archive/2026/improve-failure-output-format/improve-failure-output-format.md) |
| improve-multi-failure-report | done | [improve-multi-failure-report.md](archive/2026/improve-multi-failure-report/improve-multi-failure-report.md) |
| mysql-engine | done（已合入 `main` `134bad3`） | [mysql-engine.md](archive/2026/mysql-engine/mysql-engine.md) |
| query-header-kv-attrs | done | [query-header-kv-attrs.md](archive/2026/query-header-kv-attrs/query-header-kv-attrs.md) |
| refactor-cli-session-boundaries | done | [refactor-cli-session-boundaries.md](archive/2026/refactor-cli-session-boundaries/refactor-cli-session-boundaries.md) |
| refactor-filerunner-responsibilities | done | [refactor-filerunner-responsibilities.md](archive/2026/refactor-filerunner-responsibilities/refactor-filerunner-responsibilities.md) |
| sync-readme | done | [sync-readme.md](archive/2026/sync-readme/sync-readme.md) |
| sync-vs-slt-grammar | done | [sync-vs-slt-grammar.md](archive/2026/sync-vs-slt-grammar/sync-vs-slt-grammar.md) |
| tighten-cli-boundary-validation | done | [tighten-cli-boundary-validation.md](archive/2026/tighten-cli-boundary-validation/tighten-cli-boundary-validation.md) |
| xugu-engine | done | [xugu-engine.md](archive/2026/xugu-engine/xugu-engine.md) |
