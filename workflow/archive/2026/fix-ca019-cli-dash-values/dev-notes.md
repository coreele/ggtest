# Dev Notes: fix-ca019-cli-dash-values

## 实现摘要

- 分支：`fix-ca019-cli-dash-values` ← `main`。
- 修复 CA-019：`CliArgumentParser.requireValue` 原把任何 `startsWith("-")` 当缺值，拒掉 `--password -secret` 等合法值。
- 改为：新增 `OPTION_FLAGS`（全部 13 个 flag）；`requireValue` 仅在越界或下一 token ∈ OPTION_FLAGS 时报「missing value」，其余一律接受为值。

## 决策

- 判定收紧为「下一 token 是已知 flag」而非「以 `-` 开头」。理由：缺值的本质是「下一个看起来是另一个选项」；只有已知 flag 才能确定是选项。`-secret`/`-1`/`--bogus`(字面值)/`-path` 既非已知 flag，视为值。
- 未采用「`--` 终止选项解析」方案（改动面更大、超出审计建议范围）；按审计首选「对值选项允许 `-` 开头」实现，且对所有值选项统一生效。
- 边界保持：`--parallel -1` → `-1` 接受为值 → parseInt=-1 → `n<1` 报错（消息含 parallel）；`--url --user`（--user 是 flag）→ 报缺值。

## 变更路径

| 任务 | 路径 |
|---|---|
| 解析器 | `src/main/java/com/ggtest/cli/CliArgumentParser.java` |
| 测试 | `src/test/java/com/ggtest/cli/CliArgumentParserTest.java` |

## 验证

| 命令 | 结果 |
|---|---|
| `mvn -Dtest=CliArgumentParserTest test` | Tests=**27** Failures=0 Errors=0 Skipped=0 |
| `mvn clean test` | Tests=**368** Failures=0 Errors=0 Skipped=34；BUILD SUCCESS |

新增测试：`passwordStartingWithDashIsAccepted`、`valueStartingWithDashIsAcceptedForValueOptions`（--user/-name、--env-file/-path）、`missingValueWhenNextTokenIsKnownFlag`（--password --user → missing）。既有 `parallelNegativeYieldsUsageError`、各 missing/unknown 用例不回归。

## 文档影响

| 类别 | 已更新路径或交接说明 |
|---|---|
| 开发文档 | N/A |
| 用户文档 | N/A（缺陷修复，README 无需改） |
| 运维文档 | N/A |

## 未解决风险 / 验证缺口

| 项 | 原因 | 风险 | 恢复条件 |
|---|---|---|---|
| N/A | | | |

## QA 修复回执

| 缺陷 ID | 处理 | 摘要 | 验证 | 建议复测 |
|---|---|---|---|---|
| — | N/A | 本轮无 QA Fail | — | — |
