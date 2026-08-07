# Dev Notes: fix-cli-credential-redaction

> Developer 验证回执。依据 [spec.md](./spec.md)、[plan.md](./plan.md)。

## 摘要

实现 `CredentialRedaction` 共享 helper；`CliSession.sanitize` 脱敏 URL userinfo 与已配置 password 字面量；`CliOptions.toString` 对 `url` 做同等 userinfo 掩码。内存中 `url`/`password` 保持明文供 JDBC 连接。

## TDD 证据

| 阶段 | 命令 | 结果 |
|---|---|---|
| T1 红 | `mvn -q test -Dtest=CredentialRedactionTest,RuntimeConfigResolverTest#cliOptionsToStringRedactsUrlUserInfo` | **3 Failures**（P0-1 `secretPass`、P0-2 `super-secret-credential`、P0-3 `bob` 仍可见） |
| T2–T4 绿 | `mvn -q clean test` | **EXIT=0**，Failures/Errors = 0 |

## 变更路径

| 路径 | 变更 |
|---|---|
| `src/main/java/com/ggtest/cli/CredentialRedaction.java` | **新建** — `redactUrlUserInfo`、`redactMessage` |
| `src/main/java/com/ggtest/cli/CliSession.java` | `sanitize` → package-private 实例方法，委托 `CredentialRedaction` |
| `src/main/java/com/ggtest/cli/CliOptions.java` | `toString()` 对 `url` 调用 `redactUrlUserInfo` |
| `src/test/java/com/ggtest/cli/CredentialRedactionTest.java` | **新建** — P0-1/2/4、P1-1 |
| `src/test/java/com/ggtest/cli/RuntimeConfigResolverTest.java` | 扩展 P0-3 `cliOptionsToStringRedactsUrlUserInfo` |

**未触碰**：`runPostgresFile` teardown、`workflow/docs/standards/code-audit-register.md`（worktree 内不存在，CA-002 由 Manager 在主仓更新）。

## 验收对照

| ID | 状态 | 证据 |
|---|---|---|
| P0-1 | Pass | `CredentialRedactionTest.sanitizeRedactsUrlUserInfo` |
| P0-2 | Pass | `CredentialRedactionTest.sanitizeRedactsConfiguredPasswordLiteral` |
| P0-3 | Pass | `RuntimeConfigResolverTest.cliOptionsToStringRedactsUrlUserInfo` |
| P0-4 | Pass | T1 红 3 fail → 实现后全绿 |
| P1-1 | Pass | `CredentialRedactionTest.sanitizePreservesPlainMessagesWithoutCredentials` |
| P1-2 | Pass | `MainOrchestrationTest.passwordIsNeverPrintedInOutput`、`EnvConfigIntegrationTest.passwordFromEnvFileNeverPrinted`、`RuntimeConfigResolverTest.cliOptionsToStringRedactsPassword` 仍绿 |

## 安全

- 测试/fixture 使用虚构凭据（`secretPass`、`super-secret-credential` 等），无真实密钥入仓。
- userinfo 占位符：`://***@`（`CredentialRedaction.URL_USERINFO` 替换组 `$1***@`）。

## 未解决风险

- 无 PG 门控环境时 PG 集成测 skip（与现有一致）；与本项 P0 无直接依赖。

## 建议后续

- **Reviewer**：检查测试有效性、占位符稳定性、文档影响。
- Review 门禁 **required**；Approve 后方可 QA。
