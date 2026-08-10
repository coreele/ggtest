# 工作项记录: feat-statement-error-msg

工作项标识: feat-statement-error-msg
描述: `statement error` 支持可选的错误消息匹配（sqllogictest 规范：`statement error` 后可跟预期错误消息文本，执行失败时校对 errorSummary 包含该文本）
目标分支: main
文档影响: README.md（`statement error` 说明更新）、workflow 产物

> 权威流程见 [workflow/README.md](../../README.md)；活跃状态见 [STATUS.md](STATUS.md)。
>
> **切片级：** 路径等级、源分支、门禁、状态、阻塞。**工作项级：** 目标分支。
> 未拆分：产物在 `workflow/docs/features/feat-statement-error-msg/`。
> 归档后本文件迁至 `workflow/docs/archive/YYYY/feat-statement-error-msg/manager.md`（须修正相对链接）；`workflow/docs/manager/` 仅保留活跃项与 STATUS。
>
> 表内只填枚举、短标签或链接；较长理由写入「进度笔记」（见 `workflow/docs/standards/documentation.md` §B）。

## 切片门禁

| sub-feature-id | 路径等级 | 源分支 | Spec | Spec 门禁 | Spec 用户确认 | Design 门禁 | Review 门禁 |
|---|---|---|---|---|---|---|---|
| feat-statement-error-msg | standard | feat-statement-error-msg | [spec.md](./../features/feat-statement-error-msg/spec.md) | required | not-required | skipped | required |

## 切片状态

| sub-feature-id | 状态 | 后续步骤 | 阻塞原因 | 恢复条件 | 恢复后目标 |
|---|---|---|---|---|---|
| feat-statement-error-msg | done | 已授权合并→合入 main | | | |

## 进度笔记

- 变更范围：`StatementRecord` model（新增 `expectedErrorMsg` Optional 字段）、`SqlLogicTestParser.parseStatement`（解析 `statement error` 后的可选文本）、`SqlLogicTestRunner.runStatement`（ERROR 分支新增消息匹配逻辑）、`StatementExpectation` 与 `StatementResult` 的 Javadoc 移除"不匹配消息"的限制说明
- 向后兼容：`statement error` 后不跟消息时行为不变（仅验证执行失败）。`statement error <message>` 时须验证 errorSummary 包含指定消息（case-insensitive substring match）

## Review 轮次

- R1（2026-08-10，subagent）：结论 `Request changes`。全量回归 BUILD SUCCESS（262 通过 / 16 跳过）。详见 [review.md](./../features/feat-statement-error-msg/review.md)。
  - major #1：`README.md` 未更新 `statement error <message>` 说明（Plan 声明的用户文档影响未落实）→ 须 Developer 补齐
  - minor #2：parser tokens 不足错误文案 `at least one` vs spec「错误与约束」`exactly one` → 建议反向更新 spec 文案对齐实现
  - minor #3：runner 失败原因 diff 风格 vs spec 表述 `expected ... to contain ...`（验收 P0-3 仅要求"含 `statement error message mismatch`"已满足）→ 建议反向更新 spec 文案对齐实现
  - 进入 QA 条件：未满足（standard 须 Approve）
- R2（2026-08-10，subagent）：结论 `Approve`。R1 三项遗留全部解决（README 已补 `Statement expectations`/`语句断言` 小节；spec 文案已对齐实现）。全量回归 BUILD SUCCESS（262 通过 / 16 跳过）。详见 review.md。
- QA（2026-08-10，subagent）：结论 `Pass`。V1–V4 全 BUILD SUCCESS（38 / 34 / 11 / 262 通过）；P0(1–4)、P1(5–7) 验收点全部通过；无缺陷。详见 qa-report.md。用户已授权合并。
