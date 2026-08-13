# Review: fix-ca020-main-fatal-catch

## 审阅范围

- 实现版本 / 提交: `0d75628`（源分支 `fix-ca020-main-fatal-catch`）
- 依据: [plan.md](plan.md)；fast 路径

## 实现正确性

- `run`（7 参）的 `catch (UsageException)` 之后追加 `catch (Throwable t) → printFatalError + return 2`，置于最末（最宽），`UsageException` 仍优先匹配。任何逃逸异常/错误现映射到稳定 exit 2，符合类 Javadoc「exit 2 覆盖 fatal」。✓
- `printFatalError` 风格与 `printUsageError` 一致（`Error: fatal` + `    [WHY] …`）；消息 `null`/空白时退化为简单类名，稳健。✓
- 脱敏：`CredentialRedaction.redactUrlUserInfo` 覆盖 `://user:pass@`；configured password 此刻不可靠（parse 可能已抛）故未用 `redactMessage`——合理保守；查询参数凭据缺口属 CA-009 独立项，本切片不扩面。✓
- 范围守纪律：仅 Main.java + 其测试。

## 测试有效性

- 新增 `unexpectedExceptionExitsTwoWithRedactedFatalSummary`：注入抛 RuntimeException（消息含 `jdbc:postgresql://alice:hunter2@host:5432/db`）的 envLookup，经 7 参 `run` 重载触发。触发路径确定（`resolve`→`readProcessEnv` 裸 `envLookup.apply`，且在 `collect` 输入校验之前）。✓
- 断言：exit=2；stderr 含 `Error: fatal`+`RuntimeException`；不含 `alice:hunter2`/`hunter2`（URL userinfo 脱敏生效）。✓
- 既有 UsageException 用例不回归。验证：目标测试 Pass；`mvn clean test` 369/0/0（34 既有 skip）。

## 文档影响核对

| Plan 声明 | 实现是否一致 | 备注 |
|---|---|---|
| 开发文档 N/A | 一致 | 类 Javadoc 已声明 exit 2 覆盖 fatal；实现补齐 |
| 用户文档 N/A | 一致 | — |
| 运维文档 N/A | 一致 | — |

## 安全影响核对

| 检查项 | 结果 | 处置状态 | 备注 |
|---|---|---|---|
| 敏感信息 | 改善 | 已闭环 | 异常消息经 URL userinfo 脱敏后输出；查询参数凭据缺口属 CA-009 |
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

- 进 QA：复跑目标测试 + `mvn clean test`。
- QA Fail 修复后复审；范围限 Main.java 及测试。
