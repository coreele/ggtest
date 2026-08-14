# 工作项: fix-ca020-main-fatal-catch

描述: Main 顶层兜底异常映射（CA-020）
目标分支: main
源分支: fix-ca020-main-fatal-catch
基线提交: a6c8719bc48099cf772a6bd1807876dd4577259c
文档影响: 

> **本文件须保存为 `workflow/archive/2026/fix-ca020-main-fatal-catch/fix-ca020-main-fatal-catch.md`**，文件名与目录同名。
> 流程定义见 `workflow/WORKFLOW.md`；看板见 `workflow/STATUS.md`。
> 本工作项的全部产物平铺在 `workflow/archive/2026/fix-ca020-main-fatal-catch/`，无子目录、无版本后缀。
> 表内只填枚举、短标签或路径；理由与长说明写进「进度笔记」。

## 门禁

| 路径等级 | Spec | Spec 用户确认 | Design | Review |
|---|---|---|---|---|
| fast | skipped | not-required | skipped | required |

## 状态

| 状态 | 下一步 | 阻塞原因 | 恢复条件 | 恢复后目标 |
|---|---|---|---|---|
| archived | — |  |  |  |

## 子项（仅 tracking 项填写）

| 子项 id | 状态 |
|---|---|
| — | |

## 进度笔记

- 来源：`workflow/audit/2026-08-13-src.md` Findings CA-020。
- 2026-08-13：Developer 实施完成 —— `Main.run`（7 参）追加 `catch (Throwable)` → exit 2 + `printFatalError`（`CredentialRedaction.redactUrlUserInfo` 脱敏）；新增 `unexpectedExceptionExitsTwoWithRedactedFatalSummary`（注入抛错 envLookup 经 `resolve`→`readProcessEnv` 触发）。该测试 Pass；`mvn clean test` 369/0/0（34 既有 skip）。状态 → developing，待 Reviewer。
- 2026-08-14：按 ggnote `WORKFLOW.md` 标准迁移工作流目录（记录与产物合并为同一目录；权威文件改为 `workflow/WORKFLOW.md`）。
