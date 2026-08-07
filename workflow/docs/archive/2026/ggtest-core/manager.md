# 工作项记录: ggtest-core

工作项标识: ggtest-core
描述: GGTEST——从零到一使用 Java 实现 sqllogictest 格式测试工具。大型 Spec 拆为多子工作项：根目录仅总览 `spec.md`，各切片目录为 `ggtest-core-<sub>/`；调度主键为 `(ggtest-core, sub-feature-id)`。
路径等级: full
源分支: ggtest-core-cli-corpus（cli-corpus 切片工作分支；历史：runner-sqlite=`ggtest-core-runner-sqlite`，normalize=`ggtest-core-normalize`，parser=`ggtest-core-parser`）
目标分支: main
文档影响: workflow/docs/features/ggtest-core/（总览 + 四子工作项目录）；实现阶段更新项目 README

> 权威工作流、门禁与状态说明见 [workflow/README.md](../../../README.md)。
> 活跃状态见 [STATUS.md](../../manager/STATUS.md)。

## 子切片跟踪

| sub-feature-id | Spec | Spec 门禁 | Spec 用户确认 | Design 门禁 | Review 门禁 | 状态 | 后续步骤 |
|---|---|---|---|---|---|---|---|
| ggtest-core | [spec.md](./spec.md) | required（总览） | approved（总览化） | skipped（不对总览写 Design/Plan） | N/A（tracking，不调度 Review） | done（父项已归档） | 已关闭并归档至 `workflow/docs/archive/2026/ggtest-core/`（四切片均 done 且已合入 `main`） |
| parser | [ggtest-core-parser/spec.md](./ggtest-core-parser/spec.md) | required | approved | required（模块边界、记录模型；design.md 已产出） | required | done | 工作流已关闭；源分支 ggtest-core-parser → main（合入以 git 为准） |
| normalize | [ggtest-core-normalize/spec.md](./ggtest-core-normalize/spec.md) | required | approved | skipped（算法已在 Spec 写死） | required | done | 工作流已关闭；源分支 ggtest-core-normalize → main（合入以 git 为准） |
| runner-sqlite | [ggtest-core-runner-sqlite/spec.md](./ggtest-core-runner-sqlite/spec.md) | required | approved | required（执行器抽象、JDBC 分层；design.md 已产出） | required | done | 工作流已关闭；源分支 ggtest-core-runner-sqlite → main（合入以 git 为准） |
| cli-corpus | [ggtest-core-cli-corpus/spec.md](./ggtest-core-cli-corpus/spec.md) | required | approved | skipped（CLI/退出码已在 Spec 写死） | required | done | 工作流已关闭；源分支 ggtest-core-cli-corpus → main（合入以 git 为准） |

依赖：`parser` ∥ `normalize` → `runner-sqlite` → `cli-corpus`。

### 切片验收范围

| sub-feature-id | 验收对齐 |
|---|---|
| parser | P0-7；P1-a/b/c |
| normalize | P0-2、P0-4、P0-5、P1-3 |
| runner-sqlite | P0-3、P0-6、P0-8、P1-2、P1-4 |
| cli-corpus | P0-1、P1-1、P1-5、P1-6 |

### 总览行阻塞

- 阻塞原因: 等待子切片完成（tracking）。总览仅作索引与已决议承载，不对总览调度 Planner。
- 恢复条件: `parser`、`normalize`、`runner-sqlite`、`cli-corpus` 均 `done`（或用户明确取消/关闭）。
- 恢复后的目标状态: done

### cli-corpus 切片阻塞（QA 轮次 1；已恢复）

- 原阻塞原因: 官方语料硬验收 P0-1/P1-5 未执行——`GGTEST_CORPUS_DIR` unset，本机未找到含 `select1.test`/`select2.test`/`select3.test` 的路径；L4 必达项未证实。非实现 Fail（fixtures 项 P1-1/P1-6 与 mvn/package 已 Pass）。证据见 `qa-report.md`。
- 恢复证据: 用户提供 `/Users/zhougangjie/Space/sqllogictest/test`；Manager 已核验 `select1.test`、`select2.test`、`select3.test` 均存在。
- 恢复结果: 当时恢复为 `qa` 并完成 QA 轮次 2；最新状态见下方缺陷记录。

### cli-corpus QA 缺陷（轮次 2）

- 缺陷: `DEF-CLI-001`（高，open）——P1-5 批量执行 select1/2/3 时共享 JDBC 连接导致跨文件库污染，退出码 1、总失败数 4151；单文件均通过。
- 处理: 状态 `qa` → `developing`；Developer 按 Plan T3 修复为每文件独立连接/等价空白库并更新 `dev-notes.md`。
- 复验: 修复后重新取得 Reviewer `Approve`，再由 QA 在同一 `qa-report.md` 追加轮次，复测 P1-5、`mvn test`/`package` 与受影响 fixtures。
- **关闭**: QA 轮次 3 Pass；`DEF-CLI-001` 已验证关闭（实现 `4b9604f`）。

## 用户已确认决策（须由总览 Spec 与各子 Spec 继承，勿重开）

| 决策 | 结论 | 状态 |
|---|---|---|
| 首期目标库 | **SQLite（JDBC）**；其他库（含 PostgreSQL）为首期非目标，扩展点保留 | 已确认 |
| 产品/项目名 | **GGTEST** | 已确认 |
| 工作项 ID | **ggtest-core**；实现按 sub-feature-id 切片 | 已确认 |
| Q1 | Java 17 | 已确认 |
| Q2 | Maven | 已确认 |
| Q3 | CLI 优先 | 已确认 |
| Q4 | 用户自备语料路径 | 已确认 |
| Q5 | hash-threshold 默认 8 | 已确认 |
| Q6 | halt 后记录计为 skipped | 已确认 |
| Q7 | 官方语料在 SQLite 上硬验收失败=0 | 已确认 |
| Q9 | P1-5 硬验收范围为 **select1.test、select2.test、select3.test** | **已决议** |
| Q8 | JDBC 路径坚持「**零豁免硬验收**」；不可消除偏差须再批豁免 | **已决议** |
| 输入后缀 | 除 `.test` 外，**等价看待 `.slt`**；递归目录收集匹配 `*.test` 与 `*.slt`；单文件路径不强制扩展名 | **已确认** |
| 文档结构 | 多子工作项时根目录仅总览 Spec，每切片一目录 `<feature-id>-<sub>/`；单工作项无子目录；STATUS 用 `(feature-id, sub-feature-id)` | **已确认** |

## 审计笔记

- 2026-07-24：产品名与工作项 ID 曾短暂为 JJTEST / `jjtest-core`，同日更名为 GGTEST / `ggtest-core`；首期目标库同日定为 SQLite（JDBC）。旧路径已废弃。
- 2026-07-24：曾短暂登记为四个平级 feature-id；同日改为单一 `ggtest-core` + 同目录子 Spec；STATUS 五行 + `sub-feature-id` 列。

## 进度笔记

- 2026-07-24 登记；路径 full；Spec/Design/Review 门禁均为 required。非 Git 工作区，跳过提交与合并。
- 2026-07-24 Spec 初稿完成；用户确认 Q1–Q7；首期目标库与产品名定为 SQLite / GGTEST。
- 2026-07-24 Analyst 修订 Spec：落实 GGTEST / SQLite / Q1–Q7；新增待确认 Q8、Q9。状态 → `awaiting-spec-approval`。
- 2026-07-24 用户指示：文档去冗余；Q9 认同；Q8 先解释未拍板。Manager → `speccing`，调度 Analyst。
- 2026-07-24 **Analyst 修订完成**：去冗余；Q9 写入已决议（P1-5 = select1/2/3）；Q8 保持开放。Manager：`speccing` → **`awaiting-spec-approval`**。**未**调度 Planner。
- 2026-07-24 用户确认：① **Q8 采纳**零豁免硬验收；② 新增 **`.slt` 与 `.test` 等价**。Manager：→ **`speccing`**，调度 Analyst。
- 2026-07-24 **Analyst 增量修订完成**：Q8 入已决议并从开放问题移除；`.slt`/`.test` 等价写入范围/CLI/合同；验收修订 P1-1、新增 P1-6。Manager：`speccing` → **`awaiting-spec-approval`**。**未**写代码、**未**调度 Planner。
- 2026-07-24 用户反馈验收段文风：按原形式 GWT、勿过多精简、勿过长。Manager 调度 Analyst（只改文风/结构）；状态保持 **`awaiting-spec-approval`**。
- 2026-07-24 **Analyst 文风修订完成**：验收段恢复短标题 + 单条 GWT。**未**调度 Planner。
- 2026-07-24 **用户确认推进拆分**：曾登记四平级子工作项；后改为同目录子 Spec。总览行 → **`blocked`（tracking）**。
- 2026-07-24 **Analyst 完成**：总览 + 四子 Spec 产出（曾位于平级目录）。四切片 → `awaiting-spec-approval`。**未**调度 Planner。
- 2026-07-24 **文档结构修正**：四子 Spec 迁入 `workflow/docs/features/ggtest-core/spec-*.md`；STATUS 增加 `sub-feature-id` 列并省略重复 feature-id；删除平级 feature 目录与独立 manager 记录。
- 2026-07-24 **推进第一个子切片 `parser`（建议优先，依赖 `parser ∥ normalize` 之首）**：核验 `spec-parser.md` 已完成（含记录模型合同、解析错误定位、`.slt`/`.test` 等价语义；P0-7、P1-a/b/c）。门禁判定：Spec=required、Design=required、Review=required。`full` 路径 Spec 必须用户确认。状态维持 **`awaiting-spec-approval`**，到达 **Spec 用户确认门禁**，单步编排下停止；**未**调度 Planner（Design/Plan 待 Spec 确认后）。
- 2026-07-24 **用户确认 parser Spec（回复「ok」，批准、无修改）**。Manager：`parser` 的 Spec 用户确认 → **`approved`**；状态 `awaiting-spec-approval` → **`designing`**（Design 门禁 required）。调度 **Planner** 编写 `design-parser.md`（模块边界、记录模型）→ `plan-parser.md`；到达 **Plan 用户确认门禁**停止。**未**推进 normalize 等其他切片。
- 2026-07-24 **Planner 完成 Design + Plan**：产出 `design-parser.md`（`com.ggtest.model` 纯数据层 + `com.ggtest.parser` 单遍状态机；`sealed interface SqlTestRecord` + record 变体；`SourceLocation`；fail-fast `ParseException` 与 `IOException` 分离以支撑退出码 2）与 `plan-parser.md`（T1 Maven 骨架→T2 模型→T3/T4 TDD 解析与错误→T5 验收 fixtures→T6 文档；验证 `mvn -q clean test`；最低验证层 L2；Review 门禁 required；用户文档/运维 N/A）。Design 门禁 **通过**（design-parser.md 存在）。Manager：`designing` →（planning）→ **`awaiting-plan-approval`**。到达 **Plan 用户确认门禁**，单步编排停止；**未**调度 Developer。默认坐标 `com.ggtest:ggtest:0.1.0-SNAPSHOT`、包名 `com.ggtest`（可在 Plan 确认时调整）。
- 2026-07-24 **用户确认 parser Plan（回复「ok」，批准、无修改；包名 `com.ggtest`、坐标 `com.ggtest:ggtest:0.1.0-SNAPSHOT` 沿用默认）**。Manager：Plan 用户确认结果已持久化；状态 `awaiting-plan-approval` → **`planned`** → **`developing`**。用户授权连续调度 Developer → Reviewer → QA（到 QA Pass 后的关闭/完成授权门禁停）。调度 **Developer** 按已确认 Plan TDD 实施（`mvn -q clean test`，最低验证层 L2），产出代码/测试与 `dev-notes.md`。
- 2026-07-24 **Developer 完成**：T1–T6 已交付（Maven 骨架、model、ParseException、SqlLogicTestParser、fixtures/验收测试、README+Javadoc）。验证：`mvn -q clean test` → BUILD SUCCESS，Tests run: 10, Failures: 0, Errors: 0（L2）。`dev-notes.md` 已写。状态 `developing` → **`reviewing`**。调度 **Reviewer**。
- 2026-07-24 **Reviewer Approve**：`review.md` 结论 Approve，无阻塞项；独立 `mvn -q clean test`（JDK 17）10 tests 全过。状态 `reviewing` → **`qa`**。调度 **QA**。
- 2026-07-24 **QA Pass（轮次 1）**：`qa-report.md` 结论 Pass；独立 `mvn -q clean test`（JDK 17）10/10；P0-7、P1-a/b/c 全过。状态 `qa` → **`awaiting-merge`**。非 Git 工作区：**合并不适用**；到达 **用户关闭/完成授权门禁**，单步编排停止。未关闭、未归档。
- 2026-07-24 **用户授权关闭 parser（回复「ok」）**。Manager：曾按误标「非 Git」将状态置 `done`。
- 2026-07-24 **纠正：本仓库为 Git；实现须独立工作分支**。规范已强化（`workflow/docs/standards/git.md`、Developer/Manager agent、README Merge 门禁）。工作项源分支改为 `ggtest-core-parser`、目标分支 `main`；检出工作分支承接未提交的 parser 实现；状态曾为 **`awaiting-merge`**。后续切片实施前须先建 `<feature-id>-<sub-feature-id>` 分支。
- 2026-07-24 **流程修订：`done` = 待合入即可关闭工作流**（QA Pass + 合并授权；不等合入完成；废弃 `awaiting-merge`）。合入可在 GitHub/本地执行，合入后不再为 STATUS 单独提交。本切片状态 → **`done`**。
- 2026-07-24 **文档结构再修正**：多子工作项改为「根目录仅总览 `spec.md` + 每切片一目录 `<feature-id>-<sub>/`（标准文件名）」；单工作项仍平铺在 feature 根下、无子目录。已迁移 `normalize` / `runner-sqlite` / `cli-corpus` Spec，并将 parser 目录内 `*-parser.md` 重命名为 `spec.md` / `design.md` / `plan.md`。工作流 README、agents、skills、standards 已同步。
- 2026-07-24 **推进切片 `normalize`**：核验 `ggtest-core-normalize/spec.md` 已就绪（I/T/R、nosort/rowsort/valuesort、MD5 逐字节兼容、hash-threshold 默认 8；验收 P0-2/P0-4/P0-5、P1-3；待确认仅「整体 Spec」）。门禁：Spec=required、Design=skipped（算法已在 Spec 写死）、Review=required；Spec 用户确认仍为 **required**（未 approved）。状态维持 **`awaiting-spec-approval`**，到达 **Spec 用户确认门禁**，单步编排停止；**未**调度 Planner。
- 2026-07-24 **用户确认 normalize Spec（回复「ok」，批准、无修改）**。Manager：`normalize` 的 Spec 用户确认 → **`approved`**；Design 门禁 skipped → 状态 `awaiting-spec-approval` → **`planning`**。调度 **Planner** 编写 `ggtest-core-normalize/plan.md`（**不写** Design）。
- 2026-07-24 **Planner 完成 Plan**：产出 `ggtest-core-normalize/plan.md`（T1 I/T/R → T2 排序 → T3 MD5 → T4 比对入口 → T5 验收 fixtures → T6 文档；包 `com.ggtest.normalize`；验证 `mvn -q clean test`；最低验证层 L2；Review required；Design skipped，未建 design.md）。Manager：`planning` → **`awaiting-plan-approval`**。到达 **Plan 用户确认门禁**，单步编排停止；**未**调度 Developer。
- 2026-07-24 **用户确认 normalize Plan（`/manager plan ok`，批准、无修改）**。Manager：Plan 用户确认 → **approved**；源分支记录为 **`ggtest-core-normalize`**、目标分支 **`main`**；状态 `awaiting-plan-approval` → **`planned`** → **`developing`**。调度 **Developer** 按已确认 Plan TDD 实施（检出 `ggtest-core-normalize`，`mvn -q clean test`，L2），产出代码/测试与 `dev-notes.md`。
- 2026-07-24 **运维交接约定（本会话，不改 Spec/Plan）**：网络访问失败时允许使用 HTTP(S) 代理 `127.0.0.1:7890`；多次尝试仍未成功则停止，不得无限重试。供 Developer/工具使用。
- 2026-07-24 **再次调度 Developer（上次调度中断；实现尚未产出）**：确认已在源分支 `ggtest-core-normalize`；Plan approved；传入代理约定后重新调度实施。
- 2026-07-24 **Developer 完成**：T1–T6 已交付（`com.ggtest.normalize`：ValueNormalizer、ResultSorter、ResultHasher、ResultComparer；fixtures；README；`dev-notes.md`）。验证：`mvn -q clean test` → BUILD SUCCESS，Tests run: 36, Failures: 0（normalize 26 + parser 10）。提交 `07a8e51`，分支 `ggtest-core-normalize`。状态 `developing` → **`reviewing`**。调度 **Reviewer**。
- 2026-07-24 **Reviewer Approve**：`review.md` 结论 Approve，无阻塞项；独立 `mvn clean test`（JDK 17）36/36 Pass。状态 `reviewing` → **`qa`**。单步编排停止；**未**调度 QA（完整流程未授权）。
- 2026-07-25 **调度 QA**：核验状态 `qa`、Review Approve、Plan/Spec 已确认、源分支 `ggtest-core-normalize` → `main`。传入验证命令与代理约定后调度 QA 验收。
- 2026-07-25 **QA Pass（轮次 1）**：`qa-report.md` 结论 Pass；独立 `mvn clean test`（JDK 17）36/36；P0-2/P0-4/P0-5/P1-3 全过；缺陷 none。实现 `07a8e51`。状态保持 **`qa`**。到达 **合并/完成授权门禁**；**未**合并、**未**置 `done`。
- 2026-07-25 **用户授权合并/关闭 normalize（回复「ok」）**：Plan approved、Review Approve、QA Pass、源分支 `ggtest-core-normalize`、目标分支 `main` 均已核验。Manager：合并授权已持久化；状态 `qa` → **`done`**（源分支）。调度 Merge Executor 将 `ggtest-core-normalize` 合入 `main`。**未**归档父项 `ggtest-core`（runner-sqlite、cli-corpus 未 done）。
- 2026-07-25 **推进切片 `runner-sqlite`（`/manager 继续 runner-sqlite`）**：核验权威状态与产物。STATUS/`ggtest-core.md` 一致为 **`awaiting-spec-approval`**；路径 `full`；Spec/Design/Review 门禁均为 **required**；Spec 用户确认仍为 **required**（未 approved）。产物仅 `ggtest-core-runner-sqlite/spec.md`（无 design/plan）。上游：`parser`/`normalize` 均为 **`done`** 且已合入 `main`（`eaedcd0` / `07a8e51`+`8569af5`）。Spec 核验就绪：范围（Runner + 执行器抽象 + SQLite JDBC）、合同、验收 P0-3/P0-6/P0-8、P1-2/P1-4；开放问题仅「整体 Spec」待确认；已决议继承总览（SQLite/engine=`sqlite`/Q6/Q8 等）。状态维持 **`awaiting-spec-approval`**，到达 **Spec 用户确认门禁**，单步编排停止；**未**自行批准 Spec、**未**调度 Planner。
- 2026-07-25 **用户确认 runner-sqlite Spec（回复「ok」，批准、无修改）**。Manager：`runner-sqlite` 的 Spec 用户确认 → **`approved`**；状态 `awaiting-spec-approval` → **`designing`**（Design 门禁 required）。调度 **Planner** 编写 `ggtest-core-runner-sqlite/design.md`（执行器抽象、JDBC 分层）→ `plan.md`。运维约定：网络失败可用代理 `127.0.0.1:7890`；多次未成功则停止。**未**自行批准 Plan、**未**改 Spec、**未**实施代码。
- 2026-07-25 **Planner 完成 Design + Plan**：产出 `design.md`（`com.ggtest.db` / `db.sqlite` / `runner` 分层；`org.xerial:sqlite-jdbc`；Runner 禁止依赖 sqlite/`java.sql`）与 `plan.md`（T1–T5；验证 `mvn -q clean test`；最低验证层 L3；建议源分支 `ggtest-core-runner-sqlite`；驱动建议 3.53.2.0）。Design 门禁 **通过**（design.md 存在）。Manager：`designing` →（planning）→ **`awaiting-plan-approval`**。到达 **Plan 用户确认门禁**，单步编排停止；**未**自行批准 Plan、**未**调度 Developer。
- 2026-07-25 **用户确认 runner-sqlite Plan（回复「ok」，批准、无修改；分支 `ggtest-core-runner-sqlite`，sqlite-jdbc 3.53.2.0）**。Manager：Plan 用户确认 → **approved**；源分支记录为 **`ggtest-core-runner-sqlite`**、目标分支 **`main`**；状态 `awaiting-plan-approval` → **`planned`** → **`developing`**。调度 **Developer** 按已确认 Plan TDD 实施 T1–T5（检出源分支，`mvn -q clean test`，L3），产出代码/测试与 `dev-notes.md`。运维：代理 `127.0.0.1:7890`；多次失败则停止。**未**改已批准 Spec/Design/Plan。
- 2026-07-25 **Developer 完成**：T1–T5 已交付（`com.ggtest.db` / `db.sqlite` / `runner`；sqlite-jdbc 3.53.2.0；fixtures；README；`dev-notes.md`）。验证：`mvn -q clean test` → BUILD SUCCESS，Tests run: 84, Failures: 0（新增 48 + parser/normalize 36 无回归）。提交 `e359f05`（design/plan）、`5cf84fc`（实现），分支 `ggtest-core-runner-sqlite`。状态 `developing` → **`reviewing`**。调度 **Reviewer**。
- 2026-07-25 **Reviewer Approve**：`review.md` 结论 Approve，无阻塞项；独立 `mvn clean test`（JDK 17）84/84 Pass；P0-8 核对 runner 无 `db.sqlite`/`java.sql`。状态 `reviewing` → **`qa`**。单步编排停止；**未**调度 QA（完整流程未授权）；**未**请求合并授权。
- 2026-07-25 **调度 QA**：核验状态 `qa`、Review Approve、Plan/Spec 已确认、源分支 `ggtest-core-runner-sqlite` → `main`、实现 `5cf84fc`。传入验证命令与代理约定后调度 QA 验收 P0-3/P0-6/P0-8/P1-2/P1-4。
- 2026-07-25 **QA Pass（轮次 1）**：`qa-report.md` 结论 Pass；独立 `mvn clean test`（JDK 17）84/84；P0-3/P0-6/P0-8/P1-2/P1-4 全过；缺陷 none。实现 `5cf84fc`。状态保持 **`qa`**。到达 **合并/完成授权门禁**；**未**合并、**未**置 `done`。
- 2026-07-25 **用户授权合并/关闭 runner-sqlite（回复「ok」）**：Plan approved、Review Approve、QA Pass、源分支 `ggtest-core-runner-sqlite`、目标分支 `main` 均已核验。Manager：合并授权已持久化；状态 `qa` → **`done`**（源分支）。调度 Merge Executor 将 `ggtest-core-runner-sqlite` 合入 `main`。**未**归档父项 `ggtest-core`（cli-corpus 未 done）。
- 2026-07-25 **推进切片 `cli-corpus`（`继续 cli-corpus`）**：核验权威状态与产物。STATUS/`ggtest-core.md` 一致为 **`awaiting-spec-approval`**；路径 `full`；Spec 门禁 **required**；Spec 用户确认仍为 **required**（未 approved）；Design 门禁 **skipped**（CLI/退出码已在 Spec 写死）；Review 门禁 **required**。产物仅 `ggtest-core-cli-corpus/spec.md`（无 design/plan）。上游：`parser`/`normalize`/`runner-sqlite` 均为 **`done`** 且已合入 `main`（main HEAD `5a61e2d`，含 `5cf84fc`）。Spec 核验就绪：范围（CLI `ggtest`、统计报告、退出码 0/1/2、目录递归 `*.test`/`*.slt`、官方语料零豁免硬验收）、合同、验收 P0-1 / P1-1 / P1-5 / P1-6；开放问题仅「整体 Spec」待确认；已决议继承总览（Q4/Q5/Q7/Q8/Q9、`.slt` 等价等）。状态维持 **`awaiting-spec-approval`**，到达 **Spec 用户确认门禁**，单步编排停止；**未**自行批准 Spec、**未**调度 Planner。
- 2026-07-25 **用户确认 cli-corpus Spec（回复「ok」，批准、无修改）**。Manager：`cli-corpus` 的 Spec 用户确认 → **`approved`**；Design 门禁 skipped → 状态 `awaiting-spec-approval` → **`planning`**。调度 **Planner** 编写 `ggtest-core-cli-corpus/plan.md`（**不写** Design）。运维约定：网络失败可用代理 `127.0.0.1:7890`；多次未成功则停止。**未**自行批准 Plan、**未**改 Spec、**未**实施代码。
- 2026-07-25 **Planner 完成 Plan**：产出 `ggtest-core-cli-corpus/plan.md`（T1–T6；包 `com.ggtest.cli` / `Main`；L4；建议源分支 `ggtest-core-cli-corpus`；Design skipped，未建 design.md；已 documentation.md §B 自检）。Manager：`planning` → **`awaiting-plan-approval`**。到达 **Plan 用户确认门禁**，单步编排停止；**未**自行批准 Plan、**未**调度 Developer。
- 2026-07-25 **用户确认 cli-corpus Plan（回复「ok」，批准、无修改；分支 `ggtest-core-cli-corpus` → `main`）**。Manager：Plan 用户确认 → **approved**；源分支记录为 **`ggtest-core-cli-corpus`**、目标分支 **`main`**；状态 `awaiting-plan-approval` → **`planned`** → **`developing`**。调度 **Developer** 按已确认 Plan TDD 实施 T1–T6（检出源分支，`mvn -q clean test` / `package`，L4），产出代码/测试与 `dev-notes.md`。运维：代理 `127.0.0.1:7890`；多次失败则停止。官方语料用户自备；无语料时按 Plan 记录证据与恢复条件，不得默示豁免。**未**改已批准 Spec/Plan。
- 2026-07-25 **Developer 完成**：T1–T6 已交付（`com.ggtest.cli`；`bin/ggtest`；fixtures；README；`dev-notes.md`）。验证：`mvn -q clean test` → BUILD SUCCESS，Tests run: 110, Failures: 0, Errors: 0, Skipped: 3；`mvn -q clean package` SUCCESS；`./bin/ggtest` 最小 fixtures 退出码 0。提交 `0ed8a95`（plan/manager）、`466c6f1`（实现），分支 `ggtest-core-cli-corpus`。**P0-1/P1-5 官方语料硬验收未执行**（无 `GGTEST_CORPUS_DIR`），原因/风险/恢复条件已记入 `dev-notes.md`，未默示豁免。状态 `developing` → **`reviewing`**。调度 **Reviewer**。
- 2026-07-25 **Reviewer Approve**：`review.md` 结论 Approve，无阻塞项；独立 `mvn clean test`（JDK 17）110/110 Pass（Skipped: 3）；`package` + `bin/ggtest` smoke 退出码 0/1/2 符合合同。硬验收缺口正确记为未执行（非豁免）。状态 `reviewing` → **`qa`**。单步编排停止；**未**调度 QA（完整流程未授权）；**未**请求合并授权。
- 2026-07-25 **调度 QA**：核验状态 `qa`、Review Approve（`review.md`）、Plan/Spec 已确认、源分支 `ggtest-core-cli-corpus` → `main`、实现 `466c6f1`；已提交 `3376b5a`（review + STATUS/qa）。环境：`GGTEST_CORPUS_DIR` 未设，常见本地路径未找到 `select1.test`。传入验证命令与代理约定后调度 QA 验收 P0-1/P1-1/P1-5/P1-6（L4）；无语料时不得默示豁免。
- 2026-07-25 **QA Blocked（轮次 1）**：`qa-report.md` 结论 **Blocked**；独立 `mvn clean test` 110/0（Skipped 3）、`package` SUCCESS；P1-1/P1-6 Pass；P0-1/P1-5 未执行（无语料），**未**默示豁免、无 Fail 缺陷。状态 `qa` → **`blocked`**。恢复条件见「cli-corpus 切片阻塞」；**未**请求合并授权。
- 2026-07-25 **恢复 QA**：用户提供官方语料目录 `/Users/zhougangjie/Space/sqllogictest/test`；Manager 核验 `select1.test`、`select2.test`、`select3.test` 均存在，恢复条件满足。状态 `blocked` → **`qa`**；调度 QA 设置 `GGTEST_CORPUS_DIR` 追加回归轮次，复测 P0-1/P1-5 及 fixtures。**未**请求合并授权。
- 2026-07-25 **QA Fail（轮次 2）**：P0-1/P1-1/P1-6 Pass；P1-5 Fail——批量 select1/2/3 退出码 1、`TOTAL failed=4151`，单文件均 exit 0/failed 0。`mvn -q clean test` / `package` 因 P1-5 自动化失败而退出 1。登记高严重度缺陷 `DEF-CLI-001`（共享 JDBC 导致跨文件库污染）。状态 `qa` → **`developing`**；后续 Developer 修复 → Reviewer 重新 Approve → QA 回归。**未**请求合并授权。
- 2026-07-25 **调度 Developer 修复 DEF-CLI-001**：核验 `qa-report.md` 轮次 2 Fail；复测范围 P1-5 + `mvn test`/`package` + fixtures；语料 `/Users/zhougangjie/Space/sqllogictest/test`。要求：按 Spec/Plan T3 每文件独立连接（或等价空白库）并重置 hash-threshold 等运行时状态；零豁免；更新 `dev-notes.md` 并验证 P0-1/P1-5。状态保持 **`developing`**。
- 2026-07-25 **Developer 修复完成（DEF-CLI-001）**：`CliSession` 改为每文件独立 JDBC 连接→run→关闭；新增跨文件 schema 隔离测试。验证：`GGTEST_CORPUS_DIR=… mvn -q clean test` → 111/0（Skipped 1）；`package` SUCCESS；P0-1 exit 0 failed=0；P1-5 批量 exit 0、TOTAL failed=0（passed=5413）。提交 `4b9604f`。`dev-notes.md` 已更新。状态 `developing` → **`reviewing`**。调度 **Reviewer** 重新 Approve。
- 2026-07-25 **Reviewer Approve（修复轮次）**：`review.md` 追加轮次，结论 Approve；独立 `mvn test` 111/0、`package` SUCCESS；P1-5 批量 exit 0、TOTAL failed=0。状态 `reviewing` → **`qa`**。调度 **QA** 追加回归轮次（P1-5 为主 + fixtures）。**未**请求合并授权。
- 2026-07-25 **QA Pass（轮次 3，回归）**：`qa-report.md` 结论 Pass；`DEF-CLI-001` 已验证关闭；P0-1/P1-1/P1-5/P1-6 全过；`mvn test` 111/0、`package` SUCCESS；P1-5 批量 exit 0、TOTAL failed=0（passed=5413）。实现 `4b9604f`。状态保持 **`qa`**。到达 **合并/完成授权门禁**；**未**合并、**未**置 `done`。
- 2026-07-25 **用户授权合并/关闭 cli-corpus（回复「ok 允许合并」）**：Plan approved、Review Approve（修复轮次）、QA Pass（轮次 3）、源分支 `ggtest-core-cli-corpus`、目标分支 `main`（merge-base = main HEAD `5a61e2d`，可 ff）均已核验。Manager：合并授权已持久化；状态 `qa` → **`done`**（源分支）。调度 Merge Executor 将 `ggtest-core-cli-corpus` 合入 `main`（ff-only + push）。工作区 untracked `examples/`（用户演示 demo.slt）**不纳入**本切片提交，保留未提交。四切片均 `done` 后父项达到可关闭条件，但用户未明确要求归档，**不**归档。
- 2026-07-25 **用户授权归档父项（「归档 ggtest-core」）**：核验四切片 `parser`/`normalize`/`runner-sqlite`/`cli-corpus` 均 `done` 且实现均为 `main`（=`origin/main`=`b635c08`）祖先（`eaedcd0`/`07a8e51`/`5cf84fc`/`4b9604f`/`466c6f1` 均在 `main`）。归档执行：① tracking 行 `blocked（tracking）` → **`done`**；② 从 `STATUS.md` 活跃列表移除、归档区登记；③ `git mv workflow/docs/features/ggtest-core/` → **`workflow/docs/archive/2026/ggtest-core/`**；④ 本记录内失效 `../features/ggtest-core/` 链接改指归档路径。`examples/`（`demo.slt`、`select*.test`）为未跟踪演示/语料，**不纳入**提交、保持原样。在 `main` 提交并推送归档变更。父项工作流关闭。
