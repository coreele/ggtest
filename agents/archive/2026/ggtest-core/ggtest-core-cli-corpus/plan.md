# Plan: ggtest-core / cli-corpus

> 实施与验证计划。需求依据见 [`spec.md`](./spec.md)。Design 门禁 skipped（CLI/退出码已在 Spec 写死），无 `design.md`。
>
> **适用对象**：Developer（实施）、Reviewer（审阅）、QA（验收）。
> **前置条件**：Spec 已确认（approved）；Java 17 + Maven；上游 parser / normalize / runner-sqlite 已合入 `main`。
> **阅读顺序**：元信息 → 目标摘要 → 任务拆解 → 依赖与顺序 → 触碰路径 → 验证 → 验收 → 文档影响 → 交接。
> **预期结果**：Developer 按 TDD 交付可执行 CLI `ggtest`、报告、退出码与目录收集，并完成本地验证（含官方语料硬验收证据）；QA 据验收项独立复核。
> **失败处理**：按「验证」节定位；无法执行时按「无法执行验证时的处理」记录原因/风险/恢复条件。

## 元信息

- 工作项标识: ggtest-core（sub-feature-id: cli-corpus）
- 依据 Spec: [agents/features/ggtest-core/ggtest-core-cli-corpus/spec.md](./spec.md)
- 依据 Design: N/A（Design 门禁 skipped）
- 路径等级: full
- Review 门禁: required（进入 QA 前须 Reviewer `Approve`）
- 最低验证层: L4（可执行 CLI 端到端 + SQLite + 官方语料硬验收；自造 fixtures 覆盖 CLI 行为回归）
- 验证命令:
  - `mvn -q clean test`
  - `mvn -q clean package`
  - 硬验收（用户自备语料路径，占位 `$SELECT1` / `$SELECT2` / `$SELECT3`）：
    - `ggtest --url jdbc:sqlite::memory: "$SELECT1"` → 失败数=0、退出码=0（P0-1）
    - `ggtest --url jdbc:sqlite::memory: "$SELECT1" "$SELECT2" "$SELECT3"` → 分文件+总计、失败数=0、退出码=0（P1-5）
- 建议源分支: `ggtest-core-cli-corpus`（目标分支 `main`）

## 适用工程规范

- [文档工程](../../standards/documentation.md)
- [Git 协作](../../standards/git.md)（实施前须检出工作分支 `ggtest-core-cli-corpus`）
- [质量与验证](../../standards/quality.md)
- [安全](../../standards/security.md)

## 目标摘要

- 交付用户入口 **`ggtest`**：组装调用 parser → `SqlLogicTestRunner` / `SqliteJdbcExecutor`（normalize 由 Runner 内部使用）；本切片不改上游内部实现。
- 覆盖 Spec：CLI 参数与默认值、目录递归 `*.test`/`*.slt`、分文件+总计报告、退出码 0/1/2、跨文件状态重置、官方语料**零豁免**硬验收（P0-1、P1-5；验收另含 P1-1、P1-6）。
- 语料用户自备，仓库禁止提交官方大语料；凭据不得写入日志/报告。

## 任务拆解

TDD：先写失败测试，再实现至通过。CLI 形状对齐 Spec；行为合同以 Spec 为准。Design skipped：包名/入口由本 Plan 约定为 `com.ggtest.cli` / `com.ggtest.cli.Main`。

1. **T1 — 参数解析与用法错误（退出码 2）**：解析必填 `--url`；可选 `--user`/`--password`/`--engine`（默认 `sqlite`，首期仅允许该值）/`--hash-threshold`（默认 **8**）；至少一个位置参数（文件或目录）。缺参、未知选项、非法 `--engine`/阈值 → stderr 说明原因，退出码 **2**；不连库、不执行。
   - 完成条件：缺 `--url`、无位置参数、`--engine`≠`sqlite` → 退出码 2；输出不含凭据。

2. **T2 — 输入收集**：目录递归收集 `*.test` 与 `*.slt`（同等、稳定排序，建议绝对路径字典序）；单文件不强制扩展名，扩展名不影响语义。
   - 完成条件：嵌套 fixtures 收集齐全；显式 `.slt` 可进入流水线（支撑 P1-1、P1-6）。

3. **T3 — 编排、报告与退出码**：每文件：`SqlLogicTestParser` → JDBC（`--url` + 可选凭据）→ `SqliteJdbcExecutor` → `SqlLogicTestRunner(executor, cliHashThreshold).run(records)` → 关闭连接。
   - **跨文件重置**：每文件独立 `run`；Runner 始终以 CLI `--hash-threshold` 初值构造（默认 8）；条件/label 依赖 Runner 单次 `run` 作用域清空（不改 Runner）。
   - **报告（stdout 纯文本）**：每文件通过/失败/跳过 + 总计；失败含文件、行号、记录摘要（SQL 首行）、失败原因。解析错误：文件+行号+原因，该文件计错误，其余文件继续。
   - **退出码**：任一用法/配置/解析/连接/致命中断 → **2**；否则有失败记录 → **1**；否则 **0**。
   - 完成条件：fixtures 覆盖 0/1/2；后文件不受前文件 hash-threshold/条件/label 污染；报告含分文件/总计与失败四要素；无凭据泄漏。

4. **T4 — 可执行制品与命令名 `ggtest`**：`pom.xml` 可执行 JAR（`Main-Class`=`com.ggtest.cli.Main`）；薄启动脚本（如 `bin/ggtest`）或 README 记载的同名入口。手写 argv，不引入非必要 CLI 框架。
   - 完成条件：`mvn -q clean package` 后可用 `ggtest`（或 README 等价方式）跑通最小 fixtures。

5. **T5 — 验收测试与硬验收证据**：
   - **仓库内**：`src/test/resources/fixtures/cli/` 自造小语料，覆盖 P1-1、P1-6、退出码、跨文件重置、报告字段（进程调用入口或等价公共 API）。
   - **官方语料硬验收（L4，零豁免）**：用户自备路径，禁止入库。按「元信息」硬验收命令跑 P0-1、P1-5（空白 SQLite）。可选：`GGTEST_CORPUS_DIR` 等启用集成测试，或手工执行并将命令/退出码/统计写入 `dev-notes.md`/`qa-report.md`。不可默示豁免；不可消除偏差须停并上报。
   - 完成条件：`mvn -q clean test` 全绿（含上游回归）；P0-1/P1-5 证据在 `dev-notes.md`（命令+退出码+失败数=0）。

6. **T6 — 文档**：更新 `README.md`（安装/启动、参数、退出码、目录收集、语料自备、凭据勿入日志）；CLI Javadoc；`dev-notes.md` 记 L4/硬验收证据。
   - 完成条件：README 可指导用户跑通 P0-1 形态命令；`dev-notes.md` 含验证证据。

## 依赖与顺序

- T1 → T2 → T3 → T4 → T5 → T6（T2 可与 T1 后期并行起草；合入验证以 T5 为准）。
- 各任务内部 TDD。
- 上游只读：`parser`、`normalize`（经 Runner）、`runner`、`db`/`db.sqlite`；禁止改行为合同。
- Git：自 `main` 创建并检出 `ggtest-core-cli-corpus`。
- 网络：依赖拉取失败可用代理 `127.0.0.1:7890`；多次未成功则停止并记入 `dev-notes.md`。

## 触碰路径

- `pom.xml`（可执行 JAR / 启动插件）
- `bin/ggtest`（或等价启动脚本，若采用）
- `src/main/java/com/ggtest/cli/`、`src/test/java/com/ggtest/cli/`、`src/test/resources/fixtures/cli/`（新增，仅自造小文件）
- `README.md`；`agents/features/ggtest-core/ggtest-core-cli-corpus/dev-notes.md`（Developer 实施后）
- **禁止**：改 Spec；改上游行为；创建 `design.md`；官方大语料入库；日志/报告输出凭据

## 验证

- **最低验证层**：L4。理由：唯一用户入口为可执行 CLI，且 P0-1/P1-5 要求官方语料硬验收（失败数=0、退出码=0、零豁免）；L2/L3 不足以证明端到端与硬验收。fixtures + `mvn test` 做 CLI 回归；官方语料证据为 L4 必达。
- **验证命令**：见「元信息」。
- **预期证据**：`mvn -q clean test` → `BUILD SUCCESS`、Surefire 全 Pass（含上游无回归）；`mvn -q clean package` 成功且可启动 `ggtest`；P0-1/P1-5 退出码 0、失败数总计 0、无豁免；stdout/stderr 不含凭据。
- **无法执行验证时的处理**：
  - 缺 JDK 17/Maven → 记入 `dev-notes.md`；风险：无法确认构建/CLI；恢复：安装后重跑 `mvn`。
  - 官方语料不可得 → P1-1/P1-6/退出码仍用 fixtures；P0-1/P1-5 记阻塞（原因：无语料路径；风险：硬验收未证；恢复：提供路径后重跑硬验收并补证据）。禁止将硬验收标为通过或默示豁免。

## 验收

对齐 [`spec.md`](./spec.md)：

- **P0-1**：空白 SQLite + 官方 `select1.test` → 统计输出、失败数=0、退出码=0。
- **P1-1**：含 `.test`/`.slt`（可嵌套）的目录 → 递归执行全部匹配文件；分文件与总计统计。
- **P1-5**：官方 select1/2/3 批量 → 分文件与总计、失败数=0、退出码=0。
- **P1-6**：合法 `.slt` 单文件 → 与同等内容 `.test` 的统计与退出码一致。

## 文档影响

| 类别 | 更新路径或 N/A 理由 |
|---|---|
| 开发文档 | `README.md`（构建/打包）；`com.ggtest.cli` Javadoc；`dev-notes.md` |
| 用户文档 | `README.md`：CLI 用法、参数、退出码、目录收集、语料自备、示例命令 |
| 运维文档 | N/A：无部署/监控/运维面变更（本地 CLI） |

## 安全影响

- 接受 `--user`/`--password` 与 JDBC URL，执行用户 SQL 文件。凭据禁止写入日志、报告、fixtures 或提交。测试用内存库/自造 fixtures；官方语料只读自用户路径。Reviewer 按 [安全规范](../../standards/security.md) 确认。

## Review 门禁与进入 QA

- Review 门禁：**required**。进入 QA 前须 Reviewer `Approve`（测试有效性、文档影响、安全影响；确认无官方大语料入库、无凭据泄漏、无默示豁免）。
- full 不得省略 Review。

## 交接顺序

1. 用户确认本 Plan → Manager 持久化 → 方可 `planned`。
2. Developer：检出 `ggtest-core-cli-corpus` → T1–T6 + `dev-notes.md`（含 L4/硬验收证据）。
3. Reviewer：`Approve`（进入 QA 前置）。
4. QA：独立验收（含 P0-1/P1-5 或记录阻塞）→ `qa-report.md`。

Planner 不设状态为 `planned`，不调度 Developer，不自行批准 Plan。

## 修订记录

| 日期 | 摘要 |
|---|---|
| 2026-07-25 | 初稿并 refine-docs：T1–T6、L4、Design skipped、Review required、分支 ggtest-core-cli-corpus |
