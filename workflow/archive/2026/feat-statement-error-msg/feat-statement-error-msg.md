# 工作项: feat-statement-error-msg

描述: `statement error` 支持可选的错误消息匹配（sqllogictest 规范：`statement error` 后可跟预期错误消息文本，执行失败时校对 errorSummary 包含该文本）
目标分支: main
源分支: feat-statement-error-msg
基线提交: a6c8719bc48099cf772a6bd1807876dd4577259c
文档影响: README.md（`statement error` 说明更新）、workflow 产物

> **本文件须保存为 `workflow/archive/2026/feat-statement-error-msg/feat-statement-error-msg.md`**，文件名与目录同名。
> 流程定义见 `workflow/WORKFLOW.md`；看板见 `workflow/STATUS.md`。
> 本工作项的全部产物平铺在 `workflow/archive/2026/feat-statement-error-msg/`，无子目录、无版本后缀。
> 表内只填枚举、短标签或路径；理由与长说明写进「进度笔记」。

## 门禁

| 路径等级 | Spec | Spec 用户确认 | Design | Review |
|---|---|---|---|---|
| standard | required | not-required | skipped | required |

## 状态

| 状态 | 下一步 | 阻塞原因 | 恢复条件 | 恢复后目标 |
|---|---|---|---|---|
| archived | — |  |  |  |

## 子项（仅 tracking 项填写）

| 子项 id | 状态 |
|---|---|
| — | |

## 进度笔记

- 变更范围：`StatementRecord` model（新增 `expectedErrorMsg` Optional 字段）、`SqlLogicTestParser.parseStatement`（解析 `statement error` 后的可选文本）、`SqlLogicTestRunner.runStatement`（ERROR 分支新增消息匹配逻辑）、`StatementExpectation` 与 `StatementResult` 的 Javadoc 移除"不匹配消息"的限制说明
- 向后兼容：`statement error` 后不跟消息时行为不变（仅验证执行失败）。`statement error <message>` 时须验证 errorSummary 包含指定消息（case-insensitive substring match）

## Review 轮次

- R1（2026-08-10，subagent）：结论 `Request changes`。全量回归 BUILD SUCCESS（262 通过 / 16 跳过）。详见 [review.md](review.md)。
  - major #1：`README.md` 未更新 `statement error <message>` 说明（Plan 声明的用户文档影响未落实）→ 须 Developer 补齐
  - minor #2：parser tokens 不足错误文案 `at least one` vs spec「错误与约束」`exactly one` → 建议反向更新 spec 文案对齐实现
  - minor #3：runner 失败原因 diff 风格 vs spec 表述 `expected ... to contain ...`（验收 P0-3 仅要求"含 `statement error message mismatch`"已满足）→ 建议反向更新 spec 文案对齐实现
  - 进入 QA 条件：未满足（standard 须 Approve）
- R2（2026-08-10，subagent）：结论 `Approve`。R1 三项遗留全部解决（README 已补 `Statement expectations`/`语句断言` 小节；spec 文案已对齐实现）。全量回归 BUILD SUCCESS（262 通过 / 16 跳过）。详见 review.md。
- QA（2026-08-10，subagent）：结论 `Pass`。V1–V4 全 BUILD SUCCESS（38 / 34 / 11 / 262 通过）；P0(1–4)、P1(5–7) 验收点全部通过；无缺陷。详见 qa-report.md。用户已授权合并。
- 2026-08-14：按 ggnote `WORKFLOW.md` 标准迁移工作流目录（记录与产物合并为同一目录；权威文件改为 `workflow/WORKFLOW.md`）。
