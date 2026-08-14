# 工作项: fix-override-rowwise

描述: --override 对 row-wise（separator=<delim>）查询写入 flat value-per-line 格式而非 separator-joined rows；同时改善 separator 拼写错误提示
目标分支: main
源分支: fix-override-rowwise
基线提交: a6c8719bc48099cf772a6bd1807876dd4577259c
文档影响: N/A

> **本文件须保存为 `workflow/archive/2026/fix-override-rowwise/fix-override-rowwise.md`**，文件名与目录同名。
> 流程定义见 `workflow/WORKFLOW.md`；看板见 `workflow/STATUS.md`。
> 本工作项的全部产物平铺在 `workflow/archive/2026/fix-override-rowwise/`，无子目录、无版本后缀。
> 表内只填枚举、短标签或路径；理由与长说明写进「进度笔记」。

## 门禁

| 路径等级 | Spec | Spec 用户确认 | Design | Review |
|---|---|---|---|---|
| fast | skipped | not-required | skipped | skipped |

## 状态

| 状态 | 下一步 | 阻塞原因 | 恢复条件 | 恢复后目标 |
|---|---|---|---|---|
| archived | — |  |  |  |

## 子项（仅 tracking 项填写）

| 子项 id | 状态 |
|---|---|
| — | |

## 进度笔记

- 根因：`SqlLogicTestRunner.runQuery()` 中 override text 直接 `String.join("\n", actualView)` 拼接 flat values，未按 columnSeparator 重整为 row-wise 格式
- 修复：新增 `formatOverrideText()` 按 `typeSignature.size()` 分组，`columnSeparator` 拼接
- hash 保护：`actualView.size() == 1` 时跳过重整（单行 hash）
- misspelling：新增 `editDistance` ≤2 的模糊匹配，`seperator` → "did you mean separator?"
- 2026-08-14：按 ggnote `WORKFLOW.md` 标准迁移工作流目录（记录与产物合并为同一目录；权威文件改为 `workflow/WORKFLOW.md`）。
