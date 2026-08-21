# 工作项: ggtest-core

描述: GGTEST 从零到一实现 sqllogictest 格式测试工具（历史总览记录；本身未实施）。
目标分支: main
源分支: ggtest-core-parser
基线提交: a6c8719bc48099cf772a6bd1807876dd4577259c
文档影响: 项目 README；相关独立项见进度笔记

> 流程定义见 `workflow/WORKFLOW.md`；看板见 `workflow/STATUS.md`。
> 2026-08-14 迁移：原带子目录的归档已拆成独立工作项，符合「禁止子目录」约定。

## 门禁

| 路径等级 | Spec | Spec 用户确认 | Design | Review |
|---|---|---|---|---|
| fast | skipped | not-required | skipped | skipped |

## 状态

| 状态 | 下一步 | 阻塞原因 | 恢复条件 | 恢复后目标 |
|---|---|---|---|---|
| cancelled | — | | | |

## 进度笔记

- 本项从未实施。相关独立归档项：`ggtest-core-parser`、`ggtest-core-normalize`、`ggtest-core-runner-sqlite`、`ggtest-core-cli-corpus`（均已合入 `main`）。
- 2026-08-14：按当时 ggnote `WORKFLOW.md` 把原子目录迁成独立归档项。
- 2026-08-17：工作流取消 tracking / 父项子项；本项由 tracking 父项改为 `cancelled`。Git 字段沿用 `ggtest-core-parser` 的源分支与基线，仅满足校验，不表示本项有过实施分支。
