# Plan: ggtest-core / normalize

> 实施与验证计划。需求依据见 [`spec.md`](./spec.md)。Design 门禁 skipped（算法已在 Spec 写死），无 `design.md`。
>
> **适用对象**：Developer（实施）、Reviewer（审阅）、QA（验收）。
> **前置条件**：Spec 已确认（approved）；Java 17 + Maven；parser 已交付（可复用 `ColumnType`、`SortMode`）。
> **阅读顺序**：元信息 → 目标摘要 → 任务拆解 → 依赖与顺序 → 触碰路径 → 验证 → 验收 → 文档影响 → 交接。
> **预期结果**：Developer 可按 TDD 实现规范化/排序/哈希比对并完成本地验证；QA 可据验收项独立复核。
> **失败处理**：验证失败按「验证」节证据定位；无法执行时按「无法执行验证时的处理」记录原因/风险/恢复条件。

## 元信息

- 工作项标识: ggtest-core（sub-feature-id: normalize）
- 依据 Spec: [docs/features/ggtest-core/ggtest-core-normalize/spec.md](./spec.md)
- 依据 Design: N/A（Design 门禁 skipped）
- 路径等级: full
- Review 门禁: required（进入 QA 前须 Reviewer `Approve`）
- 最低验证层: L2（单元测试 + 构建）
- 验证命令: `mvn -q clean test`

## 适用工程规范

- [文档工程](../../standards/documentation.md)
- [Git 协作](../../standards/git.md)（实施前须检出工作分支 `ggtest-core-normalize`）
- [质量与验证](../../standards/quality.md)
- [安全](../../standards/security.md)

## 目标摘要

- 在既有 Maven 工程（`com.ggtest:ggtest:0.1.0-SNAPSHOT`，Java 17）新增包 `com.ggtest.normalize`：I/T/R 规范化、`nosort`/`rowsort`/`valuesort`、与官方 C 逐字节兼容的 MD5、hash-threshold（默认初值 **8**，由调用方传入）。
- 不依赖 JDBC；固定样例验收。复用 `com.ggtest.model` 的 `ColumnType`、`SortMode`，不改 parser。
- 依据 Spec 验收：P0-2、P0-4、P0-5、P1-3。

## 任务拆解

TDD：先写失败测试，再实现至通过。API 签名由本 Plan 约定（Design skipped）；行为合同以 Spec 为准。

1. **T1 — I/T/R 值规范化**：`com.ggtest.normalize` 按 `ColumnType` 将单个原始值转为规范化文本；规则对齐 Spec「范围与可见行为」I/T/R 表。
   - 完成条件：覆盖 I/NULL、R/`%.3f`、T/`(empty)` 及控制字符→`@` 的单元测试 Pass。
2. **T2 — 行展开与排序**：多列一行展开为每值一行；比对/哈希前按 `SortMode` 排序——`NOSORT` 保序；`ROWSORT` 以规范化整行为单位字符串排序；`VALUESORT` 打散全值后字符串排序。
   - 完成条件：同组输入在三种模式下产出符合 Spec；可支撑 P0-5、P1-3。
3. **T3 — MD5 与期望形态**：每个规范化值后接 `\n` 拼接后求 MD5（小写十六进制），与官方语料既有哈希逐字节兼容；识别期望侧 `N values hashing to <md5>`；threshold ≤ 0 时不做哈希形态比对。
   - 完成条件：固定样例（摘自如 `select1.test` 的规范化值序列 + 期望 MD5）计算一致；threshold ≤ 0 分支可测。
4. **T4 — 比对入口**：无 JDBC API——入参：类型签名、`SortMode`、hash-threshold、期望文本、实际原始行列值（或已展开值）；流程：规范化 → 排序 →（值总数 > threshold 且 threshold > 0 → 哈希形态，否则全量文本）→ 通过/失败；失败须含期望 vs 实际差异摘要素材。提供默认 threshold 常量 **8**（Q5）。
   - 完成条件：可驱动 P0-2/P0-4/P0-5/P1-3；失败路径差异摘要非空。
5. **T5 — 验收测试与 fixtures**：`src/test/java/com/ggtest/normalize/` + `src/test/resources/fixtures/normalize/` 固定样例，对齐「验收」；不引入官方大语料。
   - 完成条件：P0-2、P0-4、P0-5、P1-3 Pass；`mvn -q clean test` 全绿（含 parser 回归）。
6. **T6 — 开发文档**：更新 `README.md`（normalize 入口）；公共类型 Javadoc；`dev-notes.md` 记录验证证据。
   - 完成条件：README 含构建/测试命令与入口示例；`dev-notes.md` 含 L2 证据。

## 依赖与顺序

- T1 → T2 → T3 → T4 → T5 → T6（T3 哈希输入须为 T1+T2 产出；T3 测试可与 T2 并行起草）。
- 各任务内部 TDD。
- 上游：已交付的 `ColumnType`/`SortMode`；不依赖 `runner-sqlite` / `cli-corpus`；与 parser 无运行时耦合。
- Git：实施前自目标分支创建并检出 `ggtest-core-normalize`。

## 触碰路径

- `src/main/java/com/ggtest/normalize/`（新增）
- `src/test/java/com/ggtest/normalize/`（新增）
- `src/test/resources/fixtures/normalize/`（新增）
- `README.md`（更新）
- `docs/features/ggtest-core/ggtest-core-normalize/dev-notes.md`（Developer 实施后新增）
- 只读复用：`com.ggtest.model.ColumnType`、`SortMode`
- **禁止**：改 Spec；改 `com.ggtest.parser` 行为；引入 JDBC；创建本切片 `design.md`

## 验证

- **最低验证层**：L2。理由：纯内存算法 + 固定样例即可验收；不连库；端到端属下游切片。
- **验证命令**：`mvn -q clean test`（Java 17）；可选 `mvn -q clean compile`。
- **预期证据**：`BUILD SUCCESS`；Surefire 全 Pass、0 失败 0 错误（含 parser 无回归）；无编译错误。
- **无法执行验证时的处理**：缺 JDK 17/Maven 时记入 `dev-notes.md`——风险：无法确认构建与哈希兼容；恢复：安装后重跑命令。官方语料原文不可得时仍须用摘录固定样例完成 P0-2，不得以「无语料」跳过。

## 验收

对齐 [`spec.md`](./spec.md)：

- **P0-2**：固定样例（规范化值序列 + hash-threshold + 期望 `N values hashing to <md5>`）→ 拼接 `\n` 求 MD5 → 与期望一致。
- **P0-4**：I/NULL、R/`%.3f`、T/`(empty)` 三条 → 规范化比对通过。
- **P0-5**：行序与期望不同 → `rowsort` 通过、`nosort` 失败且含差异摘要素材。
- **P1-3**：`valuesort` + 全值排序期望 → 比对通过。

## 文档影响

| 类别 | 更新路径或 N/A 理由 |
|---|---|
| 开发文档 | `README.md`；`com.ggtest.normalize` Javadoc；`dev-notes.md` |
| 用户文档 | N/A：无 CLI/用户可见功能（属 `cli-corpus`） |
| 运维文档 | N/A：无部署/监控/运维面变更 |

## 安全影响

- 仅内存处理调用方文本/值与期望串；不执行 SQL、不连库；测试可读 classpath fixtures。无敏感信息入仓。Reviewer 按 [安全规范](../../standards/security.md) 确认。

## 交接顺序

Developer（检出 `ggtest-core-normalize` → T1–T6 + `dev-notes.md`）→ Reviewer（测试有效性、文档影响、安全影响）→ `Approve`（进入 QA 前置）→ QA（独立验收 → `qa-report.md`）。Plan 须经用户确认并由 Manager 持久化后方可调度 Developer；Planner 不设状态为 `planned`。

## 修订记录

| 日期 | 摘要 |
|---|---|
| 2026-07-24 | 初稿并 refine-docs：T1–T6、L2、P0-2/P0-4/P0-5/P1-3、Design skipped、Review required |
