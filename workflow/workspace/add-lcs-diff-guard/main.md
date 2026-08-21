# 工作项: add-lcs-diff-guard

描述: LCS diff O(n*m) 加大小门限 — 大结果集失败时兜底
目标分支: main
源分支: add-lcs-diff-guard
基线提交: a6c8719bc48099cf772a6bd1807876dd4577259c
文档影响: N/A

> **本文件须保存为 `workflow/workspace/add-lcs-diff-guard/main.md`**。
> 流程定义见 `workflow/WORKFLOW.md`；看板见 `workflow/STATUS.md`。
> 本工作项的全部产物平铺在 `workflow/workspace/add-lcs-diff-guard/`，无子目录、无版本后缀。
> 表内只填枚举、短标签或路径；理由与长说明写进「进度笔记」。

## 门禁

| 路径等级 | Spec | Spec 用户确认 | Design | Review |
|---|---|---|---|---|
| fast | skipped | not-required | skipped | skipped |

## 状态

| 状态 | 下一步 | 阻塞原因 | 恢复条件 | 恢复后目标 |
|---|---|---|---|---|
| backlog | Planner |  |  |  |

## 进度笔记

- 2026-08-07 登记。P1：已知项 CA-007（accepted）。`ResultComparer.diffOps()` 分配 `int[n+1][m+1]`，大语料（select4 1.2MB）可能内存膨胀。加门限：`n*m > 10_000` 时截断/降级。
- 2026-08-14：按 ggnote `WORKFLOW.md` 标准迁移工作流目录（记录与产物合并为同一目录；权威文件改为 `workflow/WORKFLOW.md`）。
