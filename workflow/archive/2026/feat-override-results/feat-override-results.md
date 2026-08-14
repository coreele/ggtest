# 工作项: feat-override-results

描述: 实现 `--override` 选项 — 当 query 实际输出与测试文件内嵌期望结果不一致时，用实际输出覆盖（重写）`.slt` 文件中的 expected results（golden-update 语义，**改写源文件**）。具体覆盖哪些不匹配类型由 Analyst 调研代码库后决定。
目标分支: main
源分支: feat-override-results
基线提交: a6c8719bc48099cf772a6bd1807876dd4577259c
文档影响: README.md（新增 `--override` 说明）、workflow 产物

> **本文件须保存为 `workflow/archive/2026/feat-override-results/feat-override-results.md`**，文件名与目录同名。
> 流程定义见 `workflow/WORKFLOW.md`；看板见 `workflow/STATUS.md`。
> 本工作项的全部产物平铺在 `workflow/archive/2026/feat-override-results/`，无子目录、无版本后缀。
> 表内只填枚举、短标签或路径；理由与长说明写进「进度笔记」。

## 门禁

| 路径等级 | Spec | Spec 用户确认 | Design | Review |
|---|---|---|---|---|
| standard | skipped | not-required | skipped | required |

## 状态

| 状态 | 下一步 | 阻塞原因 | 恢复条件 | 恢复后目标 |
|---|---|---|---|---|
| archived | — |  |  |  |

## 子项（仅 tracking 项填写）

| 子项 id | 状态 |
|---|---|
| — | |

## 进度笔记

- 2026-08-10 登记。新 CLI 选项 `--override`：golden-update 模式，运行中用实际输出重写源 `.slt` 文件的 expected results。
- 用户已明确语义（重写测试文件中的期望结果）；覆盖范围（仅 query result mismatch / 含 statement error 消息 / label 冲突等）由 Analyst 调研 runner 与 parser 代码后决定。
- 数据安全考量：改写源文件 → 需考虑原子写（temp + rename）/ 只改写需要变更的记录 / 保留原文件其余内容（注释、格式、statement 位置）；这些技术方案归 Design 决策。
- 2026-08-10 Spec 已确认（approved）。开放问题决议：Q1 报告用 `[OVERRIDDEN]` tag + `overridden=N` 计数（退出码不变）；Q2 v1 不提供 CI 漂移信号（override-only 无 FAILED → 0）；Q3 statement error msg override 写完整 error summary。详见 spec.md。
- 2026-08-10 Plan 已确认（approved）。Design/Plan 产出见 design.md / plan.md。
- Developer 实施完成（8 提交，321 测试全绿，+59 新测试）。详见 dev-notes.md。
- Review R1（subagent）：结论 `Approve`。全量 BUILD SUCCESS（321/0/16skip）。关键不变性全部核验通过，无 blocking。详见 review.md。
- QA（subagent）：结论 `Pass`。P0/P1 15/15 验收点全通过，无缺陷。P1-6 集成测试缺口评估为可接受低风险。详见 qa-report.md。用户已授权合并。
- 2026-08-14：按 ggnote `WORKFLOW.md` 标准迁移工作流目录（记录与产物合并为同一目录；权威文件改为 `workflow/WORKFLOW.md`）。
