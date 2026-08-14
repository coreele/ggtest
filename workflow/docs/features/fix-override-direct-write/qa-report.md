# QA Report: fix-override-direct-write

## 轮次

| 轮次 | 日期 | 实现版本 | 范围 | 结论 |
|---|---|---|---|---|
| 1 | 2026-08-13 | `f1c9ad9` | 首测 | Pass |

## 环境与命令

- 命令：`mvn test`、`mvn spotbugs:check`；端到端 SQLite `:memory:`。

## 覆盖

| ID | 条目 | 结果 | 证据 |
|---|---|---|---|
| req1 | `--override` 全绿 query 也 OVERRIDDEN（非 PASS） | Pass | 端到端 `overridden=1`；`FileRunnerTest.overrideEnabled_allPassed_fileRewrittenAsOverridden`、`SqlLogicTestRunnerTest.overrideEnabled_doesNotTriggerHalt` |
| req1 | 内容幂等（期望=实际时内容不变） | Pass | `overrideEnabled_noMismatch_fileContentUnchanged` |
| req2 | `--separator` 覆盖 header 已声明 separator | Pass | `overrideEnabled_separatorOverridesHeaderSeparator` + 端到端 `separator=,`→`separator=|` |
| req2 | 单列不注入 separator | Pass | 端到端 |
| 回归 | `mvn test` 0 failures | Pass | 407/0（50 既有 skip） |

## 文档与安全验收

| 项 | 结果 | 备注 |
|---|---|---|
| 用户可见文档 | Pass | README `--override`/`--separator` 描述更新 |
| 安全验证范围 | 通过 | 无新依赖/凭据面；spotbugs 通过 |

## 缺陷

| ID | 严重度 | 摘要 | 状态 | 处理说明 | 验证证据 |
|---|---|---|---|---|---|
| — | — | 无 | — | — | — |

## 结论

- 总体: Pass
- 恢复条件: N/A
- 合并: 待用户授权
