# 工作项: improve-failure-output-format

描述: 调整失败用例 CLI 报告输出形式——移除 `[WHY]`/`[SQL]`/`[Diff]` 标签；`at file:line : reason` 放在4空格缩进独立行；diff 区域8空格缩进；多失败块间无空行；无 diff 时不显示 diff 块
目标分支: main
源分支: improve-failure-output-format
基线提交: a6c8719bc48099cf772a6bd1807876dd4577259c
文档影响: README.md / README.zh-CN.md（报告示例需同步更新）

> **本文件须保存为 `workflow/archive/2026/improve-failure-output-format/improve-failure-output-format.md`**，文件名与目录同名。
> 流程定义见 `workflow/WORKFLOW.md`；看板见 `workflow/STATUS.md`。
> 本工作项的全部产物平铺在 `workflow/archive/2026/improve-failure-output-format/`，无子目录、无版本后缀。
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

- 2026-08-07 登记，fast 路径，跳过 Spec/Design/Review。
- 2026-08-07 Developer 实施完成：ReportWriter.java、CliSession.java、FileRunner.java 及4个测试文件更新，251测试通过。
- 2026-08-07 QA 首测 Pass（P0-1~P0-6 全部通过，0 缺陷）。
- 2026-08-07 Plan 确认 + 合并授权，Manager 置 done。
- 2026-08-14：按 ggnote `WORKFLOW.md` 标准迁移工作流目录（记录与产物合并为同一目录；权威文件改为 `workflow/WORKFLOW.md`）。
