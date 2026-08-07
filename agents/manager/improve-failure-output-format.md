# 工作项记录: improve-failure-output-format

工作项标识: improve-failure-output-format
描述: 调整失败用例 CLI 报告输出形式——移除 `[WHY]`/`[SQL]`/`[Diff]` 标签；`at file:line : reason` 放在4空格缩进独立行；diff 区域8空格缩进；多失败块间无空行；无 diff 时不显示 diff 块
路径等级: fast
源分支: improve-failure-output-format
目标分支: main
文档影响: README.md / README.zh-CN.md（报告示例需同步更新）

> 权威工作流、门禁与状态说明见 [agents/README.md](../README.md)。
> 活跃状态见 [STATUS.md](STATUS.md)。
>
> 归档后本文件迁至 `agents/archive/YYYY/improve-failure-output-format/manager.md`。

## 切片（未拆分，sub-feature-id = feature-id）

| sub-feature-id | Spec | Spec 门禁 | Spec 用户确认 | Design 门禁 | Review 门禁 | 状态 | 后续步骤 |
|---|---|---|---|---|---|---|---|
| improve-failure-output-format | 无 | skipped（目标格式由用户直接指定） | not-required | skipped | skipped（fast 路径） | done | 合并后建议归档 |

阻塞原因:
恢复条件:
恢复后的目标状态:

## 进度笔记

- 2026-08-07 登记，fast 路径，跳过 Spec/Design/Review。
- 2026-08-07 Developer 实施完成：ReportWriter.java、CliSession.java、FileRunner.java 及4个测试文件更新，251测试通过。
- 2026-08-07 QA 首测 Pass（P0-1~P0-6 全部通过，0 缺陷）。
- 2026-08-07 Plan 确认 + 合并授权，Manager 置 done。
