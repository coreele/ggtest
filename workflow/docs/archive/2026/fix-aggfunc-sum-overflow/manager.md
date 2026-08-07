# 工作项记录: fix-aggfunc-sum-overflow

工作项标识: fix-aggfunc-sum-overflow
描述: `slt_lang_aggfunc.test` ~480/484：SQLite `integer overflow` vs ggtest `query execution failed`；语料在 TBD-EVIDENCE 下为空期望/浮点期望。曾需产品决策：语料漂移跳过、文档化 out-of-scope，或增加 query-error 处理。**已 cancelled：用户判定非 ggtest 设计缺陷，无需修复。**
路径等级: standard
源分支: fix-aggfunc-sum-overflow
目标分支: main
文档影响: 无（未产出 spec/plan；空 features 目录已随归档迁入；不改产品代码、不改语料）

> 权威工作流、门禁与状态说明见 [workflow/README.md](../../../README.md)。
> 活跃状态见 [STATUS.md](../../manager/STATUS.md)。
>
> 本文件已归档为 `workflow/docs/archive/2026/fix-aggfunc-sum-overflow/manager.md`；`workflow/docs/manager/` 仅保留活跃工作项记录与 `STATUS.md`。未产出 features 文档。

## 切片（未拆分时仅一行，sub-feature-id = feature-id）

| sub-feature-id | Spec | Spec 门禁 | Spec 用户确认 | Design 门禁 | Review 门禁 | 状态 | 后续步骤 |
|---|---|---|---|---|---|---|---|
| fix-aggfunc-sum-overflow | N/A（未编写） | required（登记时：query 错误语义 / 语料期望合同可能变更） | required（登记时存在业务歧义） | skipped（无边界决策） | required（standard） | **cancelled** | 已取消并归档；不推进 Analyst/实现 |

sub-feature-id: fix-aggfunc-sum-overflow
Spec 门禁: required（登记时判定；取消后不再执行）
Spec 用户确认: required（登记时；取消后不适用）
Design 门禁: skipped（暂无边界决策）
Review 门禁: required（standard；取消后不适用）
状态: cancelled
后续步骤: none（已 cancelled 并归档）
阻塞原因: none（已取消）
恢复条件: none（用户明确取消 / wontfix，不恢复）
恢复后的目标状态: N/A

## Manager 门禁判定（2026-08-06）

- **路径**：`standard` — 可能改变失败分类或语料处理策略，非单点确定修复。
- **Spec**：`required` + **用户确认 required** — 产品/合同歧义。
- **Design**：暂 `skipped`；若实现需新错误通道再重判。
- **Review**：`required`。
- **分支**：源 `fix-aggfunc-sum-overflow` → 目标 `main`（实施前已预填；实际未 checkout 实施）。

## 用户授权记录

- 2026-08-06：用户同意登记并跟踪；**延后设计/实施**至 WI-1 之后。
- 2026-08-06：用户明确授权将本项标为 **cancelled** 并完成 Manager 关闭/归档。决策依据：不属于 ggtest 本身设计问题，无需修复；性质为 SQLite `sum()` 整数溢出语义 vs JDBC/引擎错误形态差异，非 harness 设计缺陷。

## 进度笔记

- 2026-08-06：Manager 登记；状态保持 **`backlog`**；不调度 Analyst/Planner，直至恢复条件满足。
- 2026-08-06：WI-1 `fix-normalize-integer-float` 已 `done` 并合入；本项仍 **`backlog`**（用户要求勿自动推进）。
- 2026-08-06：用户确认 cancelled；状态 `backlog` → **`cancelled`**；空 `workflow/docs/features/fix-aggfunc-sum-overflow/` 与本工作项记录一并归档至 `workflow/docs/archive/2026/fix-aggfunc-sum-overflow/`；不改产品代码、不改 `slt_lang_aggfunc.test`；按用户指示不主动 commit。
