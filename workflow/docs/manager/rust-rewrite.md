# 工作项记录: rust-rewrite

工作项标识: rust-rewrite
描述: 使用 Rust 语言重新实现 ggtest（sqllogictest CLI），取消 XuguDB，以 ODBC 统一数据库访问层。目标：功能等价、性能更优、单二进制部署。
目标分支: main
文档影响: 本 Spec；后续 Rust 项目自带 README。

> 权威流程见 [workflow/README.md](../../README.md)；活跃状态见 [STATUS.md](STATUS.md)。

## 门禁

| 路径等级 | 源分支 | Spec 门禁 | Spec 用户确认 | Design 门禁 | Review 门禁 |
|---|---|---|---|---|---|
| full | rust-rewrite | required | required | required | required |

> 全新语言重写，跨所有模块，范围大且涉及技术选型决策，故 full；Spec + Design 均 required。

## 状态

| 状态 | 后续步骤 | 阻塞原因 | 恢复条件 | 恢复后目标 |
|---|---|---|---|---|
| awaiting-spec-approval | 用户确认 Spec | | | |

## 进度笔记

- 2026-08-14：登记。需求来源：用户要求评估 Rust 重写，确定 ODBC 统一 DB 层、取消 XuguDB、AI 辅助预估 ~9.5 天。本阶段仅写 Spec，暂不实施。状态 `backlog → speccing → awaiting-spec-approval`。
