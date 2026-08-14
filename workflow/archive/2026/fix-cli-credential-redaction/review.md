# Review: fix-cli-credential-redaction

## 审阅范围

| 项 | 值 |
|---|---|
| 工作项 | `fix-cli-credential-redaction`（standard；Review **required**） |
| 实现版本 | 分支 `fix-cli-credential-redaction` @ `1ea25fc` |
| 审阅依据 | [spec.md](./spec.md)、[plan.md](./plan.md)、[dev-notes.md](./dev-notes.md)；`workflow/agents/standards/quality.md`、`security.md`、`documentation.md`、`git.md` |
| 代码范围 | `CredentialRedaction.java`（新建）、`CliSession.sanitize`、`CliOptions.toString`、`CredentialRedactionTest.java`、`RuntimeConfigResolverTest.cliOptionsToStringRedactsUrlUserInfo` |
| 独立验证 | `mvn -q clean test`（EXIT=0）；strip-only 模拟红态 3 Failures（P0-1/P0-2/P0-3 相关） |

## 实现正确性

**结论：满足 Spec P0/P1 与 Plan T2–T4。**

- **`CredentialRedaction`**：package-private 共享 helper；`redactUrlUserInfo` 以 `://***@` 稳定掩码 userinfo；`redactMessage` 保留 null→`""`、strip、URL 脱敏，且在 `options.password()` 有非空值时做字面量替换。
- **`CliSession.sanitize`**：由 strip-only 改为实例方法，委托 `CredentialRedaction.redactMessage(message, options.password())`；所有既有调用点（连接/解析/IO/PG teardown 等错误路径）经同一控制点，符合 spec 合同。
- **`CliOptions.toString`**：`url` 经同一 `redactUrlUserInfo`；`password` 仍为 `***`；无 userinfo 的 URL（如 `jdbc:sqlite::memory:`）保持可读。
- **范围**：单 commit 仅改 Plan 声明路径；未动 `runPostgresFile` teardown、JDBC/parser/runner 语义。
- **约束**：脱敏为纯字符串变换，不抛异常；内存中 `url`/`password` 仍为明文供连接。

| 验收 ID | 结果 | 证据 |
|---|---|---|
| P0-1 | Pass | `CredentialRedactionTest.sanitizeRedactsUrlUserInfo` |
| P0-2 | Pass | `CredentialRedactionTest.sanitizeRedactsConfiguredPasswordLiteral` |
| P0-3 | Pass | `RuntimeConfigResolverTest.cliOptionsToStringRedactsUrlUserInfo` |
| P0-4 | Pass | 独立 strip-only 模拟：6 tests / 3 Failures；修复后全绿 |
| P1-1 | Pass | `sanitizePreservesPlainMessagesWithoutCredentials`、`sanitizeNullBecomesEmpty`、`sanitizeStripsLeadingAndTrailingWhitespace` |
| P1-2 | Pass | `MainOrchestrationTest.passwordIsNeverPrintedInOutput`、`EnvConfigIntegrationTest.passwordFromEnvFileNeverPrinted`、`cliOptionsToStringRedactsPassword` 仍绿 |

## 测试有效性

**结论：有效；非恒真；红/绿证明成立。**

- P0 用例断言明文凭据**不出现**且非凭据片段（host、错误前缀）**仍可见**，能因 strip-only / 原样 url 实现而失败。
- Reviewer 独立复现：在 package-private + strip-only + 原样 `toString` 下，`CredentialRedactionTest` 2 Failures + `cliOptionsToStringRedactsUrlUserInfo` 1 Failure，与 dev-notes T1 记录一致。
- 测试触达方式符合 Plan：`sanitize` 为 package-private 实例方法，无 public 测试 API 泄露。

## 文档影响核对

| Plan 声明 | 实现是否一致 | 备注 |
|---|---|---|
| 开发文档 — `dev-notes.md` | 是 | TDD 红/绿、`mvn` 摘要、变更路径、验收对照齐全 |
| 开发文档 — `code-audit-register.md` CA-002 → `resolved` | **部分** | worktree 无该文件；dev-notes 注明由 Manager 在主仓更新；主仓登记册仍为 `open` |
| 用户文档 | N/A | CLI 参数/退出码/`.env` 键名不变 |
| 运维文档 | N/A | 无部署/排障变更 |

CA-002 登记册更新属 Plan T5 行政项，不阻塞本项代码与安全合同；**Manager 须在 QA 前或 `done` 时于主仓标记 `resolved`**。

## 安全影响核对

| 检查项 | 结果 | 备注 |
|---|---|---|
| 敏感信息（security.md §1） | Pass | 测试/fixture 为虚构凭据；commit diff 无真实密钥或 `.env` |
| 敏感数据 — 诊断输出脱敏 | Pass | 两处控制点（sanitize、toString）已落地；占位符 `://***@` 稳定 |
| 认证与授权 | N/A | 无认证/授权变更 |
| 输入与外部访问 | N/A | 无新增外部网络或文件上传面 |
| 依赖变更 | N/A | 无 pom/依赖变动 |

## 发现项

| 严重度 | ID | 位置 | 问题 | 状态 |
|---|---|---|---|---|
| — | — | — | 无阻塞项 | — |
| Info | R-1 | `workflow/audit/register.md` | CA-002 仍为 `open`；实现已在 `1ea25fc` | 待 Manager 更新 |
| Info | R-2 | `CredentialRedaction.java` | 正则要求 `scheme://` 前缀才匹配 userinfo；非标准无 scheme 形式不在 spec 范围 | 可接受 |

## 结论

**Approve**

实现满足 Spec 合同与 Plan 任务；测试覆盖 P0/P1 且红/绿证明可复现；安全输出控制点有效；无回归。可进入 QA。

## 后续动作

| 角色 | 动作 |
|---|---|
| **Manager** | 主仓 CA-002 → `resolved`；调度 QA |
| **QA** | 依据 spec P0/P1 与 Plan 独立验收 → `qa-report.md` |
| **Developer** | 无必修项 |

## 复审范围

本次为初审；若 QA/Developer 有修复，复审范围限于脱敏 helper、`CliSession.sanitize`、`CliOptions.toString` 及相关测试。
