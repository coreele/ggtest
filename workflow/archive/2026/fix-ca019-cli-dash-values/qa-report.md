# QA Report: fix-ca019-cli-dash-values

## 轮次

| 轮次 | 日期 | 实现版本 | 范围 | 结论 |
|---|---|---|---|---|
| 1 | 2026-08-13 | `cc052e1`（源分支 `fix-ca019-cli-dash-values`） | 首测 | Pass |

## 环境与命令

- 命令：`mvn -Dtest=CliArgumentParserTest test`、`mvn clean test`。

## 覆盖（对照 plan V1–V5；fast 无 Spec 验收）

| ID | 条目 | 结果 | 证据 |
|---|---|---|---|
| V1 | `--password -secret` 接受为密码 | Pass | `passwordStartingWithDashIsAccepted` |
| V2 | `--user -name` / `--env-file -path` 接受 | Pass | `valueStartingWithDashIsAcceptedForValueOptions` |
| V3 | 下一 token 是已知 flag 时仍报缺值 | Pass | `missingValueWhenNextTokenIsKnownFlag` |
| V4 | `--parallel -1` 仍报错（n<1） | Pass | 既有 `parallelNegativeYieldsUsageError` |
| V5 | `mvn clean test` | Pass | BUILD SUCCESS，Tests=**368** Failures=0 Errors=0 Skipped=34 |

## 文档与安全验收

| 项 | 结果 | 备注 |
|---|---|---|
| 用户可见文档 | N/A | 缺陷修复，无对外承诺变化 |
| 运维可执行文档 | N/A | — |
| 安全验证范围 | 通过 | `-` 开头密码不再被异常拒绝；密码不回显用例不回归 |

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
