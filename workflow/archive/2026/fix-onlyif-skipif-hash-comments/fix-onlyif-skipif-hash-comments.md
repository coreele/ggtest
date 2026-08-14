# 工作项: fix-onlyif-skipif-hash-comments

描述: 修复 sqllogictest 解析器对 `onlyif`/`skipif` 行尾 `#` 注释的处理，使官方语料写法 `onlyif sqlite # comment` / `skipif ... # comment` 能正确识别引擎名，消除 `in1.test` 等文件的 `onlyif requires a database name` 解析错误。
目标分支: main
源分支: fix-onlyif-skipif-hash-comments
基线提交: a6c8719bc48099cf772a6bd1807876dd4577259c
文档影响: `workflow/archive/2026/fix-onlyif-skipif-hash-comments/`（plan/dev-notes/review/qa-report）；默认不改 README 产品合同（本项为对齐官方语料既有写法的缺陷修复）

> **本文件须保存为 `workflow/archive/2026/fix-onlyif-skipif-hash-comments/fix-onlyif-skipif-hash-comments.md`**，文件名与目录同名。
> 流程定义见 `workflow/WORKFLOW.md`；看板见 `workflow/STATUS.md`。
> 本工作项的全部产物平铺在 `workflow/archive/2026/fix-onlyif-skipif-hash-comments/`，无子目录、无版本后缀。
> 表内只填枚举、短标签或路径；理由与长说明写进「进度笔记」。

## 门禁

| 路径等级 | Spec | Spec 用户确认 | Design | Review |
|---|---|---|---|---|
| fast | skipped | not-required | skipped | required |

## 状态

| 状态 | 下一步 | 阻塞原因 | 恢复条件 | 恢复后目标 |
|---|---|---|---|---|
| archived | — |  |  |  |

## 子项（仅 tracking 项填写）

| 子项 id | 状态 |
|---|---|
| — | |

## 进度笔记

- 2026-08-06：Manager 登记 `fix-onlyif-skipif-hash-comments`；路径 **fast**；Spec/Design **skipped**；Review **required**；源分支 `fix-onlyif-skipif-hash-comments` → 目标 `main`。状态 `backlog` → **`planning`**。下一步：调度 **Planner** 编写精简 `workflow/archive/2026/fix-onlyif-skipif-hash-comments/plan.md`（TDD 任务、触及文件、验证命令含 `in1.test` 或定点单测）。
- 2026-08-06：Planner 产出并 refine-docs `workflow/archive/2026/fix-onlyif-skipif-hash-comments/plan.md`（T0–T4；L2；开放问题无）。状态 `planning` → **`awaiting-plan-approval`**。**未**调度 Developer。待当前用户会话确认 Plan 后，Manager 持久化确认 → `planned` → 调度 Developer。
- 2026-08-06：用户确认 Plan（「ok」/「确认 Plan」）。Plan **approved**；状态 `awaiting-plan-approval` → **`planned`** → **`developing`**。调度 **Developer**：自 `main` 创建并检出源分支 `fix-onlyif-skipif-hash-comments`，按 plan T0–T4 TDD 实施；写 `dev-notes.md`；**不要 commit/push/merge**；禁止改 `workflow/docs/manager/*`。
- 2026-08-06：Developer 交接。T0–T4 完成于分支 `fix-onlyif-skipif-hash-comments`。T1 red：`onlyif`/`skipif` 尾注释 → `requires a database name`（Errors=2）。T2：仅对 onlyif/skipif header `stripTrailingHashComment` 后再 tokenize。验证：`SqlLogicTestParserTest` 33/0/0；`mvn -q clean test` **227/0/0/18**；`in1.test` **无**该 parse error，`passed=1 failed=0`。A1–A5 Pass。改动：`SqlLogicTestParser.java`、`SqlLogicTestParserTest.java`、`dev-notes.md`。未 commit。状态 `developing` → **`reviewing`**。调度 **Reviewer**（Review 门禁 required）。
- 2026-08-06：Reviewer **Approve**（无阻塞项）。独立复跑：`SqlLogicTestParserTest` 33/0/0/0；`mvn -q clean test` **227/0/0/18**；`in1.test` 无该 parse error 且 `failed=0`。非阻塞：合入时勿带入无关工作区 `pom.xml`。报告 `workflow/archive/2026/fix-onlyif-skipif-hash-comments/review.md`（未提交）。Review 门禁满足。状态 `reviewing` → **`qa`**。调度 **QA**；本轮 **不 merge / 不 commit**（除非用户另说）；QA Pass 后停在 merge-auth。
- 2026-08-06：QA **Pass**（无缺陷）。独立复跑：`SqlLogicTestParserTest` **33/0/0/0**；`mvn -q clean test` **227/0/0/18**；`in1.test` **无** `onlyif requires a database name`，`passed=1 failed=0`。报告 `workflow/archive/2026/fix-onlyif-skipif-hash-comments/qa-report.md`（未提交，留工作树）。状态维持 **`qa`**。**待当前用户会话明确合并授权**；授权后 Manager 在源分支置 `done` 并与未入库的 `review.md`/`qa-report.md`/实现与 manager 记录一次提交，再合入 `main`（用户本轮已声明不 merge，需另授权）。
- 2026-08-06：用户授权合并（「合入」）。门禁复核：Plan approved、Review **Approve**、QA **Pass**、源/目标分支已记录、授权已持久化。状态 `qa` → **`done`**。源分支一次提交纳入实现 + feature 文档（含 review/qa-report）+ manager 记录/STATUS；**排除** `pom.xml`、`sqllogictest/`。随后 `main` FF 合入（`6e1f311`）；**不 push**。用户要求归档父项 → 合入后归档。
- 2026-08-06：归档完成。`workflow/archive/2026/fix-onlyif-skipif-hash-comments/` 与 `workflow/archive/2026/fix-onlyif-skipif-hash-comments/fix-onlyif-skipif-hash-comments.md` 已迁至 `workflow/archive/2026/fix-onlyif-skipif-hash-comments/`（工作项记录为 `manager.md`）；相对链接已修正；活跃列表已移除。
- 2026-08-14：按 ggnote `WORKFLOW.md` 标准迁移工作流目录（记录与产物合并为同一目录；权威文件改为 `workflow/WORKFLOW.md`）。
