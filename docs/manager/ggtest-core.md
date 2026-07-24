# 工作项记录: ggtest-core

工作项标识: ggtest-core
描述: GGTEST——从零到一使用 Java 实现 sqllogictest 格式测试工具。大型 Spec 拆为同目录子 Spec（`spec.md` + `spec-<sub>.md`）；调度主键为 `(ggtest-core, sub-feature-id)`。
路径等级: full
源分支: ggtest-core-parser（parser 切片工作分支；后续切片各自独立分支）
目标分支: main
文档影响: docs/features/ggtest-core/（总览 + 四子 Spec）；实现阶段更新项目 README

> 权威工作流、门禁与状态说明见 [docs/README.md](../README.md)。
> 活跃状态见 [STATUS.md](STATUS.md)。

## 子切片跟踪

| sub-feature-id | Spec | Spec 门禁 | Spec 用户确认 | Design 门禁 | Review 门禁 | 状态 | 后续步骤 |
|---|---|---|---|---|---|---|---|
| ggtest-core | [spec.md](../features/ggtest-core/spec.md) | required（总览） | approved（总览化） | skipped（不对总览写 Design/Plan） | N/A（tracking，不调度 Review） | blocked（tracking） | 跟踪子切片；四切片均 done 后关闭父项 |
| parser | [spec-parser.md](../features/ggtest-core/spec-parser.md) | required | approved | required（模块边界、记录模型；design-parser.md 已产出） | required | done | 工作流已关闭；源分支 ggtest-core-parser → main（合入以 git 为准） |
| normalize | [spec-normalize.md](../features/ggtest-core/spec-normalize.md) | required | required | skipped（算法已在 Spec 写死） | required | awaiting-spec-approval | 确认 Spec → Plan |
| runner-sqlite | [spec-runner-sqlite.md](../features/ggtest-core/spec-runner-sqlite.md) | required | required | required（执行器抽象、JDBC 分层） | required | awaiting-spec-approval | 确认 Spec；上游就绪后再 Design |
| cli-corpus | [spec-cli-corpus.md](../features/ggtest-core/spec-cli-corpus.md) | required | required | skipped（CLI/退出码已在 Spec 写死） | required | awaiting-spec-approval | 确认 Spec；最后集成 |

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
| 文档结构 | 同目录语义子 Spec（`spec-<sub>.md`）；STATUS 用 `(feature-id, sub-feature-id)` | **已确认** |

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
- 2026-07-24 **文档结构修正**：四子 Spec 迁入 `docs/features/ggtest-core/spec-*.md`；STATUS 增加 `sub-feature-id` 列并省略重复 feature-id；删除平级 feature 目录与独立 manager 记录。
- 2026-07-24 **推进第一个子切片 `parser`（建议优先，依赖 `parser ∥ normalize` 之首）**：核验 `spec-parser.md` 已完成（含记录模型合同、解析错误定位、`.slt`/`.test` 等价语义；P0-7、P1-a/b/c）。门禁判定：Spec=required、Design=required、Review=required。`full` 路径 Spec 必须用户确认。状态维持 **`awaiting-spec-approval`**，到达 **Spec 用户确认门禁**，单步编排下停止；**未**调度 Planner（Design/Plan 待 Spec 确认后）。
- 2026-07-24 **用户确认 parser Spec（回复「ok」，批准、无修改）**。Manager：`parser` 的 Spec 用户确认 → **`approved`**；状态 `awaiting-spec-approval` → **`designing`**（Design 门禁 required）。调度 **Planner** 编写 `design-parser.md`（模块边界、记录模型）→ `plan-parser.md`；到达 **Plan 用户确认门禁**停止。**未**推进 normalize 等其他切片。
- 2026-07-24 **Planner 完成 Design + Plan**：产出 `design-parser.md`（`com.ggtest.model` 纯数据层 + `com.ggtest.parser` 单遍状态机；`sealed interface SqlTestRecord` + record 变体；`SourceLocation`；fail-fast `ParseException` 与 `IOException` 分离以支撑退出码 2）与 `plan-parser.md`（T1 Maven 骨架→T2 模型→T3/T4 TDD 解析与错误→T5 验收 fixtures→T6 文档；验证 `mvn -q clean test`；最低验证层 L2；Review 门禁 required；用户文档/运维 N/A）。Design 门禁 **通过**（design-parser.md 存在）。Manager：`designing` →（planning）→ **`awaiting-plan-approval`**。到达 **Plan 用户确认门禁**，单步编排停止；**未**调度 Developer。默认坐标 `com.ggtest:ggtest:0.1.0-SNAPSHOT`、包名 `com.ggtest`（可在 Plan 确认时调整）。
- 2026-07-24 **用户确认 parser Plan（回复「ok」，批准、无修改；包名 `com.ggtest`、坐标 `com.ggtest:ggtest:0.1.0-SNAPSHOT` 沿用默认）**。Manager：Plan 用户确认结果已持久化；状态 `awaiting-plan-approval` → **`planned`** → **`developing`**。用户授权连续调度 Developer → Reviewer → QA（到 QA Pass 后的关闭/完成授权门禁停）。调度 **Developer** 按已确认 Plan TDD 实施（`mvn -q clean test`，最低验证层 L2），产出代码/测试与 `dev-notes.md`。
- 2026-07-24 **Developer 完成**：T1–T6 已交付（Maven 骨架、model、ParseException、SqlLogicTestParser、fixtures/验收测试、README+Javadoc）。验证：`mvn -q clean test` → BUILD SUCCESS，Tests run: 10, Failures: 0, Errors: 0（L2）。`dev-notes.md` 已写。状态 `developing` → **`reviewing`**。调度 **Reviewer**。
- 2026-07-24 **Reviewer Approve**：`review.md` 结论 Approve，无阻塞项；独立 `mvn -q clean test`（JDK 17）10 tests 全过。状态 `reviewing` → **`qa`**。调度 **QA**。
- 2026-07-24 **QA Pass（轮次 1）**：`qa-report.md` 结论 Pass；独立 `mvn -q clean test`（JDK 17）10/10；P0-7、P1-a/b/c 全过。状态 `qa` → **`awaiting-merge`**。非 Git 工作区：**合并不适用**；到达 **用户关闭/完成授权门禁**，单步编排停止。未关闭、未归档。
- 2026-07-24 **用户授权关闭 parser（回复「ok」）**。Manager：曾按误标「非 Git」将状态置 `done`。
- 2026-07-24 **纠正：本仓库为 Git；实现须独立工作分支**。规范已强化（`docs/standards/git.md`、Developer/Manager agent、README Merge 门禁）。工作项源分支改为 `ggtest-core-parser`、目标分支 `main`；检出工作分支承接未提交的 parser 实现；状态曾为 **`awaiting-merge`**。后续切片实施前须先建 `<feature-id>-<sub-feature-id>` 分支。
- 2026-07-24 **流程修订：`done` = 待合入即可关闭工作流**（QA Pass + 合并授权；不等合入完成；废弃 `awaiting-merge`）。合入可在 GitHub/本地执行，合入后不再为 STATUS 单独提交。本切片状态 → **`done`**。
