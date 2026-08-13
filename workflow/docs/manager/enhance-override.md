# 工作项记录: enhance-override

工作项标识: enhance-override
描述: 强化 `--override`：① 类型签名与实际结果不匹配时自动对齐（推断并改写签名）；② SQL 执行失败可改写为 `statement error <实际消息>`；③ 新增 `--override-separator <delim>` 控制期望回写格式（未指定默认 value-per-line）。
目标分支: main
文档影响: 受影响——README（`--override` 行为与 `--override-separator` 说明）；`--help` 行。

> 权威流程见 [workflow/README.md](../../README.md)；活跃状态见 [STATUS.md](STATUS.md)。
>
> 产物始终在 `workflow/docs/features/enhance-override/` 根目录（无子目录、不拆分）。
> 归档后本文件迁至 `workflow/docs/archive/YYYY/enhance-override/manager.md`（须修正相对链接）；`workflow/docs/manager/` 仅保留活跃项与 STATUS。
>
> 表内只填枚举、短标签或链接；较长理由写入「进度笔记」（见 [`workflow/docs/standards/documentation.md`](../../standards/documentation.md) §B）。

## 门禁

| 路径等级 | 源分支 | Spec | Spec 门禁 | Spec 用户确认 | Design 门禁 | Review 门禁 |
|---|---|---|---|---|---|---|
| standard | enhance-override | [spec.md](./../features/enhance-override/spec.md) | required | approved | required | required |

> 现有 `--override` 的接口/行为增强（跨 runner/cli/normalize），故 standard；Spec 因存在业务歧义（separator 语义、错误改写形态）需用户确认；Design required（OverrideWriter 需决定如何承载签名改写与 record 类型转换）；Review required。

## 状态

| 状态 | 后续步骤 | 阻塞原因 | 恢复条件 | 恢复后目标 |
|---|---|---|---|---|
| reviewing | Reviewer | | | |

## 进度笔记

- 2026-08-13：登记。需求来源：用户运行 `--override` 时遇「row width != signature length」「no such table」两类失败，要求强化 `--override`。状态 `backlog → speccing → awaiting-spec-approval`。与已 blocked 的 `sql-to-slt` 相关但独立（本项不新增 `.sql` 自动路由）。
- 2026-08-13：用户确认 Spec（approve）。Planner 产出 design.md（类型推断入 normalize、RecordResult 扩展字段、OverrideWriter 增签名改写/记录转换、separator 走 `--override-separator`）与 plan.md（T1–T6）。状态 → `planned`，待调度 Developer。
- 2026-08-13：Developer 实施完成（`TypeSignatureInferer` + runner/OverrideWriter/CLI 增强 + 6 处测试新增/更新）。`mvn test` 405/0（50 既有 skip）；端到端验证签名对齐、执行失败转 err、separator、自洽全绿。状态 → `reviewing`，待 Reviewer。
