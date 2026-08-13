# 工作项记录: fix-ca023-prepare-conn-leak

工作项标识: fix-ca023-prepare-conn-leak
描述: FileRunner prepare 失败连接泄漏修复（CA-023，原 xugu 审计 CA-017）
目标分支: main

> 权威流程见 [workflow/README.md](../../README.md)；活跃状态见 [STATUS.md](STATUS.md)。

## 切片门禁

| sub-feature-id | 路径等级 | 源分支 | Spec 门禁 | Design 门禁 | Review 门禁 |
|---|---|---|---|---|---|
| fix-ca023-prepare-conn-leak | fast | fix-ca023-prepare-conn-leak | skipped | skipped | required |

> 来源 2026-08-12 xugu 审计 Findings Low §5（原记 CA-017），因与 2026-08-13 源码审计 CA-017 撞号，登记册改编号 CA-023。单点资源泄漏修复，故 fast + Spec/Design skipped；与 CA-015~020 一致取 Review=required。本切片在 main 上修 PG/MySQL 模式；xgtest 分支的 Xugu 同款模式在下次 rebase 后即共享同一 FileRunner 实现。

## 切片状态

| sub-feature-id | 状态 | 后续步骤 | 阻塞原因 | 恢复条件 | 恢复后目标 |
|---|---|---|---|---|---|
| fix-ca023-prepare-conn-leak | developing | Reviewer | | | |

## 进度笔记

- 来源：`workflow/docs/audit/2026-08-12-xugu-engine.md` Findings（登记册 CA-023）。
- 2026-08-13：Developer 实施完成 —— prepare 失败 catch 内 return 前显式 `first.close()`。`mvn clean test` 369/0/0（34 既有 skip）。状态 → developing，待 Reviewer。
