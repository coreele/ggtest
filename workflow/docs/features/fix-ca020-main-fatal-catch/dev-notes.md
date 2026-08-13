# Dev Notes: fix-ca020-main-fatal-catch

## 实现摘要

- 分支：`fix-ca020-main-fatal-catch` ← `main`。
- 修复 CA-020：`Main.run`（7 参，真正实现）顶层仅 `catch (UsageException)`；非 UsageException 异常会逃逸 → JVM 未捕获退出，退出码不受控。补 `catch (Throwable)` → exit 2 + 简短脱敏摘要。

## 变更

`cli/Main.java`：
- `run(...)` 的 try/catch 末尾追加：
  ```java
  } catch (Throwable t) {
      printFatalError(err, t);
      return 2;
  }
  ```
- 新增 `printFatalError(PrintStream err, Throwable t)`：与 `printUsageError` 同风格（`Error: fatal` + `    [WHY] <简单类名[: 消息]>`），消息经 `CredentialRedaction.redactUrlUserInfo` 脱敏。

## 决策

- 选 `Throwable`（非 `Exception`）：CLI 顶层 best-effort 兜底，含 Error；JVM 即将 `System.exit`，不特殊处理 InterruptedException。
- 脱敏只用 `redactUrlUserInfo`（覆盖 `://user:pass@`）。configured password 此刻未必可得（parse 可能已抛，parsed 不可靠）；查询参数凭据缺口属 CA-009（独立 open 项），本切片不扩面。
- 既有 `catch (UsageException)` 在前，更具体；`printUsageError` 路径不变。

## 测试

新增 `MainOrchestrationTest.unexpectedExceptionExitsTwoWithRedactedFatalSummary`：经 7 参 `run` 重载注入「任意 key 抛 RuntimeException（消息含 `jdbc:postgresql://alice:hunter2@host:5432/db`）」的 envLookup → `resolve`→`readProcessEnv(envLookup)` 直接 apply（无 catch）→ 抛出 → 落入新 `catch (Throwable)`。
- 断言 exit=2；stderr 含 `Error: fatal` + `RuntimeException`；不含 `alice:hunter2` / `hunter2`。

触发路径确定性：`readProcessEnv`（RuntimeConfigResolver.java:150）`envLookup.apply(key)` 裸调用；`resolve` 在 `TestFileCollector.collect`（输入校验）之前，故 `"a.test"` 是否存在不影响。

## 变更路径

| 任务 | 路径 |
|---|---|
| 兜底 catch + printFatalError | `src/main/java/com/ggtest/cli/Main.java` |
| 测试 | `src/test/java/com/ggtest/cli/MainOrchestrationTest.java` |

## 验证

| 命令 | 结果 |
|---|---|
| `mvn -Dtest=MainOrchestrationTest#unexpectedExceptionExitsTwoWithRedactedFatalSummary test` | Tests=1 Failures=0；BUILD SUCCESS |
| `mvn clean test` | Tests=**369** Failures=0 Errors=0 Skipped=34；BUILD SUCCESS |

既有 UsageException 路径（各 usage/missing 用例）不回归。

## 文档影响

| 类别 | 已更新路径或交接说明 |
|---|---|
| 开发文档 | N/A（类 Javadoc 已声明 exit 2 覆盖 fatal；实现补齐） |
| 用户文档 | N/A |
| 运维文档 | N/A |

## 未解决风险 / 验证缺口

| 项 | 原因 | 风险 | 恢复条件 |
|---|---|---|---|
| N/A | 经注入 envLookup 确定性覆盖 catch 与脱敏 | — | — |

## QA 修复回执

| 缺陷 ID | 处理 | 摘要 | 验证 | 建议复测 |
|---|---|---|---|---|
| — | N/A | 本轮无 QA Fail | — | — |
