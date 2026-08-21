# 工作项: execute-markdown-slt

描述: 支持直接执行 Markdown 文档中的 sqllogictest 代码块
目标分支: main
源分支: execute-markdown-slt
基线提交: 3014854f21af4b0b4d6adf38de5a251d1ae160ef
文档影响: README.md, README.zh-CN.md

> **本文件须保存为 `workflow/archive/2026/execute-markdown-slt/execute-markdown-slt.md`**，文件名与目录同名。
> 流程定义见 `workflow/WORKFLOW.md`；看板见 `workflow/STATUS.md`。
> 本工作项的全部产物平铺在 `workflow/archive/2026/execute-markdown-slt/`，无子目录、无版本后缀。
> 表内只填枚举、短标签或路径；理由与长说明写进「进度笔记」。

## 门禁

| 路径等级 | Spec | Spec 用户确认 | Design | Review |
|---|---|---|---|---|
| standard | required | required | required | required |

## 状态

| 状态 | 下一步 | 阻塞原因 | 恢复条件 | 恢复后目标 |
|---|---|---|---|---|
| archived | — |  |  |  |

## 进度笔记

- 2026-08-21：用户要求开启第一版 Markdown 可执行文档：`sql` / `slt` / `sqllogictest` fenced code block 映射到现有 sqllogictest 执行器；代码块外全部不执行并保留行号；设计需给未来 Python / sh 等执行器预留扩展。判定 `standard`：新增用户可见输入格式与执行合同，需 Spec；存在执行入口抽象与未来扩展边界，需 Design；Review required。
- 2026-08-21：用户确认 Spec，状态 `spec-approval → designing`。
- 2026-08-21：Design 完成。决策：Markdown 处理放在 CLI 输入适配层，生成同等行数 SLT 运行视图；parser/runner 不承担 Markdown 职责；用语言 registry 为未来执行器预留扩展。状态 `designing → planning`。
- 2026-08-21：Plan 完成，覆盖 extractor、FileRunner 接线、目录收集、`--override` 与 README。状态 `planning → developing`。
- 2026-08-21：Developer 完成实现、自验与目标分支同步检查。实现提交 `cada48b3e431a36a85131d751b46c9b15b772f07`；本地 `main` `3014854f21af4b0b4d6adf38de5a251d1ae160ef` 为源分支 HEAD 祖先，无需 rebase；三条计划验证命令均通过。状态 `developing → reviewing`。
- 2026-08-21：Review 结论 `Approve`，未发现必修项或未解决安全问题。状态 `reviewing → qa`。
- 2026-08-21：QA 首轮结论 `Pass`，三条计划验证命令均通过。状态 `qa → merge-approval`，等待用户授权合并；`review.md` 与 `qa-report.md` 按 Git 规范暂留工作树，授权后与 `done` 一次提交。
- 2026-08-21：用户原话「ok，允许合并」= 明确合并授权。状态 `merge-approval → done`；合入前按 Git 规范整理源分支独有提交，然后 fast-forward 合入 `main`。
- 2026-08-21：合入前提交整理完成，源分支独有提交压缩为实现/测试、README、工作流生命周期三类提交；整理后三条计划验证命令均通过。
- 2026-08-21：确认合入：本地 `main` 与源分支同为 `dc45391`（`7951ed6` 实现、`ccddcfe` README、`dc45391` 工作流）。用户确认 Markdown 可执行能力已合入。状态 `done → archived`。旁路登记 `md-input` 仅为重复登记（独有提交只有工作项记录），能力由本项交付；拆除其 worktree 并删除源分支。
