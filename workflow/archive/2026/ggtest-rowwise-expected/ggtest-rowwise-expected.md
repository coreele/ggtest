# 工作项: ggtest-rowwise-expected

描述: 让 ggtest 接受/支持「按结果行书写、列以空格分隔」的期望结果形式；列分隔符写在**本条 query 期望头**上：`---- separator <delim>`（仅本条；废止文件级作用域与 `---separator`）。显式 separator 路径对 token **trim**。**废止**行式期望单引号/`''` 语法壳（R3）。默认仍「每值一行」兼容；哈希/Diff 值行硬约束不变。禁止入库 `.env`、`examples/demo2.slt`。
目标分支: main
源分支: ggtest-rowwise-expected
基线提交: a6c8719bc48099cf772a6bd1807876dd4577259c
文档影响: workflow/archive/2026/ggtest-rowwise-expected/{spec,design,plan,dev-notes,review,qa-report}.md；README；fixtures 于 `src/test/resources/fixtures/`

> **本文件须保存为 `workflow/archive/2026/ggtest-rowwise-expected/ggtest-rowwise-expected.md`**，文件名与目录同名。
> 流程定义见 `workflow/WORKFLOW.md`；看板见 `workflow/STATUS.md`。
> 本工作项的全部产物平铺在 `workflow/archive/2026/ggtest-rowwise-expected/`，无子目录、无版本后缀。
> 表内只填枚举、短标签或路径；理由与长说明写进「进度笔记」。

## 门禁

| 路径等级 | Spec | Spec 用户确认 | Design | Review |
|---|---|---|---|---|
| full | skipped | not-required | skipped | required |

## 状态

| 状态 | 下一步 | 阻塞原因 | 恢复条件 | 恢复后目标 |
|---|---|---|---|---|
| archived | — |  |  |  |

## 子项（仅 tracking 项填写）

| 子项 id | 状态 |
|---|---|
| — | |

## 进度笔记

- 2026-07-25…26：登记→实现→多轮合入前合同修订（含废止 R3）→ Review Approve → QA Pass 轮次3。
- 2026-07-26：用户回复「ok」授权合入 main；Manager 置 `done` 并与 review/qa 一次提交。
- 2026-07-26：fast-forward 合入 `main`（`95173b0`→`944e719`）；归档至 `workflow/archive/2026/ggtest-rowwise-expected/`；用户授权 push `main`。
- 2026-08-14：按 ggnote `WORKFLOW.md` 标准迁移工作流目录（记录与产物合并为同一目录；权威文件改为 `workflow/WORKFLOW.md`）。
