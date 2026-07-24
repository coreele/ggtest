# 工作项记录: ggtest-core

工作项标识: ggtest-core
描述: 【Epic / 总览】GGTEST——从零到一使用 Java 实现 sqllogictest 格式测试工具。已拆分为 4 个子工作项；本项不再承载全部合同/验收，也不再对巨型 Spec 调度 Planner。
路径等级: full
Spec 门禁: required（总览 Spec：背景、非目标摘要、子项职责与依赖、已决议表；详细合同迁至各子项）
Spec 用户确认: approved（子项拆分与总览化已由用户确认推进；各子项 Spec 仍须各自确认）
Design 门禁: skipped（父项不再写 Design/Plan；模块边界由子项 `ggtest-parser` / `ggtest-runner-sqlite` 等各自 Design 门禁处理）
Review 门禁: skipped（父项不直接实现；审阅在各子项进行）
源分支: 不适用（非 Git 工作区，跳过提交与合并操作）
目标分支: 不适用（非 Git 工作区，跳过提交与合并操作）
文档影响: docs/features/ggtest-core/spec.md 改为总览；详细 Spec 位于各子项目录
状态: blocked
后续步骤: 跟踪四子项进度；默认建议先开 `ggtest-parser` 的 Spec 确认 → Design/Plan。父项在四子项均 `done` 后关闭归档。确认前不得对父项调度 Planner。
阻塞原因: 已拆分为子工作项（decomposed epic）。父项仅作总览与进度跟踪，等待子项完成。
恢复条件: 四个子项 `ggtest-parser`、`ggtest-normalize`、`ggtest-runner-sqlite`、`ggtest-cli-corpus` 均达到 `done`（或用户明确取消/关闭父项）。
恢复后的目标状态: done

> 权威工作流、门禁与状态说明见 [docs/README.md](../README.md)。

## 子工作项（依赖顺序）

| 顺序 | feature-id | 职责摘要 | 验收对齐（原 Spec） |
|---|---|---|---|
| 1 | [ggtest-parser](ggtest-parser.md) | 解析 `.test`/`.slt`/单文件 → 记录模型；解析错误含文件+行号；不连库 | P0-7 |
| 2 | [ggtest-normalize](ggtest-normalize.md) | I/T/R 规范化、排序、MD5 兼容、hash-threshold | P0-2、P0-4、P0-5、P1-3 |
| 3 | [ggtest-runner-sqlite](ggtest-runner-sqlite.md) | Runner + 执行器抽象 + SQLite JDBC；skipif/onlyif/halt/label/statement/query | P0-3、P0-6、P0-8、P1-2、P1-4 |
| 4 | [ggtest-cli-corpus](ggtest-cli-corpus.md) | CLI、统计、退出码、目录收集、官方语料硬验收 | P0-1、P1-1、P1-5、P1-6 |

依赖：parser ∥ normalize → runner-sqlite → cli-corpus。

## 审计笔记

- 2026-07-24：产品名与工作项 ID 曾短暂为 JJTEST / `jjtest-core`，同日更名为 GGTEST / `ggtest-core`；首期目标库同日定为 SQLite（JDBC）。旧路径已废弃。
- 2026-07-24：用户确认推进工作项拆分；父项改为 decomposed epic（状态 `blocked` 跟踪）；不再对巨型 core 调度 Planner。

## 用户已确认决策（须由总览 Spec 与各子项 Spec 继承，勿重开）

| 决策 | 结论 | 状态 |
|---|---|---|
| 首期目标库 | **SQLite（JDBC）**；其他库（含 PostgreSQL）为首期非目标，扩展点保留 | 已确认 |
| 产品/项目名 | **GGTEST** | 已确认 |
| 工作项 ID | **ggtest-core**（现为 epic）；实现拆为 4 子项 | 已确认 |
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
| 拆分 | 拆为 parser / normalize / runner-sqlite / cli-corpus 四子项 | **已确认** |

## 进度笔记

- 2026-07-24 登记；路径 full；Spec/Design/Review 门禁均为 required。非 Git 工作区，跳过提交与合并。
- 2026-07-24 Spec 初稿完成；用户确认 Q1–Q7；首期目标库与产品名定为 SQLite / GGTEST。
- 2026-07-24 Analyst 修订 Spec：落实 GGTEST / SQLite / Q1–Q7；新增待确认 Q8、Q9。状态 → `awaiting-spec-approval`。
- 2026-07-24 用户指示：文档去冗余；Q9 认同；Q8 先解释未拍板。Manager → `speccing`，调度 Analyst。
- 2026-07-24 **Analyst 修订完成**：去冗余；Q9 写入已决议（P1-5 = select1/2/3）；Q8 保持开放。Manager：`speccing` → **`awaiting-spec-approval`**。**未**调度 Planner。
- 2026-07-24 用户确认：① **Q8 采纳**零豁免硬验收；② 新增 **`.slt` 与 `.test` 等价**。Manager：→ **`speccing`**，调度 Analyst。
- 2026-07-24 **Analyst 增量修订完成**：Q8 入已决议并从开放问题移除；`.slt`/`.test` 等价写入范围/CLI/合同；验收修订 P1-1、新增 P1-6。Manager：`speccing` → **`awaiting-spec-approval`**。**未**写代码、**未**调度 Planner。当前无开放待确认项；待用户确认本轮 Spec 整体。
- 2026-07-24 用户反馈验收段文风：按原形式 GWT、勿过多精简、勿过长。Manager 调度 Analyst（只改文风/结构）；状态保持 **`awaiting-spec-approval`**。
- 2026-07-24 **Analyst 文风修订完成**：验收段恢复短标题 + 单条 GWT；去掉政策旁注与多余加粗；开放问题「待确认」精简为一句；Q1–Q9/合同未改。**未**调度 Planner。待用户确认 Spec 整体。
- 2026-07-24 **用户确认推进拆分**：登记四子项；父项 → **`blocked`（decomposed epic）**；调度 Analyst：① 改写 core Spec 为总览；② 为四子项各写 Spec（从原 core 裁切）。**不**对父项调度 Planner。
- 2026-07-24 **Analyst 完成**：`ggtest-core/spec.md` 已改为 Epic 总览；四子项 Spec 均已产出。四子项 → `awaiting-spec-approval`。父项保持 `blocked`。**未**调度 Planner。
