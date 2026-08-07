# 工作项记录: ggtest-pg

工作项标识: ggtest-pg
描述: 为 GGTEST 增加 PostgreSQL 支持（多库扩展第二引擎）+ **CLI `.env` 配置加载**（对 SQLite 与 PG 均适用）。首期 `ggtest-core`（SQLite + CLI）已归档；本项交付 PG JDBC 执行器；CLI `--engine` 规范名 **`postgres`**；每文件 **schema 级**隔离；engine↔URL 不一致为硬错误（退出码 2）；与 SQLite 并存且不破坏 SQLite 硬验收；**不**要求 PG 官方语料硬验收失败=0。`.env`：优先级 CLI > 环境变量 > `.env`；CWD `.env` + `--env-file`；键 `GGTEST_URL`/`USER`/`PASSWORD`/`ENGINE`/`HASH_THRESHOLD`；未知键忽略；禁止提交真实 `.env`。不纳入 `examples/` 未跟踪语料/demo 提交。
路径等级: full
源分支: ggtest-pg
目标分支: main
文档影响: workflow/workflow/docs/features/ggtest-pg/；README / CLI 用法 / `.env.example`（见 Plan）

> 权威工作流、门禁与状态说明见 [workflow/README.md](../../../README.md)。
> 活跃状态见 [STATUS.md](../../manager/STATUS.md)。
>
> 文档路径：未拆分时 Spec 为 `workflow/workflow/docs/features/ggtest-pg/spec.md`（无子目录）。

## 切片（未拆分时仅一行，sub-feature-id = feature-id）

| sub-feature-id | Spec | Spec 门禁 | Spec 用户确认 | Design 门禁 | Review 门禁 | 状态 | 后续步骤 |
|---|---|---|---|---|---|---|---|
| ggtest-pg | [spec.md](./spec.md) | required | approved | required（多库边界 + `.env` 配置解析/优先级） | required | done | 已授权关闭；合入 main；已归档至 `workflow/workflow/docs/archive/2026/ggtest-pg/` |

阻塞原因: none
恢复条件: none
恢复后的目标状态: none

## QA 缺陷（轮次 2）

| ID | 严重度 | 状态 | 说明 |
|---|---|---|---|
| DEF-PG-001 | 高 | closed | conditions.test 补空行；QA 轮次 3 验证关闭 |
| DEF-PG-002 | 高 | closed | shade ServicesResourceTransformer；QA 轮次 3 验证关闭 |
| DEF-PG-003 | 高 | closed | 根目录运行时 `.env` 污染 CLI 编排测试（曾 148/9/15）。**方案 A** 修复 `b6ea61f`：产品 CLI 读 CWD `.env` **不改**；测试注入 `envLookup`(null) + `@TempDir` 隔离。QA 轮次 4 验证关闭：根 `.env` postgres 在场时门控关 148/0/17、门控开 148/0/5（PG 实跑）、package SUCCESS、CLI 合同/select1 不回归。 |

## 门禁判定说明

- **路径等级 `full`**：PostgreSQL + CLI `.env`。
- **Spec 门禁 `required`**；**Spec 用户确认 `approved`**。
- **Design 门禁 `required`**；design.md 已定稿。
- **Review 门禁 `required`**。
- **Plan 用户确认 `approved`**（2026-07-25：ok 实施 / 继续；T1–T7、`.env` 决策、驱动 42.7.13、源分支 `ggtest-pg` → `main`，无修改）。
- **拆分**：未拆分；留在 `ggtest-pg`。

## 用户已确认决策（勿重开）

### PG（2026-07-25）

| 议题 | 结论 |
|---|---|
| engine 名 | **`postgres`** |
| 每文件隔离 | **schema 级** |
| engine↔URL 不一致 | **硬错误**（退出码 2） |
| PG 官方语料硬验收失败=0 | **否** |

### `.env` 增量（2026-07-25）

| 议题 | 结论 |
|---|---|
| E1 优先级 | **命令行 > 环境变量 > `.env`** |
| E2 路径 | CWD `.env` + 支持 **`--env-file`** |
| E3 键名 | `GGTEST_URL` / `GGTEST_USER` / `GGTEST_PASSWORD` / `GGTEST_ENGINE` / `GGTEST_HASH_THRESHOLD`（≠ `GGTEST_PG_*` 测试门控） |
| E4 未知键 | **忽略** |
| E5 归属 | **留在 `ggtest-pg`**（不另立） |
| Spec 修订稿整体 | **approved（ok）** |

### Plan

| 议题 | 结论 |
|---|---|
| Plan / Design | **approved**（T1–T7；postgresql 42.7.13；源分支 `ggtest-pg` → `main`；无修改） |

## 运维约定

- 网络失败可用代理 `127.0.0.1:7890`；多次失败则停止。
- **不要**提交 `examples/` 下未跟踪语料/demo。
- **禁止**创建或提交真实 `.env`；示例仅 `.env.example`（无真实凭据）。

## 进度笔记

- 2026-07-25 **登记**：用户指令 `/manager 开启下一工作项，支持 PG`。feature-id=`ggtest-pg`；路径 `full`；Spec/Design/Review 均为 required；未拆分。状态 `backlog` → **`speccing`**。调度 Analyst 编写 `workflow/workflow/docs/features/ggtest-pg/spec.md`。单步编排：Spec 完成后停在 `awaiting-spec-approval`；**不得**自行批准 Spec。
- 2026-07-25 **Analyst 完成 Spec**：产出 `workflow/workflow/docs/features/ggtest-pg/spec.md`（合同、P0/P1、开放问题；已 refine-docs）。状态 `speccing` → **`awaiting-spec-approval`**。到达 Spec 用户确认门禁；单步停止；**未**自行批准 Spec、**未**调度 Planner。Analyst 建议拆分 `executor-pg` / `cli-engine`（仅建议，未执行）。
- 2026-07-25 **用户确认 Spec（整体 ok）并拍板开放问题**：engine=`postgres`；隔离=schema 级；engine↔URL 不一致=硬错误（退出码 2）；PG 官方语料硬验收失败=0=否。Manager：Spec 用户确认 → **`approved`**；决议已写入本记录。因 Spec 正文仍含候选 `postgresql` 等待确认表述，调度 **Analyst** 做最小修订对齐后再进入 Design。
- 2026-07-25 **Analyst 最小修订 Spec 完成**：`spec.md` 已对齐 `postgres` / schema 隔离 / 硬错误 / P1-PG-4 非硬验收；待确认已清空。状态 `awaiting-spec-approval` → **`designing`**。调度 **Planner** 编写 `design.md` 然后 `plan.md`。运维：代理 `127.0.0.1:7890`；多次失败则停止；勿提交 `examples/`。**不得**自行批准 Plan。
- 2026-07-25 **Planner 完成 Design + Plan**：产出 `design.md`（`db.postgres`、JDBC 42.7.13、schema 隔离由 CLI 编排、engine↔URL 硬校验、`GGTEST_PG_URL` 门控）与 `plan.md`（T1–T6；L3；建议源分支 `ggtest-pg`）。Design 门禁 **通过**（design.md 存在）。状态 `designing` →（planning）→ **`awaiting-plan-approval`**。到达 Plan 用户确认门禁；单步停止；**未**自行批准 Plan、**未**调度 Developer。
- 2026-07-25 **Plan 确认前门禁前新增需求（`.env`）**：判定为 Spec 变更（新增可见行为 + 公开 CLI 合同 + 安全影响）。**未**将用户表述视为 Plan 批准。状态 `awaiting-plan-approval` → **`speccing`**；Spec 用户确认 → **`required`**（增量再确认）；既有 PG 决议勿重开；`design.md`/`plan.md` 标为过时草案。Manager 暂选将 `.env` 纳入本工作项 Spec 修订（方案 A），利弊与是否另立项交用户定夺。调度 **Analyst** 修订 Spec。
- 2026-07-25 **Analyst 完成 `.env` Spec 增量修订**：`spec.md` 新增 `.env` 合同与 P0-ENV-1…4 / P1-ENV-1…2；开放问题 E1–E5；PG 已决议保留。状态 `speccing` → **`awaiting-spec-approval`**。到达 Spec 用户确认门禁；单步停止；**未**自行批准 Spec/Plan、**未**调度 Planner。
- 2026-07-25 **用户确认 `.env` Spec 修订稿（ok）并拍板 E1–E5**：优先级 CLI>env>`.env`；CWD `.env`+`--env-file`；键 `GGTEST_*`；未知键忽略；归属留在 `ggtest-pg`。Manager：Spec 用户确认 → **`approved`**；决议已写入本记录。调度 Analyst 最小修订 Spec「已决议」→ 再 `designing` → Planner 更新 Design/Plan。**不得**自行批准 Plan。
- 2026-07-25 **Analyst 对齐 E1–E5 完成**：`spec.md` 待确认已清空；正文已决议化。状态 `awaiting-spec-approval` → **`designing`**。调度 **Planner** **更新** `design.md` 与 `plan.md`（覆盖 `.env` + 既有 PG）。运维：代理 `127.0.0.1:7890`；勿提交 `examples/`/真实 `.env`。**不得**自行批准 Plan。
- 2026-07-25 **Planner 更新 Design + Plan 完成**：`design.md` 保留 PG 决策 1–6，新增 `.env` 决策 7–11（CLI 内合并、`--env-file` 替换 CWD、字段级 CLI>env>`.env`）；`plan.md` 为 T1–T7（含 ENV）。Design 门禁 **通过**。状态 `designing` →（planning）→ **`awaiting-plan-approval`**。到达 Plan 用户确认门禁；单步停止；**未**自行批准 Plan、**未**调度 Developer。
- 2026-07-25 **用户确认 Plan（「ok 实施」/「继续」）**：批准更新后 Plan+Design；T1–T7；`.env` 决策；驱动 postgresql **42.7.13**；源分支 **`ggtest-pg`** → 目标 **`main`**；无修改。Manager：Plan → **`approved`**；状态 `awaiting-plan-approval` → **`planned`** → **`developing`**。源/目标分支已记录。调度 **Developer** 在源分支按 TDD 实施 T1–T7（L3：`mvn -q clean test` / `package`），产出 `dev-notes.md`。运维：代理 `127.0.0.1:7890`；勿提交 `examples/`/真实 `.env`。
- 2026-07-25 **Developer 完成**：分支 `ggtest-pg`，commit **`be38ad5`**；T1–T7 交付；`dev-notes.md` 已写。验证：`mvn clean test` → 146 run / 0 fail / 16 skip（无 PG 门控）；`mvn -q clean package` SUCCESS；自备 select1 SQLite 回归 exit 0。未提交 `examples/`/真实 `.env`。状态 `developing` → **`reviewing`**。调度 **Reviewer**。
- 2026-07-25 **Reviewer Approve**：`review.md` 结论 Approve，无阻塞项；独立验证 146/0/16 skip、package SUCCESS、select1 exit 0。缺口：P0-PG-1…4 未实跑（无门控）。状态 `reviewing` → **`qa`**。调度 **QA**。**未**提交 review.md。
- 2026-07-25 **QA 轮次 1 Blocked**：无实现缺陷；缺 PG 门控致 P0-PG-1…4 未证。已通过：mvn 146/0/16 skip、package、ENV P0/P1、select1（1031/0）。`qa-report.md` 已写、**未** commit。状态 `qa` → **`blocked`**。恢复条件见上；**未**请求合并授权。
- 2026-07-25 **恢复 PG 门控**：用户提供本机 localhost postgres（psql -U postgres -d postgres -h localhost -p 5432）。Manager 探测：空密码连接成功；CREATE/DROP SCHEMA 成功。状态 `blocked` → **`qa`**。调度 QA 追加回归轮次（门控 URL/USER；PASSWORD 空；报告中仅写「localhost postgres」占位，**禁止**明文密码）。**未**提交报告。
- 2026-07-25 **QA 轮次 2 Fail**：有门控；146 run / 1 fail / 4 skip。开放 **DEF-PG-001**（conditions fixture 空行）、**DEF-PG-002**（shade JDBC SPI）。ENV/SQLite 回归通过。`qa-report.md` 已追加、**未** commit。状态 `qa` → **`developing`**。调度 Developer 修复；full 须再经 Reviewer Approve 后 QA 轮次 3。
- 2026-07-25 **Developer 修复 DEF-PG-001/002**：commit **`e7e6249`**；门控下 `mvn test` 148/0/5 skip；`./bin/ggtest` PG fixtures exit 0。状态 `developing` → **`reviewing`**。调度 Reviewer 复审。
- 2026-07-25 **Reviewer 复审 Approve（轮次 2）**：`e7e6249`；DEF-PG-001/002 实质修复；门控独立验证 148/0/5；CLI PG fixtures exit 0。`review.md` 已更新、**未** commit。状态 `reviewing` → **`qa`**。调度 QA 轮次 3。
- 2026-07-25 **QA 轮次 3 Pass**：有门控 148/0/5；package SUCCESS；CLI PG fixtures + select1 exit 0；DEF-PG-001/002 **closed**。`qa-report.md`/`review.md` **未** commit（待合并授权后与 STATUS/`done` 一次提交）。状态保持 **`qa`**。请求用户合并授权：源 `ggtest-pg` → 目标 `main`。**未**越过合并门禁。
- 2026-07-25 **合并授权前重新打开测试策略门禁**：当前 `ggtest-pg` 分支 HEAD=`e7e6249`；父会话描述的 `Main.run(..., envLookup, workingDirectory)` / CLI 测试临时目录隔离修复既未提交，也不在当前工作树。根目录运行时 `.env`（`GGTEST_ENGINE=postgres` + PG URL）污染 `MainOrchestrationTest`，实测 `mvn test` 为 148 run / **9 fail** / 15 skip；其中显式 SQLite URL被 `.env` engine 造成硬错配，`missingUrl` 被 `.env` URL 补足。登记 **DEF-PG-003**。门禁判定：仅隔离测试输入并保留 `GGTEST_PG_*` 门控属于既有 Spec/Design 内工程修复；若改变默认 `mvn test` 的引擎选择、自动读取运行时 `.env` 加跑 PG 或要求双引擎矩阵，则属于 Spec/Design 变更。状态 `qa` → **`blocked`**，等待用户选择；A 恢复至 `developing`，B 恢复至 `speccing`。产品 CLI 读取 CWD `.env` 的既定合同不变。`review.md`/`qa-report.md` 继续未提交；禁止提交真实 `.env` 与 `examples/`。
- 2026-07-25 **用户拍板方案 A**：确认「隔离运行时 `.env`，保留 SQLite 必跑及 `GGTEST_PG_*` 门控 PG 测试」。DEF-PG-003 处置为 **A**（工程修复，**无** Spec/Design 变更）：产品 CLI 读 CWD `.env` **不改**；测试通过注入 `envLookup` + 临时 `workingDirectory`（或等价）隔离运行时 `.env` / 进程 `GGTEST_*`；SQLite 基线必跑；`GGTEST_PG_*` 门控时同时跑 PG 套件（既有行为保留）。状态 `blocked` → **`developing`**。源分支 `ggtest-pg`（HEAD `e7e6249`）→ 目标 `main`（已记录）。调度 **Developer** 按 TDD 实现隔离修复（参考未入树思路 `Main.run(..., envLookup, workingDirectory)`；更新 `MainOrchestrationTest` / `CorpusHardAcceptanceTest` / `PostgresCliIntegrationTest` / `EnvConfigIntegrationTest` 等），并更新 `dev-notes.md`。验收关键：根目录**存在**本地 `.env`（postgres）时 `mvn -q clean test` 全绿（无门控 PG skip；有 `GGTEST_PG_*` 则 PG 亦跑）。full 修复须再经 Reviewer `Approve` → QA 回归（含 DEF-PG-003 关闭证据）。运维：代理 `127.0.0.1:7890`，多次失败停止；禁止提交真实 `.env` / `examples/`；`review.md`/`qa-report.md` Pass 待授权前不单独 commit。
- 2026-07-25 **Developer 完成 DEF-PG-003（方案 A）**：源分支 `ggtest-pg` 新增 commit **`b6ea61f`**（未 amend `e7e6249`）。`Main.java` 增可注入重载 `run(args,out,err,envLookup,workingDirectory)`；三参 `run`/`main` 仍用 `System::getenv` + 进程 CWD（产品合同不变）。隔离测试（`key -> null` + `@TempDir`）：`MainOrchestrationTest` / `EnvConfigIntegrationTest` / `CorpusHardAcceptanceTest` / `PostgresCliIntegrationTest`。验证（根目录**存在**本地 `.env`：postgres + PG URL）：无门控 `mvn -q clean test` **148/0/17 skip**；有 `GGTEST_PG_*`（PASSWORD 空）**148/0/5 skip**；`package` SUCCESS；`./bin/ggtest --url jdbc:sqlite::memory: select1` 1031/0 exit 0。提交边界核验：不含 `.env`/`.env.pg`/`examples/`/`workflow/workflow/docs/manager/*`/`review.md`/`qa-report.md`，`.env` 仍被 `.gitignore` 忽略。`dev-notes.md` 已回执。状态 `developing` → **`reviewing`**。调度 **Reviewer** 复审 `b6ea61f`（DEF-PG-003 修复实现/测试隔离/产品合同不变/安全）。**未**提交 `review.md`。
- 2026-07-25 **Reviewer 复审 Approve（轮次 3 / DEF-PG-003）**：`b6ea61f`；无阻塞项。独立验证（根 `.env` postgres 在场）：门控关 `mvn -q clean test` **148/0/17**；门控开（`GGTEST_PG_URL`/`USER`，PASSWORD 空）**148/0/5**，PG CLI/executor/schema 实跑；`package` exit 0；产品 CLI `./bin/ggtest --url jdbc:sqlite::memory:` 在根 `.env` 下硬错配 exit 2（产品合同不变）。要点：`main`/三参 `run` 仍 `System::getenv`+CWD；四类测 `key->null`+`@TempDir`；SQLite 必跑、PG 门控未削弱；无凭据/`.env`/`examples/` 入库。`review.md` 已更新、**未** commit。状态 `reviewing` → **`qa`**。调度 **QA** 回归关闭 DEF-PG-003。
- 2026-07-25 **QA 轮次 4 Pass（DEF-PG-003 closed）**：被测 `b6ea61f`。根目录本地 `.env`（postgres）在场：门控**关** `mvn -q clean test` **148/0/17**；门控**开**（localhost postgres，PASSWORD 空）**148/0/5**，PG executor/schema/cli 实跑；`mvn -q clean package` exit 0；CLI 合同（默认/sqlite、postgres fixtures、未知 engine、错配均 exit 2）无回归；产品读 CWD `.env` 未削弱（根 `.env`+`--url jdbc:sqlite::memory:` → exit 2）；SQLite 硬验收 select1 1031/0 exit 0；ENV P0/P1（临时目录/`--env-file`）通过。**DEF-PG-003 closed**。未验证：非空 `GGTEST_PG_PASSWORD`（本机空密码，P1 由 ENV 临时密码不回显覆盖）；未回退 `e7e6249` 复现。`qa-report.md`/`review.md` 仍 **未 commit**（`??`），暂存区空，无 `.env`/`.env.pg`/`examples/` staged，用户根 `.env` 未改写/删除。状态保持 **`qa`**。**请求用户合并授权**：源 `ggtest-pg`（HEAD `b6ea61f`）→ 目标 `main`。授权后：在源分支置 `done` 并与 STATUS/工作项记录 + 未入库 `review.md`/`qa-report.md` **一次提交**，随后合入 `main`。**未**越过合并门禁。
- 2026-07-25 **用户合并授权（ok）**：确认「授权合并 `ggtest-pg`（HEAD `b6ea61f`）→ `main`：先在源分支置 done 并一次提交 STATUS/工作项记录/`review.md`/`qa-report.md`，再合入 main」。前置核验：Plan `approved`；Reviewer Approve（轮次 3，`b6ea61f`）；QA 轮次 4 **Pass**（DEF-PG-001/002/003 **closed**）；源 `ggtest-pg` → 目标 `main`。状态 `qa` → **`done`**。在源分支**一次提交**纳入 STATUS/`done`、工作项记录、`review.md`、`qa-report.md`（禁止 `.env`/`.env.pg`/`examples/`）。随后调度 **Merge Executor**（QA 兼任）ff-only 合入 `main` 并 push；合入后不得再为 STATUS/报告单独提交。父项**不归档**（用户未要求）。
