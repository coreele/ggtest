# 工作项记录: fix-aggfunc-sum-overflow

工作项标识: fix-aggfunc-sum-overflow
描述: `slt_lang_aggfunc.test` ~480/484：SQLite `integer overflow` vs ggtest `query execution failed`；语料在 TBD-EVIDENCE 下为空期望/浮点期望。需产品决策：语料漂移跳过、文档化 out-of-scope，或增加 query-error 处理。**禁止在无 Spec/门禁下静默改合同**。
路径等级: standard
源分支: fix-aggfunc-sum-overflow
目标分支: main
文档影响: `docs/features/fix-aggfunc-sum-overflow/`（预计 spec/plan 等）；可能触及错误分类或语料策略文档

> 权威工作流、门禁与状态说明见 [docs/README.md](../README.md)。
> 活跃状态见 [STATUS.md](STATUS.md)。

## 切片（未拆分时仅一行，sub-feature-id = feature-id）

| sub-feature-id | Spec | Spec 门禁 | Spec 用户确认 | Design 门禁 | Review 门禁 | 状态 | 后续步骤 |
|---|---|---|---|---|---|---|---|
| fix-aggfunc-sum-overflow | （待 Analyst） | required（query 错误语义 / 语料期望合同可能变更；存在业务歧义） | required（溢出：失败形态、是否 skip、是否对接官方 TBD 需用户拍板） | skipped（暂无边界决策；若 Spec 要求新错误通道再升为 required） | required（standard） | backlog | 恢复条件已满足（WI-1 done）；仍 backlog，待用户指示再调度 Analyst |

阻塞原因: 用户要求暂不自动推进；保持 backlog 直至明确指示。
恢复条件: 用户明确要求推进本项（WI-1 已 done）。
恢复后的目标状态: `speccing`

## Manager 门禁判定（2026-08-06）

- **路径**：`standard` — 可能改变失败分类或语料处理策略，非单点确定修复。
- **Spec**：`required` + **用户确认 required** — 产品/合同歧义。
- **Design**：暂 `skipped`；若实现需新错误通道再重判。
- **Review**：`required`。
- **分支**：源 `fix-aggfunc-sum-overflow` → 目标 `main`（实施前已预填；实际 checkout 在 Developer 调度时）。

## 用户授权记录

- 2026-08-06：用户同意登记并跟踪；**延后设计/实施**至 WI-1 之后。

## 进度笔记

- 2026-08-06：Manager 登记；状态保持 **`backlog`**；不调度 Analyst/Planner，直至恢复条件满足。
- 2026-08-06：WI-1 `fix-normalize-integer-float` 已 `done` 并合入；本项仍 **`backlog`**（用户要求勿自动推进）。
