# 工作项记录: ggtest-rowwise-expected

工作项标识: ggtest-rowwise-expected
描述: 让 ggtest 接受/支持「按结果行书写、列以空格分隔」的期望结果形式；列分隔符写在**本条 query 期望头**上：`---- separator <delim>`（仅本条；废止文件级作用域与 `---separator`）。显式 separator 路径对 token **trim**。**废止**行式期望单引号/`''` 语法壳（R3）。默认仍「每值一行」兼容；哈希/Diff 值行硬约束不变。禁止入库 `.env`、`examples/demo2.slt`。
路径等级: full
源分支: ggtest-rowwise-expected
目标分支: main
文档影响: docs/features/ggtest-rowwise-expected/{spec,design,plan,dev-notes,review,qa-report}.md；README；fixtures 于 `src/test/resources/fixtures/`

> 权威工作流、门禁与状态说明见 [docs/README.md](../README.md)。
> 活跃状态见 [STATUS.md](STATUS.md)。

## 切片（未拆分时仅一行，sub-feature-id = feature-id）

| sub-feature-id | Spec | Spec 门禁 | Spec 用户确认 | Design 门禁 | Review 门禁 | 状态 | 后续步骤 |
|---|---|---|---|---|---|---|---|
| ggtest-rowwise-expected | [spec.md](../features/ggtest-rowwise-expected/spec.md) | required | **approved** | required | required（**Approve**） | **done** | 已授权合入；合入 main 后归档 |

阻塞原因: none
恢复条件: none
恢复后的目标状态: none

## 门禁判定理由

- **路径 full**：合同级期望书写语义。
- Spec/Plan 已确认；Review Approve；QA Pass 轮次3；用户 2026-07-26 回复「ok」**明确授权合入 main**。

## 合入前合同修订纪要（最终）

| 决议 | 内容 |
|---|---|
| R1 | `---- separator <delim>` 写在本条期望头上，仅本条生效 |
| R2 | 显式 separator 时 split 后各 token trim |
| R3 | **已废止**（无单引号/`''`/unquote） |

## 进度笔记

- 2026-07-25…26：登记→实现→多轮合入前合同修订（含废止 R3）→ Review Approve → QA Pass 轮次3。
- 2026-07-26：用户回复「ok」授权合入 main；Manager 置 `done` 并与 review/qa 一次提交；合入 main；关闭归档。
