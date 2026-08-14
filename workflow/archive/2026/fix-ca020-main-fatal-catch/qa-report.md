# QA Report: fix-ca020-main-fatal-catch

## 轮次

| 轮次 | 日期 | 实现版本 | 范围 | 结论 |
|---|---|---|---|---|
| 1 | 2026-08-13 | `0d75628`（源分支 `fix-ca020-main-fatal-catch`） | 首测 | Pass |

## 环境与命令

- 命令：`mvn -Dtest=MainOrchestrationTest#unexpectedExceptionExitsTwoWithRedactedFatalSummary test`、`mvn clean test`。

## 覆盖（对照 plan V1–V5；fast 无 Spec 验收）

| ID | 条目 | 结果 | 证据 |
|---|---|---|---|
| V1 | 非 UsageException 异常 → exit 2 | Pass | 新增测试 exit=2 |
| V2 | stderr 含 fatal 摘要 | Pass | 含 `Error: fatal` + `RuntimeException` |
| V3 | 摘要经脱敏（URL userinfo） | Pass | 不含 `alice:hunter2` / `hunter2` |
| V4 | 既有 UsageException 路径不回归 | Pass | 既有 usage/missing 用例仍 exit 2 |
| V5 | `mvn clean test` | Pass | BUILD SUCCESS，Tests=**369** Failures=0 Errors=0 Skipped=34 |

## 文档与安全验收

| 项 | 结果 | 备注 |
|---|---|---|
| 用户可见文档 | N/A | 类 Javadoc 已声明 exit 2 覆盖 fatal；实现补齐 |
| 运维可执行文档 | N/A | — |
| 安全验证范围 | 通过 | 异常消息经 URL userinfo 脱敏；查询参数凭据缺口属 CA-009 |

## 缺陷

| ID | 严重度 | 摘要 | 状态 | 处理说明 | 验证证据 |
|---|---|---|---|---|---|
| — | — | 无 | — | — | — |

## 阻塞（Blocked 时）

- 原因: N/A | 风险: — | 恢复条件: — | 复测范围: —

## 结论

- 总体: Pass
- 恢复条件: N/A
- 合并: 待用户授权（已授权；源分支 done 一次提交）
