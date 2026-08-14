# 工作项记录: fix-override-direct-write

工作项标识: fix-override-direct-write
描述: 修复 `--override` 行为：① 无需比较原期望是否 pass，直接以实际结果覆盖（即使结果未变也标 OVERRIDDEN 而非 PASS）；② `--separator` 优先级高于 query header 已声明的 separator，覆盖写 .slt 中的分隔符。
目标分支: main
文档影响: README（`--override`/`--separator` 行为微调说明）。

> 权威流程见 [workflow/README.md](../../README.md)；活跃状态见 [STATUS.md](STATUS.md)。

## 门禁

| 路径等级 | 源分支 | Spec 门禁 | Design 门禁 | Review 门禁 |
|---|---|---|---|---|
| fast | fix-override-direct-write | skipped | skipped | required |

> 对既有 `--override`/`--separator` 的两处行为修正，范围明确单点，故 fast + Spec/Design skipped；与 CA 修复一致取 Review=required。

## 状态

| 状态 | 后续步骤 | 阻塞原因 | 恢复条件 | 恢复后目标 |
|---|---|---|---|---|
| done | — | | | |

## 进度笔记

- 2026-08-13：登记。来源：用户对 enhance-override 结果的两点修正：① `--override` 直接覆盖（不比较 pass/fail，全标 OVERRIDDEN）；② `--separator` 覆盖 query header 中已声明的 separator。
