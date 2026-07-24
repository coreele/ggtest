# 工作项记录: ggtest-core

工作项标识: ggtest-core
描述: GGTEST——从零到一使用 Java 实现 sqllogictest 格式测试工具。大型 Spec 拆为同目录子 Spec（`spec.md` + `spec-<sub>.md`）；调度主键为 `(ggtest-core, sub-feature-id)`。
路径等级: full
源分支: 不适用（非 Git 工作区，跳过提交与合并操作）
目标分支: 不适用（非 Git 工作区，跳过提交与合并操作）
文档影响: docs/features/ggtest-core/（总览 + 四子 Spec）；实现阶段更新项目 README

> 权威工作流、门禁与状态说明见 [docs/README.md](../README.md)。
> 活跃状态见 [STATUS.md](STATUS.md)。

## 子切片跟踪

| sub-feature-id | Spec | Spec 门禁 | Spec 用户确认 | Design 门禁 | Review 门禁 | 状态 | 后续步骤 |
|---|---|---|---|---|---|---|---|
| ggtest-core | [spec.md](../features/ggtest-core/spec.md) | required（总览） | approved（总览化） | skipped（不对总览写 Design/Plan） | N/A（tracking，不调度 Review） | blocked（tracking） | 跟踪子切片；四切片均 done 后关闭父项 |
| parser | [spec-parser.md](../features/ggtest-core/spec-parser.md) | required | required | required（模块边界、记录模型） | required | awaiting-spec-approval | **建议优先**确认 Spec → Design/Plan |
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
