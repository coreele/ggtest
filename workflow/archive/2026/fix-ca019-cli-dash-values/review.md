# Review: fix-ca019-cli-dash-values

## 审阅范围

- 实现版本 / 提交: `cc052e1`（源分支 `fix-ca019-cli-dash-values`）
- 依据: [plan.md](plan.md)；fast 路径

## 实现正确性

- `requireValue` 判定由 `args[index].startsWith("-")` 收紧为 `OPTION_FLAGS.contains(next)`，正确区分「下一 token 是已知 flag（真缺值）」与「值恰好以 `-` 开头」。OPTION_FLAGS 覆盖全部 13 个 flag（含 `-h`），与 switch 分支一致。✓
- 边界保持：`--parallel -1` → `-1` 接受为值 → `n<1` 报错；越界（`--url` 为末参）仍报缺值；`--url --user`（`--user` 是 flag）报缺值。✓
- 副作用面小：仅 `requireValue` 与新增常量；switch 与其余校验不变。
- 范围守纪律：仅 CliArgumentParser + 其测试。

## 测试有效性

- 新增 3 测试：`passwordStartingWithDashIsAccepted`（`-secret`）、`valueStartingWithDashIsAcceptedForValueOptions`（`--user -name`、`--env-file -path`）、`missingValueWhenNextTokenIsKnownFlag`（`--password --user` → missing）。✓
- 既有 `parallelNegativeYieldsUsageError`、`parallelMissingValueYieldsUsageError`、各 unknown-option 用例不回归。✓
- 验证：CliArgumentParserTest 27/0；`mvn clean test` 368/0/0（34 既有 skip）。

## 文档影响核对

| Plan 声明 | 实现是否一致 | 备注 |
|---|---|---|
| 开发文档 N/A | 一致 | — |
| 用户文档 N/A | 一致 | 缺陷修复，README 未承诺拒 `-` 值 |
| 运维文档 N/A | 一致 | — |

## 安全影响核对

| 检查项 | 结果 | 处置状态 | 备注 |
|---|---|---|---|
| 敏感信息 | 改善 | 已闭环 | 以 `-` 开头的密码不再被拒/回显异常；既有 `usageErrorMessageDoesNotContainPasswordValue` 仍 Pass |
| 认证与授权 | 无 | n/a | — |
| 输入与外部访问 | 无 | n/a | — |
| 依赖变更 | 无 | n/a | — |

## 必修项

| ID | 位置 | 问题 | 状态 |
|---|---|---|---|
| — | — | 无阻塞项 | — |

## 结论

Approve

## 后续动作与复审范围

- 进 QA：复跑 CliArgumentParserTest + `mvn clean test`。
- QA Fail 修复后复审；范围限 CliArgumentParser 及测试。
