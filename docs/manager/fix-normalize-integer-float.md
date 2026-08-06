# 工作项记录: fix-normalize-integer-float

工作项标识: fix-normalize-integer-float
描述: 修复 `query I` 对浮点样数字字符串（如 JDBC `getString` 的 `"1.0"`/`"5.0"`）误走 `Long.parseLong` 失败并归一为 `"0"` 的问题；对齐官方 sqllogictest `%d`（可解析数值后向零截断）。消除 `slt_lang_aggfunc.test` 中 total()/avg() 及连带 label-sum 冲突等位点。不含 sum 溢出语义与 total 大浮点精度漂移（见并列工作项）。
路径等级: fast
源分支: fix-normalize-integer-float
目标分支: main
文档影响: `docs/features/fix-normalize-integer-float/`（plan/dev-notes/review/qa-report）；必要时更新 `ValueNormalizer` Javadoc；不改公开用户文档除非 Plan 标明

> 权威工作流、门禁与状态说明见 [docs/README.md](../README.md)。
> 活跃状态见 [STATUS.md](STATUS.md)。
>
> 文档路径：未拆分时 Spec 为 `docs/features/<feature-id>/spec.md`（无子目录）；已拆分时根目录仅总览 Spec，各切片为 `docs/features/<feature-id>/<feature-id>-<sub>/spec.md`。
> 归档后本文件迁至 `docs/archive/YYYY/<feature-id>/manager.md`，相对链接须同步修正；`docs/manager/` 仅保留活跃工作项记录与 `STATUS.md`。

## 切片（未拆分时仅一行，sub-feature-id = feature-id）

| sub-feature-id | Spec | Spec 门禁 | Spec 用户确认 | Design 门禁 | Review 门禁 | 状态 | 后续步骤 |
|---|---|---|---|---|---|---|---|
| fix-normalize-integer-float | N/A（跳过） | skipped（对齐已归档 [ggtest-core-normalize Spec](../archive/2026/ggtest-core/ggtest-core-normalize/spec.md)「I：按 `%d`；无法解释为整数 → `0`」；浮点样字符串属可解释数值后截断，非新增公开合同） | not-required | skipped（单点 `ValueNormalizer.normalizeInteger`；无模块边界/分层/选型决策） | required（虽为 fast，但触及核心比对归一化；合入前需 Review Approve） | done | 已授权合入 main（不 push）；待归档 |

阻塞原因:
恢复条件:
恢复后的目标状态:

## Manager 门禁判定（2026-08-06）

- **路径**：`fast` — 根因与改动面明确（`ValueNormalizer` + 既有单测）；验证点为单元测试 + `slt_lang_aggfunc.test` 相关位点。
- **Spec**：`skipped` — 既有合同已要求 `%d`；当前实现用 `Long.parseLong` 过窄，导致 `"1.0"` 等落入「无法解释 → 0」。本项是实现纠偏，不扩写合同。
- **Design**：`skipped` — 无边界/选型决策。
- **Review**：`required` — 归一化正确性影响全语料比对。
- **分支**：源 `fix-normalize-integer-float` → 目标 `main`（调度 Developer 前已填写）。
- **排除**：`pom.xml` 无关改动；未跟踪 `sqllogictest/` 大体量；WI-2 溢出语义；WI-3 total 精度漂移。

## 失败证据（登记时）

- 命令：`./bin/ggtest ./sqllogictest/test/evidence/slt_lang_aggfunc.test`
- 症状类：`query I` 期望 1/5/3 等，实际 `0`；连带 `label-sum` / `label-sum-distinct` 冲突（只读分析归因为整数归一化，非独立 bug）。
- 根因：`normalizeInteger` → `Long.parseLong`；JDBC 字符串含小数点 → `NumberFormatException` → `"0"`。

## Plan 确认

- Plan 路径: [docs/features/fix-normalize-integer-float/plan.md](../features/fix-normalize-integer-float/plan.md)
- 确认结果: **approved**（2026-08-06）
- 确认依据: 当前用户会话对 Plan 回复「ok」。

## 用户授权记录

- 2026-08-06：用户 `/manager ok，按需开多个工作项，逐个推进` = 同意按推荐拆分登记并**逐个推进**；先推进本项；**非** Plan/merge 授权；勿 commit/merge 除非另说。
- 2026-08-06：用户对 Plan 回复「ok」= **Plan 确认**；授权推进 Developer → Reviewer → QA；**仍非** merge 授权；**不要** commit/push/merge，除非用户另说。不启动 WI-2/WI-3 实施。
- 2026-08-06：用户 `/manager 合入` = **合并授权**（合入 `main`，本地 FF；**不 push**）。排除无关 `pom.xml` 与未跟踪 `sqllogictest/`。并要求归档本项。

## 进度笔记

- 2026-08-06：Manager 登记；路径 **fast**；Spec/Design **skipped**；Review **required**；状态 `backlog` → **`planning`**。调度 **Planner** 编写 `plan.md`。
- 2026-08-06：Planner 产出 plan.md（T0–T4；L2）。状态 → **`awaiting-plan-approval`**。
- 2026-08-06：用户确认 Plan（「ok」）。Plan **approved**；→ **`planned`** → **`developing`**。调度 Developer。
- 2026-08-06：Developer T0–T4 done。验证 228/0/0/18；WI-1 位点消失。→ **`reviewing`**。
- 2026-08-06：Reviewer **Approve**。→ **`qa`**。
- 2026-08-06：QA **Pass**。停 merge-auth。
- 2026-08-06：用户授权合并（「合入」）。前置核对：Plan approved、Review Approve、QA Pass、源/目标已记录、授权持久化。状态 → **`done`**；源分支一次提交（实现 + plan/dev-notes/review/qa-report + STATUS/工作项记录）；随后 FF 合入 `main`；**不 push**；并归档。
