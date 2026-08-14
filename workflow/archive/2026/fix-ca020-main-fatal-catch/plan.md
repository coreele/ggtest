# Plan: fix-ca020-main-fatal-catch

## 元信息

- 工作项标识: fix-ca020-main-fatal-catch（未拆分）
- 路径等级: fast | Review 门禁: required
- 来源: `workflow/audit/2026-08-13-src.md` Findings CA-020（Medium §2/§9）
- 验证命令: `mvn -Dtest=MainOrchestrationTest#unexpectedExceptionExitsTwoWithRedactedFatalSummary test`、`mvn -q clean test`
- 预期证据: 新增测试 Pass；`mvn clean test` 0 failures

## 目标摘要

修复 CA-020：`Main.run(...)`（7 参，真正实现）顶层仅 `catch (UsageException → exit 2)`；任何非 `UsageException` 的异常（逃逸的 NPE / 未预期 RuntimeException）会传播出 `run`，在 `main` 以未捕获异常退出，退出码不受控。类 Javadoc 承诺 exit 2 覆盖「connection / fatal errors」。补顶层兜底。

## Bug 定位

`cli/Main.java:107-122`：try 块后仅 `catch (UsageException)`。审计注：正常错误路径已在 CliSession/FileRunner 内映射为 FileOutcome，故实际触发概率低；但 Javadoc 承诺与实现不符。

## 修复方案

在 `catch (UsageException)` 之后追加 `catch (Throwable t)`：
- 映射稳定退出码 2；
- 调用新增 `printFatalError(err, t)` 打印简短摘要（与 `printUsageError` 同风格：`Error: fatal` + `[WHY] <类型: 消息>`）；
- 消息经 `CredentialRedaction.redactUrlUserInfo` 脱敏（覆盖 `://user:pass@` 形式；查询参数凭据缺口属 CA-009，独立项）。

选 `Throwable`（含 Error）：CLI 顶层兜底，best-effort 打印 + 稳定退出码；JVM 即将 System.exit，InterruptedException 等不特别处理。

## 测试方案

新增 `MainOrchestrationTest.unexpectedExceptionExitsTwoWithRedactedFatalSummary`：注入对任意 key 抛 `RuntimeException`（消息含 `jdbc:postgresql://alice:hunter2@host/db`）的 `envLookup`，经 7 参 `run` 重载触发（`resolve`→`readProcessEnv(envLookup)` 直接 apply，无 catch）。断言：exit=2；stderr 含 `Error: fatal` + `RuntimeException`；不含 `alice:hunter2` / `hunter2`（脱敏）。

## 触碰路径

- `src/main/java/com/ggtest/cli/Main.java`
- `src/test/java/com/ggtest/cli/MainOrchestrationTest.java`

## 验收与验证

| ID | 要求 | 预期证据 |
|---|---|---|
| V1 | 非 UsageException 异常 → exit 2 | 新增测试 exit=2 |
| V2 | stderr 含 fatal 摘要 | 含 `Error: fatal` + 类型名 |
| V3 | 摘要经脱敏（URL userinfo） | 不含 `alice:hunter2` / `hunter2` |
| V4 | 既有 UsageException 路径不回归 | 既有 usage 用例仍 exit 2 |
| V5 | `mvn clean test` | BUILD SUCCESS，0 failures |

## 文档影响

开发/用户/运维均 N/A（类 Javadoc 已声称 exit 2 覆盖 fatal；实现补齐，无需改文档）。

## 交接顺序

Developer → Reviewer（required）→ QA → 合并授权 → done → 合入 main。

## 修订记录

| 日期 | 摘要 |
|---|---|
| 2026-08-13 | 初版 Plan（来源：2026-08-13 审计 CA-020） |
