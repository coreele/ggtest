# Plan: ggtest-core / runner-sqlite

> 实施与验证计划。需求依据见 [`spec.md`](./spec.md)，架构依据见 [`design.md`](./design.md)。
>
> **适用对象**：Developer（实施）、Reviewer（审阅）、QA（验收）。
> **前置条件**：Spec 已确认（approved）；Design（`design.md`）已存在；Java 17 + Maven；上游 parser/normalize 已在 `main`。
> **阅读顺序**：元信息 → 目标摘要 → 任务拆解 → 依赖与顺序 → 触碰路径 → 验证 → 验收 → 文档影响 → 交接。
> **预期结果**：Developer 可按 TDD 实现 Runner + 执行器抽象 + SQLite JDBC 并完成本地验证；QA 可据验收项独立复核。
> **失败处理**：验证失败按「验证」节证据定位；无法执行时按「无法执行验证时的处理」记录原因/风险/恢复条件。

## 元信息

- 工作项标识: ggtest-core（sub-feature-id: runner-sqlite）
- 依据 Spec: [workflow/workflow/docs/features/ggtest-core/ggtest-core-runner-sqlite/spec.md](./spec.md)
- 依据 Design: [workflow/workflow/docs/features/ggtest-core/ggtest-core-runner-sqlite/design.md](./design.md)
- 路径等级: full
- Review 门禁: required（进入 QA 前须 Reviewer `Approve`）
- 最低验证层: L3（单元/组件 + SQLite JDBC 内存库集成 + 构建）
- 验证命令: `mvn -q clean test`
- 建议源分支: `ggtest-core-runner-sqlite`（目标分支 `main`）

## 适用工程规范

- [文档工程](../../standards/documentation.md)
- [Git 协作](../../standards/git.md)（实施前须检出工作分支 `ggtest-core-runner-sqlite`）
- [质量与验证](../../standards/quality.md)
- [安全](../../standards/security.md)

## 目标摘要

- 在既有 Maven 工程新增：`com.ggtest.db`（执行器抽象）、`com.ggtest.db.sqlite`（SQLite JDBC）、`com.ggtest.runner`（单文件执行编排）。
- Runner 只依赖执行器接口与 `model`/`normalize`；首期交付 `engineName=sqlite` 的 JDBC 实现。
- 覆盖 Spec 验收：P0-3、P0-6、P0-8、P1-2、P1-4。不交付 CLI/官方语料硬验收。

## 任务拆解

TDD：先写失败测试，再实现至通过。签名对齐 Design；行为合同以 Spec 为准。

1. **T1 — 执行器抽象（`com.ggtest.db`）**：`DatabaseExecutor`；`StatementResult` / `QueryResult`；`FatalDatabaseException`。
   - 完成条件：类型编译通过；无 JDBC/SQLite 依赖进入本包。
2. **T2 — Runner 状态机（假执行器）**：`SqlLogicTestRunner` 消费有序记录；实现 skipif/onlyif、hash-threshold、halt→skipped、statement ok/error、query（调用 `ResultComparer`）、label 一致性、致命异常中止；产出逐记录结果。测试使用内存假 `DatabaseExecutor`（`engineName` 可配置为 `sqlite`）。
   - 完成条件：P0-6、P1-2、P1-4 及 statement 断言逻辑在假执行器下可测通过；`runner` 源码无 `com.ggtest.db.sqlite` / `java.sql` import。
3. **T3 — SQLite JDBC 适配**：`pom.xml` 增加 `org.xerial:sqlite-jdbc`（建议 **3.53.2.0**）；实现 `SqliteJdbcExecutor`（持有调用方 `Connection`）；`engineName()`=`sqlite`；业务失败 vs 致命失败分类；`getString`/`wasNull` 值抽取。
   - 完成条件：`jdbc:sqlite::memory:` 上可执行语句与查询；连接关闭类场景抛 `FatalDatabaseException`。
4. **T4 — 真库验收与 fixtures**：`src/test/resources/fixtures/runner/` 自造 `.test`；用 parser → Runner → `SqliteJdbcExecutor` 跑 P0-3；条件/halt/label 可真库或假执行器回归；P0-8 以测试或审查清单锁定「runner 不依赖 sqlite 包」。
   - 完成条件：P0-3、P0-6、P0-8、P1-2、P1-4 Pass；`mvn -q clean test` 全绿（含 parser/normalize 回归）。
5. **T5 — 开发文档**：更新 `README.md`（Runner/执行器/SQLite 用法）；公共类型 Javadoc；`dev-notes.md` 记录 L3 验证证据与依赖版本。
   - 完成条件：README 含构建/测试命令与最小调用示例；`dev-notes.md` 含验证证据。

## 依赖与顺序

- T1 → T2 → T3 → T4 → T5（T2 仅依赖 T1 + 假执行器；T3 可与 T2 后期并行起草，但合入验证以 T4 为准）。
- 各任务内部 TDD。
- 上游只读复用：`model`、`parser`、`normalize`；禁止改其行为合同。
- Git：实施前自 `main` 创建并检出 `ggtest-core-runner-sqlite`。
- 网络：拉取依赖失败可用代理 `127.0.0.1:7890`；多次未成功则停止并记入 `dev-notes.md`。

## 触碰路径

- `pom.xml`（增加 sqlite-jdbc）
- `src/main/java/com/ggtest/db/`（新增）
- `src/main/java/com/ggtest/db/sqlite/`（新增）
- `src/main/java/com/ggtest/runner/`（新增）
- `src/test/java/com/ggtest/runner/`、`src/test/java/com/ggtest/db/`（新增）
- `src/test/resources/fixtures/runner/`（新增）
- `README.md`（更新）
- `workflow/workflow/docs/features/ggtest-core/ggtest-core-runner-sqlite/dev-notes.md`（Developer 实施后新增）
- **禁止**：改 Spec；改 `parser`/`normalize` 行为；实现 CLI/退出码/目录收集；引入默示豁免逻辑；`runner` 包依赖 `db.sqlite` 或直接 `java.sql`

## 验证

- **最低验证层**：L3。理由：Spec 验收依赖真实 JDBC（内存 SQLite）；编排可用假执行器，但 P0-3 等须集成驱动。
- **验证命令**：`mvn -q clean test`（Java 17）；可选 `mvn -q clean compile`。
- **预期证据**：`BUILD SUCCESS`；Surefire 全 Pass、0 失败 0 错误（含既有 parser/normalize 无回归）；无编译错误。
- **无法执行验证时的处理**：缺 JDK 17/Maven、或 sqlite-jdbc 无法下载时记入 `dev-notes.md`——风险：无法确认 JDBC 集成与 P0-3；恢复：安装工具链/配置代理后重跑。不得以「无官方语料」跳过本切片验收（本切片用自造 fixtures）。

## 验收

对齐 [`spec.md`](./spec.md)：

- **P0-3**：含 `statement ok` 与 `statement error` 的文件 → 均通过；将 error 条改为合法 SQL 后该条失败。
- **P0-6**：`skipif sqlite` / `onlyif sqlite` / `onlyif postgresql` → engine=`sqlite` 时第一、三条 skipped，第二条执行。
- **P0-8**：`runner` 仅依赖执行器抽象；SQLite 实现可整体替换而不改 parser/runner 源码（Reviewer 依赖检查佐证）。
- **P1-2**：`halt` 后记录不执行且计 skipped。
- **P1-4**：同 label 结果冲突 → 后出现记录失败并指明 label 冲突。

## 文档影响

| 类别 | 更新路径或 N/A 理由 |
|---|---|
| 开发文档 | `README.md`；`com.ggtest.db` / `db.sqlite` / `runner` Javadoc；`dev-notes.md` |
| 用户文档 | N/A：无 CLI/用户可见入口（属 `cli-corpus`） |
| 运维文档 | N/A：无部署/监控/运维面变更 |

## 安全影响

- 执行调用方提供的 SQL；连接由调用方配置。测试使用内存库/本地 fixtures。凭据不得写入日志或报告。Reviewer 按 [安全规范](../../standards/security.md) 确认。

## Review 门禁与进入 QA

- Review 门禁：**required**。进入 QA 前须 Reviewer `Approve`（含 P0-8 依赖检查、测试有效性、文档影响、安全影响）。
- standard/full 不得省略 Review。

## 交接顺序

1. 用户确认本 Plan → Manager 持久化确认 → 状态方可 `planned`。
2. Developer：检出 `ggtest-core-runner-sqlite` → T1–T5 + `dev-notes.md`。
3. Reviewer：`Approve`（进入 QA 前置）。
4. QA：独立验收 → `qa-report.md`。

Planner 不将状态设为 `planned`，不调度 Developer。

## 修订记录

| 日期 | 摘要 |
|---|---|
| 2026-07-25 | 初稿并 refine-docs：T1–T5、L3、Design required、Review required、分支 ggtest-core-runner-sqlite |
