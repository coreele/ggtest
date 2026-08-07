# 工作项记录: chore-audit-tails

工作项标识: chore-audit-tails
描述: 上轮盘点尾巴一并收口（排除 CA-007）：CA-008 补 ValueNormalizer Javadoc + 登记册状态；cli-report TTY `--color auto` 真机/可控验证；ggtest-pg 非空 `GGTEST_PG_PASSWORD` 路径与 DEF-PG-003 回归证据；官方语料 `.temp/select1..5.test`（或 `GGTEST_CORPUS_DIR`）sqlite 硬验收冒烟并写入产物。
路径等级: fast
源分支: chore-audit-tails
目标分支: main
文档影响: `workflow/docs/archive/2026/chore-audit-tails/`（plan/dev-notes/review/qa-report）；`workflow/docs/standards/code-audit-register.md`（CA-008）；必要时 README 仅当验证命令需公开补充（默认不改产品合同文档）

> 权威工作流、门禁与状态说明见 [workflow/README.md](../../../README.md)。
> 活跃状态见 [STATUS.md](STATUS.md)。
>
> 文档路径：未拆分时 Spec 为 `workflow/docs/archive/2026/<feature-id>/spec.md`（无子目录）；已拆分时根目录仅总览 Spec，各切片为 `workflow/docs/archive/2026/<feature-id>/<feature-id>-<sub>/spec.md`。
> 归档后本文件迁至 `workflow/docs/archive/YYYY/<feature-id>/manager.md`，相对链接须同步修正；`workflow/docs/manager/` 仅保留活跃工作项记录与 `STATUS.md`。

## 切片（未拆分时仅一行，sub-feature-id = feature-id）

| sub-feature-id | Spec | Spec 门禁 | Spec 用户确认 | Design 门禁 | Review 门禁 | 状态 | 后续步骤 |
|---|---|---|---|---|---|---|---|
| chore-audit-tails | N/A（跳过） | skipped（无新增公开合同/行为意图；CA-008 已 accepted；其余为既有能力验证缺口与可能的小缺陷修复；CA-007 明确排除） | not-required | skipped（无模块边界/分层/选型决策） | required（虽为 fast，但触及 CLI/PG/归一化注释与多处测试，合入前需 Review Approve） | done | 已授权合入 main（不 push）；归档待用户指令 |

阻塞原因:
恢复条件:
恢复后的目标状态:

## Plan 确认

- Plan 路径: [workflow/docs/archive/2026/chore-audit-tails/plan.md](../features/chore-audit-tails/plan.md)
- 确认结果: **approved**（2026-07-26）
- 确认依据: 当前用户会话「/manager 一起修了」+ 分项动词（补/测/修/补测）= 执行授权；Manager 将精简 Plan 要点视为已获实施授权并持久化后进入 `planned` → `developing`。

## 范围与排除

| # | 项 | 指令 | 纳入本项 |
|---|---|---|---|
| 1 | CA-007 ResultComparer LCS | 暂时不动 | **否** — 禁止登记/改代码 |
| 2 | CA-008 ValueNormalizer Javadoc + 登记册 | 补一下 | 是 |
| 3 | cli-report TTY `--color auto` | 测一下；缺测补测；有缺陷则修 | 是 |
| 4 | ggtest-pg 非空密码路径；DEF-PG-003 回归 | 修一下；可控验证；勿编造 Pass | 是 |
| 5 | `.temp/select1..5` / 语料硬验收冒烟 | 补测一下；命令与结果写入产物 | 是 |

禁止：重开 `architecture-overview`；提交真实 `.env` / 凭据；强制入库 `.temp/select*.test`（gitignore 保留）。

## 用户授权记录

- 2026-07-26：用户「/manager 一起修了」+ 分项动词（补/测/修/补测）= **执行授权**。Plan 仍须产出；确认方式：用户会话已授权实施（见进度笔记）；确认写入后进入 `planned` → `developing`。
- 本轮约束：**不要 commit / 不要 push / 不要 merge**，除非父会话另说；QA Pass 后停 merge-auth。
- 2026-07-26：用户回复「ok」——**合并授权**（合入 `main`，本地 FF 或等价安全合入；**不 push**；CA-007 继续未动）。

## 进度笔记

- 2026-07-26：Manager 登记 umbrella `chore-audit-tails`；路径 **fast**；Spec/Design **skipped**；Review **required**；源分支 `chore-audit-tails` → 目标 `main`。状态 `backlog` → **`planning`**。调度 **Planner** 编写精简 `plan.md`（四项任务、验证命令、勿动 CA-007）。
- 2026-07-26：Planner 产出 `workflow/docs/archive/2026/chore-audit-tails/plan.md`（T1–T5；排除 CA-007）。用户会话已授权实施 → Plan **approved**；状态 `planning` → **`planned`** → **`developing`**。调度 **Developer**：自 `main` 创建并检出源分支 `chore-audit-tails`，按 Plan TDD 实施；本轮 **不要 commit/push/merge**；更新 `dev-notes.md` 与 CA-008 登记册；禁止改 CA-007 / ResultComparer LCS。
- 2026-07-26：Developer 交接。T1（CA-008 Javadoc+登记册）、T2（`--color auto` 可注入 TTY + 测试）、T4（select1–5 sqlite 冒烟 exit 0/failed=0）**done**；T3 可控「非空密码装配 + 永不回显」及无门控 DEF-PG-003 **Pass**，真库门控实连按 quality.md §6 记 **skip**（本机 `.env→GGTEST_PG_*` 后 JDBC 连接失败，未探凭据、未标 Pass）；T5 `dev-notes.md` 产出。验证：`mvn -q clean test` **224/0/0/18**；`package` **SUCCESS**；未改 `workflow/docs/manager/*`、未 commit、未碰 CA-007。改动文件：`ValueNormalizer.java`、`code-audit-register.md`、`Main.java`、`CliReportAcceptanceTest.java`、`RuntimeConfigResolverTest.java`、`PostgresCliIntegrationTest.java`、新增 `dev-notes.md`。本轮约束更新：**push 禁止**，commit 允许但由 Manager 在 merge 授权后统一处理。状态 `developing` → **`reviewing`**。调度 **Reviewer**（Review 门禁 required）。
- 2026-07-26：Reviewer **Approve**（无阻塞项）；A1–A6 逐项 Pass（A3 真库 / A4 有门控全量按 §6 记证，未编造 Pass）；报告 `workflow/docs/archive/2026/chore-audit-tails/review.md`（未提交，留工作树）。Review 门禁满足。状态 `reviewing` → **`qa`**。调度 **QA** 独立复跑；QA Pass 后停在 merge-auth 用户确认。
- 2026-07-26：QA **Pass**（无缺陷、非 Blocked）。独立复跑：`mvn -q clean test` **224/0/0/18**；`package` **SUCCESS**；定点三测 **40/0/0/4**（可控非空密码不回显 OK，门控 4 skip）；Corpus **2/0/0**、select1–5 `passed=5 failed=0`。A1–A6 全 Pass；真库 PG E2E 与有门控全量按 quality.md §6 记缺口（恢复条件见 `qa-report.md`）。报告 `workflow/docs/archive/2026/chore-audit-tails/qa-report.md`（未提交，留工作树）。**门禁到达 merge-auth**：按 README/git.md，QA Pass 待授权期间不单独提交 `review.md`/`qa-report.md`。状态维持 `qa`，**待当前用户会话明确合并授权**；授权后 Manager 在源分支置 `done` 并将 STATUS/工作项记录/`review.md`/`qa-report.md` 一次提交，随后合入 `main`（本轮 **不 push**）。
- 2026-07-26：用户授权合并（「ok」）。合并前置七项核对通过（Plan approved、Review Approve、QA Pass、源/目标分支已记录、授权已持久化、状态置 `done`、git.md 满足：merge-base=main tip 可 FF）。Manager 在源分支将状态置 **`done`**，与实现改动及未入库的 `dev-notes.md`/`review.md`/`qa-report.md`/`plan.md` **一次提交**；随后 `main` 上 `git merge --ff-only chore-audit-tails` 合入；**不 push**。CA-007 全程未动。
