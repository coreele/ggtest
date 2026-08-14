# 工作项: fix-override-direct-write

描述: 修复 `--override` 行为：① 无需比较原期望是否 pass，直接以实际结果覆盖（即使结果未变也标 OVERRIDDEN 而非 PASS）；② `--separator` 优先级高于 query header 已声明的 separator，覆盖写 .slt 中的分隔符。
目标分支: main
源分支: fix-override-direct-write
基线提交: a6c8719bc48099cf772a6bd1807876dd4577259c
文档影响: README（`--override`/`--separator` 行为微调说明）。

> **本文件须保存为 `workflow/archive/2026/fix-override-direct-write/fix-override-direct-write.md`**，文件名与目录同名。
> 流程定义见 `workflow/WORKFLOW.md`；看板见 `workflow/STATUS.md`。
> 本工作项的全部产物平铺在 `workflow/archive/2026/fix-override-direct-write/`，无子目录、无版本后缀。
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

- 2026-08-13：登记。来源：用户对 enhance-override 结果的两点修正：① `--override` 直接覆盖（不比较 pass/fail，全标 OVERRIDDEN）；② `--separator` 覆盖 query header 中已声明的 separator。
- 2026-08-14：按 ggnote `WORKFLOW.md` 标准迁移工作流目录（记录与产物合并为同一目录；权威文件改为 `workflow/WORKFLOW.md`）。
