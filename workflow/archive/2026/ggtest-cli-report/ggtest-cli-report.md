# 工作项: ggtest-cli-report

描述: 优化 GGTEST CLI 执行报告的可读性。当前报告为单行紧凑机器格式：成功时 `FILE: <path> passed=.. failed=.. skipped=..` + `TOTAL: ...`；失败时把 expected/actual 差异挤在单行 `FAILURE: file=.. line=.. summary=.. reason=..`。用户反馈「正确信息与报错提示都很不友好」，要求优化成功与失败两种输出。本项改动为**用户可见输出合同变更**（归档 cli-corpus Spec 规定了 file/line/SQL 首行/reason 合同），故走 Spec 门禁。退出码 0/1/2 语义默认不变。
目标分支: main
源分支: ggtest-cli-report
基线提交: a6c8719bc48099cf772a6bd1807876dd4577259c
文档影响: workflow/archive/2026/ggtest-cli-report/spec.md；后续 plan/dev-notes/review/qa；可能涉及 README CLI 输出示例、既有报告断言测试（`CorpusHardAcceptanceTest`/`MainOrchestrationTest`/`EnvConfigIntegrationTest`/`ResultComparerTest`）；**计数语义改为按文件**（非 query）

> **本文件须保存为 `workflow/archive/2026/ggtest-cli-report/ggtest-cli-report.md`**，文件名与目录同名。
> 流程定义见 `workflow/WORKFLOW.md`；看板见 `workflow/STATUS.md`。
> 本工作项的全部产物平铺在 `workflow/archive/2026/ggtest-cli-report/`，无子目录、无版本后缀。
> 表内只填枚举、短标签或路径；理由与长说明写进「进度笔记」。

## 门禁

| 路径等级 | Spec | Spec 用户确认 | Design | Review |
|---|---|---|---|---|
| standard | skipped | not-required | skipped | required |

## 状态

| 状态 | 下一步 | 阻塞原因 | 恢复条件 | 恢复后目标 |
|---|---|---|---|---|
| archived | — |  |  |  |

## 子项（仅 tracking 项填写）

| 子项 id | 状态 |
|---|---|
| — | |

## 进度笔记

- 2026-07-25 **登记**：用户指令 `/manager` + 反馈 CLI 报告不友好，要求优化成功/失败输出。feature-id=`ggtest-cli-report`；路径 `standard`；Spec `required`、Spec 用户确认 `required`；Design `skipped`；Review `required`；未拆分。源分支 `ggtest-cli-report` → 目标 `main`。状态 `backlog` → **`speccing`**。调度 **Analyst**。
- 2026-07-25 **Analyst 完成 Spec**：产出 `spec.md`（已 refine-docs）。开放问题 Q-R1…Q-R8。状态 → **`awaiting-spec-approval`**。
- 2026-07-25 **状态查询核验**：仍为 `awaiting-spec-approval`；仅有 `spec.md`。
- 2026-07-25 **部分开放问题决议**：Q-R5=(B)，偏离 Analyst (A)；其余未决。
- 2026-07-25 **Spec 全部确认**：用户确认 Q-R1…Q-R8 与 Spec（样例参考 sqllogictest-rs）。**Spec 用户确认 → approved**。要点：相对 CWD；成功无 `FILE:` + 耗时 + **按文件计数**；失败 `[WHY]/[SQL]/[Diff]` git 风格；彩色 + 非 TTY 自动降级；Q-R5=(B)；退出码不变；失败内联 + 末尾 Error 文件列表 + TOTAL；硬错误同视觉体系。因初稿样例/计数语义与决议不一致，**先调度 Analyst 回写 `spec.md`**，再 Planner；状态 `awaiting-spec-approval` → **`speccing`（决议回写）**。**未** commit、**未**写实现。
- 2026-07-25 **Analyst 决议回写完成**：`workflow/archive/2026/ggtest-cli-report/spec.md` 已按 Q-R1…Q-R8 与用户样例冻结合同（含按文件计数、无 FILE:、git Diff、彩色自动降级、Q-R5=(B)），已 refine-docs；开放问题「无（全部已决）」。Spec 门禁通过且合同已与批准决议一致。状态 `speccing` → **`planning`**。调度 **Planner** 编写 `plan.md`（Design skipped；Plan 须含「识别并修改受影响测试」及按文件计数/彩色/git Diff 任务）。**未** commit、**未**写实现。
- 2026-07-25 **Planner 完成 Plan**：产出 `workflow/archive/2026/ggtest-cli-report/plan.md`（已 refine-docs）。任务 T1–T8；**T6 = 识别并修改受影响测试**（Q-R5=(B) 必做）。验证层 L3。Plan 列出待用户确认的推导项：① 文件级计数边界（failed/passed/skipped/硬错误不计三者）；② 无文件上下文的用法错误多行样式。状态 `planning` → **`awaiting-plan-approval`**。到达 Plan 用户确认门禁；**未**自行批准 Plan、**未**调度 Developer、**未** commit。
- 2026-07-25 **Q-R4 修订（Plan 未批准，用户 modify）**：覆盖「仅自动探测、无 --color」→ 需要彩色 + `--color <auto|always|never>`（默认 auto）+ env `CARGO_TERM_COLOR`；优先级 CLI > env > auto。Spec 保持 **approved**（增量修订已由用户拍板，不退回 Spec 确认门禁；并入 Plan 确认）。状态保持 **`awaiting-plan-approval`**。调度 Analyst 回写 Spec → Planner 修订 Plan。**不**调度 Developer、**不** commit。
- 2026-07-25 **Q-R4 回写完成**：Analyst 已更新 `spec.md`（`--color` / `CARGO_TERM_COLOR` / 优先级 / P1-4·P1-5；非目标改为除 `--color` 外不新增其他标志）；Planner 已修订 `plan.md`（T4/T6/T7、触碰路径、确认事项第 3 项=接受 Q-R4 修订）。状态仍 **`awaiting-plan-approval`**。待用户确认 Plan（三项：文件计数边界、用法错误样式、Q-R4 修订）。**未**调度 Developer、**未** commit。
- 2026-07-25 **Plan 确认意见修订**：① 硬错误**计入** `TOTAL.failed`（覆盖「不计入三者」）；退出码与计数独立（硬错误仍码 2）。② 用法/配置错误样式用户确认「可」。③ env 改名 **`GGTEST_TERM_COLOR`**（可再改；废止 `CARGO_TERM_COLOR`）。状态保持 **`awaiting-plan-approval`**。调度 Analyst → Planner 回写；**请用户再审 Plan**。**不**调度 Developer、**不** commit。
- 2026-07-25 **Plan 意见回写完成**：Analyst 已更新 `spec.md`（计数边界专节 + `GGTEST_TERM_COLOR` + P1-3）；Planner 已修订 `plan.md`（T1/T4/T5/T6/T7、确认事项改为「已反映用户修改、仍待最终批准」）。状态仍 **`awaiting-plan-approval`**。**已按意见修改，请用户再审核 Plan**。**未**调度 Developer、**未** commit。
- 2026-07-25 **Q-R4 Java 生态命名**：用户要求符合 Java 惯例。env → **`GGTEST_COLOR`**；新增系统属性 **`ggtest.color`**（`-Dggtest.color=…`）。优先级：CLI `--color` > `ggtest.color` > `GGTEST_COLOR` > 默认 `auto`。弃用 `GGTEST_TERM_COLOR` / `CARGO_TERM_COLOR`。状态保持 **`awaiting-plan-approval`**。调度 Analyst → Planner；**请用户再审 Plan**。**不**调度 Developer、**不** commit。
- 2026-07-25 **Java 彩色命名回写完成**：Analyst/`spec.md` 与 Planner/`plan.md` 已改为 `GGTEST_COLOR` + `ggtest.color` 与四级优先级；确认事项第 3 项已更新。状态仍 **`awaiting-plan-approval`**。**请用户再审 Plan**。**未**调度 Developer、**未** commit。
- 2026-07-25 **状态行标签增量**：成功 → `.. [OK] in N ms`；失败介词一律 `in`（废 `after`）；skipped → `.. [SKIPPED]`（无耗时）；补混合场景样例（Error 仅失败文件）。状态保持 **`awaiting-plan-approval`**。调度 Analyst → Planner；**请用户再审 Plan**。**不**调度 Developer、**不** commit。
- 2026-07-25 **状态行回写完成**：Analyst/`spec.md` 与 Planner/`plan.md` 已同步 `[OK]`/`[FAILED] in`/`[SKIPPED]`、混合样例与 P1-1。状态仍 **`awaiting-plan-approval`**。**请用户再审 Plan**。**未**调度 Developer、**未** commit。
- 2026-07-25 **成功标签 `[OK]`→`[PASSED]`**：与 `TOTAL.passed=` 成对（FAILED/failed、SKIPPED/skipped 已成对）。状态保持 **`awaiting-plan-approval`**。调度 Analyst → Planner；**请用户再审 Plan**。**不**调度 Developer、**不** commit。
- 2026-07-25 **`[PASSED]` 回写完成**：Analyst/`spec.md` 与 Planner/`plan.md` 已统一成功标签为 `[PASSED]`（禁止 `[OK]`）。状态仍 **`awaiting-plan-approval`**。**请用户批准 Plan**。**未**调度 Developer、**未** commit。
- 2026-07-25 **Plan 用户批准**：用户在多轮修订后回复「ok」；结合此前明确提示「确认 Plan 没问题后回复『批准 Plan』」，视为正式批准 `workflow/archive/2026/ggtest-cli-report/plan.md`（确认事项 1–4 全部接受）。Plan 用户确认 → **approved**。状态 `awaiting-plan-approval` → **`planned`** → 立即调度 Developer → **`developing`**。源分支 `ggtest-cli-report` → 目标 `main`。约束：TDD；L3（`mvn -q test` + package + jar 端到端）；写 `dev-notes.md`；**禁止** commit/merge；**禁止**提交 `examples/` 未跟踪样例与 `.env`；不得自行改变冻结合同。完成后调度 Reviewer；Approve 后调度 QA。
- 2026-07-25 **Developer 完成 T1–T8**：源分支 `ggtest-cli-report`；产出含 CliSession/报告着色/ResultComparer Diff、受影响测试、README、`dev-notes.md`。验证：`mvn -q test` 通过；`mvn -q -DskipTests package` 通过；jar E2E（成功/失败/混合/硬错误与彩色优先级）通过；TTY `auto` 真机彩色为缺口声明（见 dev-notes）。合同偏差 none。**未** commit/merge。状态 `developing` → **`reviewing`**。调度 **Reviewer**。
- 2026-07-25 **Reviewer Approve**：产出 `workflow/archive/2026/ggtest-cli-report/review.md`；阻塞项 none；L3 独立复验通过。状态 `reviewing` → **`qa`**。调度 **QA**。**未**单独提交 review.md。
- 2026-07-25 **QA Pass**：产出 `workflow/archive/2026/ggtest-cli-report/qa-report.md`；缺陷 none；L3 独立验收通过（`mvn test` / package / jar E2E）。状态保持 **`qa`（Pass；待合并授权）**。**未**单独提交 qa-report.md / review.md；**未** merge。到达合并用户确认门禁：源分支 `ggtest-cli-report` → 目标 `main`。用户明确授权后：Manager 在源分支置 `done` 并与未入库报告一次提交，再允许合入。
- 2026-07-25 **合入前视觉小修（非新工作项）**：用户本地反馈状态行路径与 `..` 间距过窄（仅按最长路径对齐，短路径几乎贴合）。**修复合同**：路径列宽 = `max(本次最长路径长度, 60)`（对齐 Spec 冻结样例约 60 字符列宽；路径 ≥60 不截断）；不改 diff/计数/彩色/退出码；不改 Spec 样例。Plan 正文原写「按最长路径对齐」无下限——本轮以 Manager 台账记澄清/修复，不退回 Plan 确认门禁。状态 `qa` → **`developing`（合入前间距小修）**。调度 Developer；完成后短 Review + QA 回归；目标仍回到合并授权。**禁止** commit/merge。
- 2026-07-25 **间距小修实现完成**：`STATUS_PATH_COLUMN_WIDTH=60`；`mvn -q test` 通过；dev-notes 已记。状态 → **`reviewing`（合入前间距小修）**。调度短 **Reviewer**。
- 2026-07-25 **短审 Approve（轮次 2）**：`review.md` 已追加；阻塞 none。状态 → **`qa`（间距小修回归）**。调度 **QA** 回归。
- 2026-07-25 **QA 回归 Pass（轮次 2）**：`qa-report.md` 已追加；列宽下限 60 与标签合同核对通过；`mvn -q test` / jar 冒烟通过。状态回到 **`qa`（Pass；待合并授权）**。**未** commit/merge。仍待用户授权合入 `main`。
- 2026-07-25 **合入前小修（`[SQL]` 多行省略；非新工作项）**：用户反馈失败块 `[SQL]` 只显示首行，多行 SQL（如 `SELECT name`\\n`FROM items`）易误解为单行。**合同（用户已拍板，作 Spec 增量批准）**：取首行（去尾随空白）；若去除首行后仍有非空白内容，则显示为 `<首行> ...`；纯单行不加 ` ...`。不改 diff/计数/彩色/退出码/路径列宽。状态 `qa` → **`speccing`（合入前 SQL 省略标记）**。调度 **Analyst** 回写 Spec（样例与 fixture 对齐：`[SQL] SELECT name ...`）；再 **Planner** 修订 Plan（T2/T6）；再 Developer；短 Review + QA 回归后回到合并授权。**禁止** commit/merge。不退回整份 Spec/Plan 确认门禁（增量合同已由本轮用户指令明确）。
- 2026-07-25 **Analyst Spec 增量回写完成**：`spec.md` 已含 `[SQL]` 多行 `<首行> ...` 合同与样例对齐。状态 → **`planning`**。调度 **Planner**（不另开 Plan 用户确认门禁）。
- 2026-07-25 **Planner Plan 修订完成**：T2/T6 已含 `[SQL]` 省略规则。状态 → **`developing`（合入前 SQL 省略标记）**。调度 **Developer**。
- 2026-07-25 **Developer `[SQL]` 省略完成**：`sqlFirstLine` 多行追加 ` ...`；测试/README/dev-notes 已更；`mvn -q test` 通过。状态 → **`reviewing`**。调度短 **Reviewer**。
- 2026-07-25 **短审 Approve（轮次 3）**：`review.md` 已追加；阻塞 none。状态 → **`qa`（SQL 省略回归）**。调度 **QA**。
- 2026-07-25 **QA 回归 Pass（轮次 3）**：`qa-report.md` 已追加；多行 `[SQL] … ...`、单行无省略核对通过；`mvn -q test` / package / jar 冒烟通过。状态回到 **`qa`（Pass；待合并授权）**。**未** commit/merge。
- 2026-07-25 **用户授权合入**：当前用户会话明确授权将源分支 `ggtest-cli-report` 合入 `main`。合并前置条件核验通过：Plan approved、Review Approve（轮次 3）、QA Pass（轮次 3）、源/目标分支已记录。状态 `qa` → **`done`**。Manager 在源分支执行：① `feat` 提交实现/测试/fixtures/README/spec/plan/dev-notes；② `docs` 一次提交 STATUS/`done`、本台账、`review.md`、`qa-report.md`。**排除**：`.env`（gitignore 覆盖）、`examples/` 未跟踪语料、`.gitignore` 本地工具忽略项改动、architecture-overview 工作项文档（仅 STATUS 登记行随治理文件入库）。随后由 QA 兼任 Merge Executor fast-forward 合入 `main`（不 push 远端），再归档。遗留缺口：TTY 下 `auto` 真机彩色未验证（低风险，见 dev-notes §缺口）。
- 2026-07-25 **合入并归档**：`main` fast-forward 至 `c67184a`（`529792e` feat + `c67184a` workflow/done）。用户明确要求关闭并归档：`workflow/archive/2026/ggtest-cli-report/` → `workflow/archive/2026/ggtest-cli-report/`；STATUS 活跃列表移除本项并记入归档区。工作项关闭。未 push 远端（用户未要求）。
- 2026-08-14：按 ggnote `WORKFLOW.md` 标准迁移工作流目录（记录与产物合并为同一目录；权威文件改为 `workflow/WORKFLOW.md`）。
