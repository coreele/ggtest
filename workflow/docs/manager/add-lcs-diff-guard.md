# 工作项记录: add-lcs-diff-guard

工作项标识: add-lcs-diff-guard
描述: LCS diff O(n*m) 加大小门限 — 大结果集失败时兜底
路径等级: fast
源分支: add-lcs-diff-guard
目标分支: main
文档影响: N/A

> 权威流程见 [workflow/README.md](../../README.md)；活跃状态见 [STATUS.md](STATUS.md)。

## 切片门禁（未拆分）

| sub-feature-id | 路径等级 | 源分支 | Spec | Spec 门禁 | Spec 用户确认 | Design 门禁 | Review 门禁 |
|---|---|---|---|---|---|---|---|
| add-lcs-diff-guard | fast | add-lcs-diff-guard | 无 | skipped | not-required | skipped | skipped |

## 切片状态

| sub-feature-id | 状态 | 后续步骤 |
|---|---|---|
| add-lcs-diff-guard | backlog | Planner |

## 进度笔记

- 2026-08-07 登记。P1：已知项 CA-007（accepted）。`ResultComparer.diffOps()` 分配 `int[n+1][m+1]`，大语料（select4 1.2MB）可能内存膨胀。加门限：`n*m > 10_000` 时截断/降级。
