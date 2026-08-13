# 工作项记录: fix-ca019-cli-dash-values

工作项标识: fix-ca019-cli-dash-values
描述: CLI 允许以 `-` 开头的值（CA-019）
目标分支: main

> 权威流程见 [workflow/README.md](../../README.md)；活跃状态见 [STATUS.md](STATUS.md)。

## 切片门禁

| sub-feature-id | 路径等级 | 源分支 | Spec 门禁 | Design 门禁 | Review 门禁 |
|---|---|---|---|---|---|
| fix-ca019-cli-dash-values | fast | fix-ca019-cli-dash-values | skipped | skipped | required |

## 切片状态

| sub-feature-id | 状态 | 后续步骤 | 阻塞原因 | 恢复条件 | 恢复后目标 |
|---|---|---|---|---|---|
| fix-ca019-cli-dash-values | done | — | | | |

## 进度笔记

- 来源：`workflow/docs/audit/2026-08-13-src.md` Findings CA-019。
- 2026-08-13：Developer 实施完成 —— `requireValue` 改为仅当下一 token ∈ OPTION_FLAGS（或越界）时报缺值；新增 3 测试（`-secret` 密码、`-name/-path` 值、`--password --user` 缺值）。CliArgumentParserTest 27/0；`mvn clean test` 368/0/0（34 既有 skip）。状态 → developing，待 Reviewer。
