# 工作项: fix-ca023-prepare-conn-leak

描述: FileRunner prepare 失败连接泄漏修复（CA-023，原 xugu 审计 CA-017）
目标分支: main
源分支: fix-ca023-prepare-conn-leak
基线提交: a6c8719bc48099cf772a6bd1807876dd4577259c
文档影响: 

> **本文件须保存为 `workflow/archive/2026/fix-ca023-prepare-conn-leak/fix-ca023-prepare-conn-leak.md`**，文件名与目录同名。
> 流程定义见 `workflow/WORKFLOW.md`；看板见 `workflow/STATUS.md`。
> 本工作项的全部产物平铺在 `workflow/archive/2026/fix-ca023-prepare-conn-leak/`，无子目录、无版本后缀。
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

- 来源：`workflow/audit/2026-08-12-xugu-engine.md` Findings（登记册 CA-023）。
- 2026-08-13：Developer 实施完成 —— prepare 失败 catch 内 return 前显式 `first.close()`。`mvn clean test` 369/0/0（34 既有 skip）。状态 → developing，待 Reviewer。
- 2026-08-14：按 ggnote `WORKFLOW.md` 标准迁移工作流目录（记录与产物合并为同一目录；权威文件改为 `workflow/WORKFLOW.md`）。
