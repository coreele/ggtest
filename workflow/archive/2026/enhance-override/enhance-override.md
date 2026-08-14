# 工作项: enhance-override

描述: 强化 `--override`：① 类型签名与实际结果不匹配时自动对齐（推断并改写签名）；② SQL 执行失败可改写为 `statement error <实际消息>`；③ 新增 `--override-separator <delim>` 控制期望回写格式（未指定默认 value-per-line）。
目标分支: main
源分支: enhance-override
基线提交: a6c8719bc48099cf772a6bd1807876dd4577259c
文档影响: 受影响——README（`--override` 行为与 `--override-separator` 说明）；`--help` 行。

> **本文件须保存为 `workflow/archive/2026/enhance-override/enhance-override.md`**，文件名与目录同名。
> 流程定义见 `workflow/WORKFLOW.md`；看板见 `workflow/STATUS.md`。
> 本工作项的全部产物平铺在 `workflow/archive/2026/enhance-override/`，无子目录、无版本后缀。
> 表内只填枚举、短标签或路径；理由与长说明写进「进度笔记」。

## 门禁

| 路径等级 | Spec | Spec 用户确认 | Design | Review |
|---|---|---|---|---|
| standard | required | approved | required | required |

## 状态

| 状态 | 下一步 | 阻塞原因 | 恢复条件 | 恢复后目标 |
|---|---|---|---|---|
| archived | — |  |  |  |

## 子项（仅 tracking 项填写）

| 子项 id | 状态 |
|---|---|
| — | |

## 进度笔记

- 2026-08-13：登记。需求来源：用户运行 `--override` 时遇「row width != signature length」「no such table」两类失败，要求强化 `--override`。状态 `backlog → speccing → awaiting-spec-approval`。与已 blocked 的 `sql-to-slt` 相关但独立（本项不新增 `.sql` 自动路由）。
- 2026-08-13：用户确认 Spec（approve）。Planner 产出 design.md（类型推断入 normalize、RecordResult 扩展字段、OverrideWriter 增签名改写/记录转换、separator 走 `--override-separator`）与 plan.md（T1–T6）。状态 → `planned`，待调度 Developer。
- 2026-08-13：Developer 实施完成（`TypeSignatureInferer` + runner/OverrideWriter/CLI 增强 + 6 处测试新增/更新）。`mvn test` 405/0（50 既有 skip）；端到端验证签名对齐、执行失败转 err、separator、自洽全绿。状态 → `reviewing`，待 Reviewer。
- 2026-08-13：Spec 增补两项：① `--override-separator` 简写为 `--separator`；② PASS 的 query 也以新 separator 强制重写。Developer 实施完成（rename + force-reformat-passing）；`mvn test` 406/0；端到端验证 PASS 文件被行式重写。Review Approve r2 + QA Pass r2。状态 → `qa`，待用户授权合并。
- 2026-08-14：按 ggnote `WORKFLOW.md` 标准迁移工作流目录（记录与产物合并为同一目录；权威文件改为 `workflow/WORKFLOW.md`）。
