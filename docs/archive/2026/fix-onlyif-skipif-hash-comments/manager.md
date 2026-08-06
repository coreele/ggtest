# 工作项记录: fix-onlyif-skipif-hash-comments

工作项标识: fix-onlyif-skipif-hash-comments
描述: 修复 sqllogictest 解析器对 `onlyif`/`skipif` 行尾 `#` 注释的处理，使官方语料写法 `onlyif sqlite # comment` / `skipif ... # comment` 能正确识别引擎名，消除 `in1.test` 等文件的 `onlyif requires a database name` 解析错误。
路径等级: fast
源分支: fix-onlyif-skipif-hash-comments
目标分支: main
文档影响: `docs/features/fix-onlyif-skipif-hash-comments/`（plan/dev-notes/review/qa-report）；默认不改 README 产品合同（本项为对齐官方语料既有写法的缺陷修复）

> 权威工作流、门禁与状态说明见 [docs/README.md](../../README.md)。
> 活跃状态见 [STATUS.md](../../manager/STATUS.md)。
>
> 文档路径：未拆分时 Spec 为 `docs/features/<feature-id>/spec.md`（无子目录）；已拆分时根目录仅总览 Spec，各切片为 `docs/features/<feature-id>/<feature-id>-<sub>/spec.md`。
> 本文件已归档为 `docs/archive/2026/fix-onlyif-skipif-hash-comments/manager.md`；`docs/manager/` 仅保留活跃工作项记录与 `STATUS.md`。

## 切片（未拆分时仅一行，sub-feature-id = feature-id）

| sub-feature-id | Spec | Spec 门禁 | Spec 用户确认 | Design 门禁 | Review 门禁 | 状态 | 后续步骤 |
|---|---|---|---|---|---|---|---|
| fix-onlyif-skipif-hash-comments | N/A（跳过） | skipped（范围明确的单点解析缺陷；无新增公开行为/接口/状态机合同，仅恢复官方 `onlyif`/`skipif` 行尾 `#` 注释写法可解析） | not-required | skipped（无模块边界/分层/选型决策；在既有 parser 内处理行尾注释） | required（虽为 fast，但触及语料解析与条件指令语义，合入前需 Review Approve） | done | 已合入 main（`6e1f311`）；已归档；不 push |

阻塞原因: none
恢复条件: N/A
恢复后的目标状态: N/A

## Plan 确认

- Plan 路径: [plan.md](./plan.md)
- 确认结果: **approved**（2026-08-06）
- 确认依据: 当前用户会话「ok」/「确认 Plan」

## 用户授权记录

- 2026-08-06：Plan **approved**（「ok」/「确认 Plan」）。授权进入 `planned` → `developing`；本轮 **不要 commit / 不要 merge**，除非用户另说。
- 2026-08-06：用户「合入」——**合并授权**（合入 `main`，本地 FF；**不 push**）。合入范围排除无关 `pom.xml` 与未纳入本项的 `sqllogictest/` 语料目录。


## Manager 门禁判定（2026-08-06）

- **路径**：`fast` — 失败位点与根因猜测明确（`onlyif sqlite # …` 行尾注释导致引擎名未识别）；单点 parser 修复。
- **Spec**：`skipped` — 不发明超出「剥离/忽略 `onlyif`/`skipif` 行尾 `#` 注释」之外的产品需求；对齐官方 sqllogictest 语料既有写法。
- **Spec 用户确认**：`not-required`。
- **Design**：`skipped` — 无架构/选型决策。
- **Review**：`required` — 解析行为影响语料条件执行；合入前需 Approve。
- **分支**：源 `fix-onlyif-skipif-hash-comments` → 目标 `main`（调度 Developer 前已填写）。

## 失败证据（登记时）

- 命令：用户对 `sqllogictest/test/evidence/in1.test` 运行 ggtest
- 错误：`parse error: onlyif requires a database name`（报于约第 22 行；此前亦报第 38 行）
- 示例行：
  ```text
  onlyif sqlite # empty RHS
  query I nosort
  SELECT 1 NOT IN ()
  ----
  1
  ```
- 用户判断（登记采信为工作假设）：行内 `#` 注释导致 parser 未正确取得 database/engine 名。

## 约束

- Prefer TDD：先为带尾注释的 `onlyif`/`skipif` 补/改单元测试，再改 parser。
- 勿 invent 超出本缺陷修复的产品需求。
- **不要 commit**，除非用户后续明确要求。
- 验证至少使 `in1.test` 的该 parse error 消失（或对该文件/等价 fixture 复跑确认）。

## 进度笔记

- 2026-08-06：Manager 登记 `fix-onlyif-skipif-hash-comments`；路径 **fast**；Spec/Design **skipped**；Review **required**；源分支 `fix-onlyif-skipif-hash-comments` → 目标 `main`。状态 `backlog` → **`planning`**。下一步：调度 **Planner** 编写精简 `docs/features/fix-onlyif-skipif-hash-comments/plan.md`（TDD 任务、触及文件、验证命令含 `in1.test` 或定点单测）。
- 2026-08-06：Planner 产出并 refine-docs `docs/features/fix-onlyif-skipif-hash-comments/plan.md`（T0–T4；L2；开放问题无）。状态 `planning` → **`awaiting-plan-approval`**。**未**调度 Developer。待当前用户会话确认 Plan 后，Manager 持久化确认 → `planned` → 调度 Developer。
- 2026-08-06：用户确认 Plan（「ok」/「确认 Plan」）。Plan **approved**；状态 `awaiting-plan-approval` → **`planned`** → **`developing`**。调度 **Developer**：自 `main` 创建并检出源分支 `fix-onlyif-skipif-hash-comments`，按 plan T0–T4 TDD 实施；写 `dev-notes.md`；**不要 commit/push/merge**；禁止改 `docs/manager/*`。
- 2026-08-06：Developer 交接。T0–T4 完成于分支 `fix-onlyif-skipif-hash-comments`。T1 red：`onlyif`/`skipif` 尾注释 → `requires a database name`（Errors=2）。T2：仅对 onlyif/skipif header `stripTrailingHashComment` 后再 tokenize。验证：`SqlLogicTestParserTest` 33/0/0；`mvn -q clean test` **227/0/0/18**；`in1.test` **无**该 parse error，`passed=1 failed=0`。A1–A5 Pass。改动：`SqlLogicTestParser.java`、`SqlLogicTestParserTest.java`、`dev-notes.md`。未 commit。状态 `developing` → **`reviewing`**。调度 **Reviewer**（Review 门禁 required）。
- 2026-08-06：Reviewer **Approve**（无阻塞项）。独立复跑：`SqlLogicTestParserTest` 33/0/0/0；`mvn -q clean test` **227/0/0/18**；`in1.test` 无该 parse error 且 `failed=0`。非阻塞：合入时勿带入无关工作区 `pom.xml`。报告 `docs/features/fix-onlyif-skipif-hash-comments/review.md`（未提交）。Review 门禁满足。状态 `reviewing` → **`qa`**。调度 **QA**；本轮 **不 merge / 不 commit**（除非用户另说）；QA Pass 后停在 merge-auth。
- 2026-08-06：QA **Pass**（无缺陷）。独立复跑：`SqlLogicTestParserTest` **33/0/0/0**；`mvn -q clean test` **227/0/0/18**；`in1.test` **无** `onlyif requires a database name`，`passed=1 failed=0`。报告 `docs/features/fix-onlyif-skipif-hash-comments/qa-report.md`（未提交，留工作树）。状态维持 **`qa`**。**待当前用户会话明确合并授权**；授权后 Manager 在源分支置 `done` 并与未入库的 `review.md`/`qa-report.md`/实现与 manager 记录一次提交，再合入 `main`（用户本轮已声明不 merge，需另授权）。
- 2026-08-06：用户授权合并（「合入」）。门禁复核：Plan approved、Review **Approve**、QA **Pass**、源/目标分支已记录、授权已持久化。状态 `qa` → **`done`**。源分支一次提交纳入实现 + feature 文档（含 review/qa-report）+ manager 记录/STATUS；**排除** `pom.xml`、`sqllogictest/`。随后 `main` FF 合入（`6e1f311`）；**不 push**。用户要求归档父项 → 合入后归档。
- 2026-08-06：归档完成。`docs/features/fix-onlyif-skipif-hash-comments/` 与 `docs/manager/fix-onlyif-skipif-hash-comments.md` 已迁至 `docs/archive/2026/fix-onlyif-skipif-hash-comments/`（工作项记录为 `manager.md`）；相对链接已修正；活跃列表已移除。
