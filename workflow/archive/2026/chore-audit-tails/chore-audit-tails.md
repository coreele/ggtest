# 工作项: chore-audit-tails

描述: 上轮盘点尾巴一并收口（排除 CA-007）：CA-008 补 ValueNormalizer Javadoc + 登记册状态；cli-report TTY `--color auto` 真机/可控验证；ggtest-pg 非空 `GGTEST_PG_PASSWORD` 路径与 DEF-PG-003 回归证据；官方语料 `.temp/select1..5.test`（或 `GGTEST_CORPUS_DIR`）sqlite 硬验收冒烟并写入产物。
目标分支: main
源分支: chore-audit-tails
基线提交: a6c8719bc48099cf772a6bd1807876dd4577259c
文档影响: `workflow/archive/2026/chore-audit-tails/`（plan/dev-notes/review/qa-report）；`workflow/audit/register.md`（CA-008）；必要时 README 仅当验证命令需公开补充（默认不改产品合同文档）

> **本文件须保存为 `workflow/archive/2026/chore-audit-tails/chore-audit-tails.md`**，文件名与目录同名。
> 流程定义见 `workflow/WORKFLOW.md`；看板见 `workflow/STATUS.md`。
> 本工作项的全部产物平铺在 `workflow/archive/2026/chore-audit-tails/`，无子目录、无版本后缀。
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

- 2026-07-26：Manager 登记 umbrella `chore-audit-tails`；路径 **fast**；Spec/Design **skipped**；Review **required**；源分支 `chore-audit-tails` → 目标 `main`。状态 `backlog` → **`planning`**。调度 **Planner** 编写精简 `plan.md`（四项任务、验证命令、勿动 CA-007）。
- 2026-07-26：Planner 产出 `workflow/archive/2026/chore-audit-tails/plan.md`（T1–T5；排除 CA-007）。用户会话已授权实施 → Plan **approved**；状态 `planning` → **`planned`** → **`developing`**。调度 **Developer**：自 `main` 创建并检出源分支 `chore-audit-tails`，按 Plan TDD 实施；本轮 **不要 commit/push/merge**；更新 `dev-notes.md` 与 CA-008 登记册；禁止改 CA-007 / ResultComparer LCS。
- 2026-07-26：Developer 交接。T1（CA-008 Javadoc+登记册）、T2（`--color auto` 可注入 TTY + 测试）、T4（select1–5 sqlite 冒烟 exit 0/failed=0）**done**；T3 可控「非空密码装配 + 永不回显」及无门控 DEF-PG-003 **Pass**，真库门控实连按 quality.md §6 记 **skip**（本机 `.env→GGTEST_PG_*` 后 JDBC 连接失败，未探凭据、未标 Pass）；T5 `dev-notes.md` 产出。验证：`mvn -q clean test` **224/0/0/18**；`package` **SUCCESS**；未改 `workflow/docs/manager/*`、未 commit、未碰 CA-007。改动文件：`ValueNormalizer.java`、`code-audit-register.md`、`Main.java`、`CliReportAcceptanceTest.java`、`RuntimeConfigResolverTest.java`、`PostgresCliIntegrationTest.java`、新增 `dev-notes.md`。本轮约束更新：**push 禁止**，commit 允许但由 Manager 在 merge 授权后统一处理。状态 `developing` → **`reviewing`**。调度 **Reviewer**（Review 门禁 required）。
- 2026-07-26：Reviewer **Approve**（无阻塞项）；A1–A6 逐项 Pass（A3 真库 / A4 有门控全量按 §6 记证，未编造 Pass）；报告 `workflow/archive/2026/chore-audit-tails/review.md`（未提交，留工作树）。Review 门禁满足。状态 `reviewing` → **`qa`**。调度 **QA** 独立复跑；QA Pass 后停在 merge-auth 用户确认。
- 2026-07-26：QA **Pass**（无缺陷、非 Blocked）。独立复跑：`mvn -q clean test` **224/0/0/18**；`package` **SUCCESS**；定点三测 **40/0/0/4**（可控非空密码不回显 OK，门控 4 skip）；Corpus **2/0/0**、select1–5 `passed=5 failed=0`。A1–A6 全 Pass；真库 PG E2E 与有门控全量按 quality.md §6 记缺口（恢复条件见 `qa-report.md`）。报告 `workflow/archive/2026/chore-audit-tails/qa-report.md`（未提交，留工作树）。**门禁到达 merge-auth**：按 README/git.md，QA Pass 待授权期间不单独提交 `review.md`/`qa-report.md`。状态维持 `qa`，**待当前用户会话明确合并授权**；授权后 Manager 在源分支置 `done` 并将 STATUS/工作项记录/`review.md`/`qa-report.md` 一次提交，随后合入 `main`（本轮 **不 push**）。
- 2026-07-26：用户授权合并（「ok」）。合并前置七项核对通过（Plan approved、Review Approve、QA Pass、源/目标分支已记录、授权已持久化、状态置 `done`、git.md 满足：merge-base=main tip 可 FF）。Manager 在源分支将状态置 **`done`**，与实现改动及未入库的 `dev-notes.md`/`review.md`/`qa-report.md`/`plan.md` **一次提交**；随后 `main` 上 `git merge --ff-only chore-audit-tails` 合入；**不 push**。CA-007 全程未动。
- 2026-08-14：按 ggnote `WORKFLOW.md` 标准迁移工作流目录（记录与产物合并为同一目录；权威文件改为 `workflow/WORKFLOW.md`）。
