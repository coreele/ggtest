# 工作项记录: fix-aggfunc-total-precision

工作项标识: fix-aggfunc-total-precision
描述: `slt_lang_aggfunc.test` ~491：`total()` 大浮点期望 `-18446744073709550000.000` vs 实际 `-18446744073709552000.000`。属语料/SQLite/JDBC double 格式化漂移，与整数归一化（WI-1）独立。
路径等级: standard
源分支: fix-aggfunc-total-precision
目标分支: main
文档影响: `docs/features/fix-aggfunc-total-precision/`（预计 spec/plan 等，视漂移处理策略而定）

> 权威工作流、门禁与状态说明见 [docs/README.md](../README.md)。
> 活跃状态见 [STATUS.md](STATUS.md)。

## 切片（未拆分时仅一行，sub-feature-id = feature-id）

| sub-feature-id | Spec | Spec 门禁 | Spec 用户确认 | Design 门禁 | Review 门禁 | 状态 | 后续步骤 |
|---|---|---|---|---|---|---|---|
| fix-aggfunc-total-precision | （待 Analyst） | required（R 比对/精度容差或语料策略可能构成合同澄清） | required（存在歧义：改格式化、记 skip、还是接受引擎差异） | skipped（暂无边界决策；策略确定后重判） | required（standard） | backlog | WI-1 done；仍 backlog，待用户指示 |

阻塞原因: 用户要求暂不自动推进；保持 backlog 直至明确指示。
恢复条件: 用户明确要求推进本项。
恢复后的目标状态: `speccing`

## Manager 门禁判定（2026-08-06）

- **路径**：`standard` — 精度/容差或语料策略可能改变比对合同。
- **Spec**：`required` + **用户确认 required**。
- **Design**：暂 `skipped`。
- **Review**：`required`。
- **分支**：源 `fix-aggfunc-total-precision` → 目标 `main`。

## 用户授权记录

- 2026-08-06：用户同意登记并跟踪；实施排在 WI-1（及可能 WI-2）之后。

## 进度笔记

- 2026-08-06：Manager 登记；状态保持 **`backlog`**；不调度产出角色，直至恢复条件满足。
- 2026-08-06：WI-1 已 done；本项仍 **`backlog`**（用户要求勿自动推进）。
