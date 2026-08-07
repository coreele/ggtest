# 工作项记录: fix-aggfunc-total-precision

工作项标识: fix-aggfunc-total-precision
描述: `slt_lang_aggfunc.test` ~491：`total()` 大浮点期望 `-18446744073709550000.000` vs 实际 `-18446744073709552000.000`。属语料/SQLite/JDBC double 格式化漂移，与整数归一化（WI-1）独立。**已 cancelled / wontfix：已知限制，不专门对齐原版。**
路径等级: standard
源分支: fix-aggfunc-total-precision
目标分支: main
文档影响: 无（未产出 spec/plan；空 features 目录已随归档迁入；不移植 SQLite float printf，不改语料）

> 权威工作流、门禁与状态说明见 [agents/README.md](../../README.md)。
> 活跃状态见 [STATUS.md](../../manager/STATUS.md)。
>
> 本文件已归档为 `agents/archive/2026/fix-aggfunc-total-precision/manager.md`；`agents/manager/` 仅保留活跃工作项记录与 `STATUS.md`。未产出 features 文档。

## 切片（未拆分时仅一行，sub-feature-id = feature-id）

| sub-feature-id | Spec | Spec 门禁 | Spec 用户确认 | Design 门禁 | Review 门禁 | 状态 | 后续步骤 |
|---|---|---|---|---|---|---|---|
| fix-aggfunc-total-precision | N/A（未编写） | required（登记时：R 比对/精度容差或语料策略可能构成合同澄清） | required（登记时存在歧义） | skipped（无边界决策） | required（standard） | **cancelled** | 已取消并归档；记为已知限制 |

sub-feature-id: fix-aggfunc-total-precision
Spec 门禁: required（登记时判定；取消后不再执行）
Spec 用户确认: required（登记时；取消后不适用）
Design 门禁: skipped（暂无边界决策）
Review 门禁: required（standard；取消后不适用）
状态: cancelled
后续步骤: none（已 cancelled 并归档；已知限制见用户决策）
阻塞原因: none（已取消）
恢复条件: none（用户明确 cancelled / wontfix，不恢复）
恢复后的目标状态: N/A

## Manager 门禁判定（2026-08-06）

- **路径**：`standard` — 精度/容差或语料策略可能改变比对合同。
- **Spec**：`required` + **用户确认 required**。
- **Design**：暂 `skipped`。
- **Review**：`required`。
- **分支**：源 `fix-aggfunc-total-precision` → 目标 `main`。

## 用户授权记录

- 2026-08-06：用户同意登记并跟踪；实施排在 WI-1（及可能 WI-2）之后。
- 2026-08-06：用户明确授权将本项标为 **cancelled** 并完成 Manager 关闭/归档。评估结论：不值得专门对齐原版修复。根因：~491 `total()` 大浮点 `%.3f` 差异来自 Java `String.format` vs 官方 `sqlite3_snprintf` 在 |x|~2^64 边界的打印差异；不是 `total()` 算错，也不是 JDBC 读错。决策：cancelled / wontfix；记为已知限制（极端量级 R 格式化可能与 C harness 不完全逐字节一致）。不移植 SQLite float printf，不改语料。

## 进度笔记

- 2026-08-06：Manager 登记；状态保持 **`backlog`**；不调度产出角色，直至恢复条件满足。
- 2026-08-06：WI-1 已 done；本项仍 **`backlog`**（用户要求勿自动推进）。
- 2026-08-06：用户确认 cancelled / wontfix（已知限制）；状态 `backlog` → **`cancelled`**；空 `agents/features/fix-aggfunc-total-precision/` 与本工作项记录一并归档至 `agents/archive/2026/fix-aggfunc-total-precision/`；不改产品代码、不改 `slt_lang_aggfunc.test`；按用户指示不主动 commit。
