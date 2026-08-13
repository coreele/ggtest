# 工作项记录: fix-ca020-main-fatal-catch

工作项标识: fix-ca020-main-fatal-catch
描述: Main 顶层兜底异常映射（CA-020）
目标分支: main

> 权威流程见 [workflow/README.md](../../README.md)；活跃状态见 [STATUS.md](STATUS.md)。

## 切片门禁

| sub-feature-id | 路径等级 | 源分支 | Spec 门禁 | Design 门禁 | Review 门禁 |
|---|---|---|---|---|---|
| fix-ca020-main-fatal-catch | fast | fix-ca020-main-fatal-catch | skipped | skipped | required |

## 切片状态

| sub-feature-id | 状态 | 后续步骤 | 阻塞原因 | 恢复条件 | 恢复后目标 |
|---|---|---|---|---|---|
| fix-ca020-main-fatal-catch | done | — | | | |

## 进度笔记

- 来源：`workflow/docs/audit/2026-08-13-src.md` Findings CA-020。
- 2026-08-13：Developer 实施完成 —— `Main.run`（7 参）追加 `catch (Throwable)` → exit 2 + `printFatalError`（`CredentialRedaction.redactUrlUserInfo` 脱敏）；新增 `unexpectedExceptionExitsTwoWithRedactedFatalSummary`（注入抛错 envLookup 经 `resolve`→`readProcessEnv` 触发）。该测试 Pass；`mvn clean test` 369/0/0（34 既有 skip）。状态 → developing，待 Reviewer。
