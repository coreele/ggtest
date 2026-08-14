# 工作项: rust-rewrite

描述: 使用 Rust 语言重新实现 ggtest（sqllogictest CLI），取消 XuguDB，以 ODBC 统一数据库访问层。目标：功能等价、性能更优、单二进制部署。
目标分支: main
源分支: rust-rewrite
基线提交: a6c8719bc48099cf772a6bd1807876dd4577259c
文档影响: 本 Spec；后续 Rust 项目自带 README。

> **本文件须保存为 `workflow/workspace/rust-rewrite/rust-rewrite.md`**，文件名与目录同名。
> 流程定义见 `workflow/WORKFLOW.md`；看板见 `workflow/STATUS.md`。
> 本工作项的全部产物平铺在 `workflow/workspace/rust-rewrite/`，无子目录、无版本后缀。
> 表内只填枚举、短标签或路径；理由与长说明写进「进度笔记」。

## 门禁

| 路径等级 | Spec | Spec 用户确认 | Design | Review |
|---|---|---|---|---|
| full | required | required | required | required |

## 状态

| 状态 | 下一步 | 阻塞原因 | 恢复条件 | 恢复后目标 |
|---|---|---|---|---|
| spec-approval | 用户确认 Spec |  |  |  |

## 子项（仅 tracking 项填写）

| 子项 id | 状态 |
|---|---|
| — | |

## 进度笔记

- 2026-08-14：登记。需求来源：用户要求评估 Rust 重写，确定 ODBC 统一 DB 层、取消 XuguDB、AI 辅助预估 ~9.5 天。本阶段仅写 Spec，暂不实施。状态 `backlog → speccing → awaiting-spec-approval`。
- 2026-08-14：按 ggnote `WORKFLOW.md` 标准迁移工作流目录（记录与产物合并为同一目录；权威文件改为 `workflow/WORKFLOW.md`）。
