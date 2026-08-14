# QA Report: enhance-override

## 轮次

| 轮次 | 日期 | 实现版本 | 范围 | 结论 |
|---|---|---|---|---|
| 1 | 2026-08-13 | `3034c39` | 首测 | Pass |
| 2 | 2026-08-13 | `de5ce0a` | 增补：`--separator` 简写 + PASS 强制行式 | Pass |

## 环境与命令

- 命令：`mvn test`、`mvn spotbugs:check`；端到端用打包 jar（SQLite `:memory:`）。

## 覆盖（对照 Spec P0/P1 + Plan V1–V5）

| ID | 条目 | 结果 | 证据 |
|---|---|---|---|
| P0-1 签名对齐 | `query T` 实际 2 列 → 签名改写 + 重跑全绿 | Pass | 端到端：`query T` → `query IT` + 值逐行；重跑 PASSED |
| P0-2 类型签名 | I/R/T 推断 | Pass | `TypeSignatureInfererTest`（7 用例） |
| P0-3 查询失败 → statement error | `SELECT * FROM missing_table` → `statement error`（脱敏） | Pass | 端到端 + `SqlLogicTestRunnerTest` |
| P0-4 statement ok 失败 → statement error | — | Pass | `MainOrchestrationTest.overrideEnabled_statementOkFailure_rewrittenToStatementError` |
| P0-5 separator | `--override-separator "|"` → 头含 `separator=|` + 行式 | Pass | 端到端 + `MainOrchestrationTest.overrideEnabled_separatorRowWiseOutput` |
| P0-6 默认 value-per-line | 未指定 separator | Pass | 端到端签名对齐写值逐行 |
| P0-7 回归 | 既有 --override/测试/非 override | Pass | `mvn test` 405/0 |
| P1-1 statement error msg 更新 | 既有 msg 不符 → 更新 | Pass | 既有 `overrideEnabled_statementErrorMessageMismatch_yieldsOverridden` 不回归 |
| P1-2 全 NULL/空集回退 | → T / 空签名 | Pass | `TypeSignatureInfererTest` |

## 文档与安全验收

| 项 | 结果 | 备注 |
|---|---|---|
| 用户可见文档 | Pass | README 选项表 + synopsis 含 `--override`/`--override-separator` |
| 运维可执行文档 | N/A | — |
| 安全验证范围 | 通过 | 无新依赖/凭据面；separator 校验无空白；spotbugs:check 通过 |

## 缺陷

| ID | 严重度 | 摘要 | 状态 | 处理说明 | 验证证据 |
|---|---|---|---|---|---|
| — | — | 无 | — | — | — |

## 阻塞（Blocked 时）

- 原因: N/A | 风险: — | 恢复条件: — | 复测范围: —

## 结论

- 总体: Pass
- 恢复条件: N/A
- 合并: 待用户授权（授权后 Manager 在源分支置 `done` 并与 `review.md`/`qa-report.md` 一次提交）
