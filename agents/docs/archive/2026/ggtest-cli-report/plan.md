# Plan: ggtest-cli-report

## 元信息

- 工作项标识: ggtest-cli-report（未拆分，sub-feature-id = feature-id）
- 依据 Spec: [agents/docs/features/ggtest-cli-report/spec.md](./spec.md)（已批准；合入前增量：失败块 `[SQL]` 首行/` ...`；既有：状态行 `[PASSED]` / `[FAILED] in` / `[SKIPPED]`、文件级计数、`--color` / `ggtest.color` / `GGTEST_COLOR`）
- 依据 Design: N/A（Design 门禁 skipped；本轮仍不改 Design）
- 路径等级: standard
- Review 门禁: required（进入 QA 前须 Reviewer Approve；本轮合入前小修走短审）
- 最低验证层: L3（单元测试 + 构建 + CLI 集成测试）
- 验证命令:
  - `mvn -q test` — 全量单元 + CLI 集成（含 `[SQL]` 多行 ` ...` / 单行不加、新标签、混合、`--color` / `ggtest.color` / `GGTEST_COLOR`）
  - `mvn -q -DskipTests package` — 构建 shaded jar
  - `java -jar target/ggtest-*.jar --url jdbc:sqlite::memory: <本地 .slt…>` — 端到端核对成功/失败/混合/硬错误（`--color never` / `| cat`；含多行 SQL 失败样例）

## 适用工程规范

- [文档工程](../../standards/documentation.md)
- [Git 协作](../../standards/git.md)
- [质量与验证](../../standards/quality.md)
- [安全](../../standards/security.md)

## 目标摘要

把 CLI 报告从单行机器格式重排为人类可读格式，冻结合同见 Spec「目标输出样例」。映射：Q-R1/Q-R2 → T1；Q-R3（失败块 + git Diff + **`[SQL]` 首行/` ...`**）→ T2；Q-R7 → T3；Q-R4 → T4；Q-R8 → T5；Q-R5=(B)（含 `[SQL]` 多行/单行覆盖）→ T6。

**本轮合入前增量（非新工作项）**：失败详情 `[SQL]` 呈现 = 首行；去除首行后仍有非空白 → 追加 ` ...`（`<首行> ...`）；纯单行不加。范围外：diff / 计数 / 彩色 / 退出码 / 路径列宽。Design 仍 skipped。**Manager 不另开 Plan 用户确认门禁**（用户指令已明确合同）。

不变量：退出码 `0`/`1`/`2` 不变且与文件计数独立（Q-R6）；比较/规范化语义不变；凭据不进 stdout/stderr。

## 任务拆解

本轮合入前增量**仅改 T2 / T6**（必要时 T7 / T8）；T1 / T3 / T4 / T5 不改。

### T1 报告骨架：相对路径、计时、文件级状态行与文件计数

- 改 `CliSession`：
  - 路径相对进程 CWD（`Path.of("").toAbsolutePath()` 相对化；无法相对化则回退原路径）；
  - 每文件计时（`System.nanoTime()`）；状态行（废 `PASS in` 与 `after`）：
    - 成功：`<path> .. [PASSED] in N ms`（路径列对齐；已实现列宽下限，本轮不改）；
    - 失败：`<path> .. [FAILED] in N ms`（介词一律 **`in`**）；
    - 跳过：`<path> .. [SKIPPED]`（**不写耗时**）；
  - 文件级计数（Spec 已冻）：
    - `failed` = 含 ≥1 条 FAILED，**或**硬错误的文件；
    - `passed` = 执行完毕且无 FAILED、**无硬错误**；
    - `skipped` = 全部可断言记录被 skip、**且无硬错误**；
    - 三桶互斥、均为文件数；硬错误优先（一律 `failed`，不得冒充成功）。
  - 退出码与计数独立：硬错误 → `2`（即使已计入 `failed`）；仅断言失败 → `1`；`failed=0` 且无硬错误 → `0`。保留 hardError 标志，**不得**由 `failed` 反推退出码。
- 完成条件：全通过 / 失败 / 跳过 / 硬错误本地跑通；标签、耗时（跳过无）、文件级 `TOTAL:`、退出码符合 Spec；无 `PASS in`、无 `after`、无 `FILE:`。

### T2 失败详情块与 git 风格 Diff

- 改 `ResultComparer#buildDiffSummary`：git 风格（未变无前缀、期望 `-`、实际 `+`、带上下文；保留行号供 `at`）。比较判定与规范化/排序/哈希不变，只改呈现。
- 改 `CliSession#printFailure` → `[WHY]` / `[SQL]` / `[Diff] (-expected|+actual)`（statement 可省略 Diff）/ `at <相对路径>:<行号>`；删除 `singleLine()`。
- **`[SQL]` 呈现（合入前增量 · Spec 已冻）**：取 SQL **第一行**（去尾随空白）；若去除首行后仍有非空白内容 → 追加一个空格 + `...`（`<首行> ...`）；纯单行**不加** ` ...`。
- statement 失败同视觉体系；`[SQL]` 同规则；不改「只断言失败事实、不做消息/正则匹配」。
- 完成条件：与 Spec 失败/statement 样例结构一致；多行含 ` ...`、单行不含；无整段单行 `reason=`。本轮不改 Diff/计数/彩色/退出码/路径列宽。

### T3 汇总区：混合顺序、Error 仅失败文件 + TOTAL

- 按发现/参数顺序逐文件状态行；失败详情紧跟该文件；成功/跳过行间**不插额外空块**。
- 若有失败文件：`Error: some test case failed:` + 引号列表（`[` … `]`），**仅列失败文件**；空行后 `TOTAL: passed=N failed=N skipped=N`。
- 完成条件：混合场景顺序与 Spec 混合样例一致；退出码 `1`（无硬错误，P1-1）。

### T4 彩色输出：`--color` + `ggtest.color` + `GGTEST_COLOR`（Q-R4）

- 解析 `--color <auto|always|never>`（默认 `auto`）；读 `ggtest.color`、`GGTEST_COLOR`。优先级：CLI > 属性 > env > `auto`。非法取值 → `UsageException`（码 `2`）。属性键/env 名单一定义，禁止散落字面量。
- 经 `Main` 注入 `CliSession`：`auto` → TTY（`System.console() != null`）；`always`/`never` 强制。构造注入「是否 ANSI」便于测试。
- 包内样式助手（不新增对外 API）：彩色仅标签/结构符；文本与布局两种模式一致。
- 完成条件：P1-4 / P1-5 —— `always` 含 ANSI；`never` 与 `auto`+非 TTY 无 ANSI；未显式 CLI 时属性优于 env；显式 CLI 覆盖二者。

### T5 硬错误重排（Q-R8）

- 解析/IO/连接/隔离/aborted：同失败视觉（多行头 + 相对路径 + 原因缩进；解析含行号）；按 T1 **计入 `TOTAL.failed`**；退出码仍 `2`。
- `UsageException`（无文件上下文）：同视觉多行头（细节 Developer 自定，无单独样例）；码 `2`；`EnvConfigIntegrationTest`「不得冒充成功统计」保持。
- 完成条件：P1-3 —— 硬错误计入 `failed`、码 `2`、多行硬错误、无假装全通过。

### T6 识别并修改受影响测试（Q-R5=(B)，必做）

先检索旧耦合（`FILE:`、`FAILURE:`、`ERROR:`、`reason=`、`PASS in`、`after`、`TOTAL:` 正则、`countFailures`/`extractPassed`、`diffSummary`）及旧彩色名（`GGTEST_TERM_COLOR`、`CARGO_TERM_COLOR`），再改断言：

| 测试 | 耦合点 | 动作 |
|---|---|---|
| `CorpusHardAcceptanceTest` | `TOTAL:`、failed 正则、文件名 | 文件计数；去 `FILE:` / `PASS in`；断言 `[PASSED]` / `[FAILED] in` / `[SKIPPED]` |
| `MainOrchestrationTest` | `countFailures`/`extractPassed` | 按新标签与文件计数更新 |
| `PostgresCliIntegrationTest` | 自带 `countFailures` | 同上（无 gate 时跳过） |
| `EnvConfigIntegrationTest` | `!stdout.contains("FILE:")` | 改断言无成功统计/无 `[PASSED]`；仍码 `2` |
| `ResultComparerTest`、`NormalizeAcceptanceTest` | `diffSummary` expected/actual | 比较语义不变；呈现改 git 风格 |

新增 Spec 验收断言（`Main.run` 注入流；默认非 TTY）：

- 布局：P0-1（`.. [PASSED] in N ms`、文件计数、码 0）；P0-2（`.. [FAILED] in`、`[WHY]`/`[SQL]`/`[Diff]`/`at`、Error、无 `reason=`、无 `after`）；P0-3 无密码；**P1-1 混合**（顺序、失败内联、成功行间无空块、Error 仅失败、混合 `TOTAL`、码 1）；P1-2 statement（`[FAILED] in`）；P1-3 硬错误（计入 `failed` **且**码 `2`）。
- **`[SQL]` 省略（合入前增量，必覆盖）**：多行 fixture（如 `SELECT name` 换行 `FROM items`）→ `[SQL] SELECT name ...`；纯单行（如 statement `INSERT ...`）→ 完整单行且**不**含尾随 ` ...`。
- 彩色 P1-4/P1-5：`always` 有 ANSI；`never` / 非 TTY `auto` 无；未显式 CLI 时 `GGTEST_COLOR=never` 无；`GGTEST_COLOR=always` + `-Dggtest.color=never` 无（属性优）；env/属性 `never` + `--color always` 以 CLI 为准。注入 `envLookup`/属性/参数即可。**禁止**残留旧彩色名断言。
- 完成条件：`mvn -q test` 全过；旧格式/旧彩色名已替换；新标签与混合齐备；**多行带 ` ...`、单行不带**有自动化证据；P0-4、P1-1、P1-4、P1-5 满足。

### T7 文档更新

- `README.md` stdout 段：成功/失败/**混合**示例（相对路径、`.. [PASSED] in N ms`、`.. [FAILED] in N ms`、`.. [SKIPPED]`、Error 仅失败、成功行间无空块、文件计数）；说明 `--color` / `ggtest.color` / `GGTEST_COLOR` 与优先级；说明文件计数与退出码独立（硬错误计入 `failed`、码仍 `2`）。退出码表不变。禁止示例残留 `PASS in` / `after` / `FILE:`。
- 本轮：若 README 失败样例含多行 SQL，`[SQL]` 对齐为 `<首行> ...`；单行不加。
- `CliSession` / `Main` / 参数解析 Javadoc 与输出及彩色合同同步。
- 完成条件：README 与实际输出一致（可由 T6 核对，含混合标签与 `[SQL]` 规则）；彩色文档与 Spec Q-R4 一致。

### T8 dev-notes 记录

- 写/追加 `agents/docs/features/ggtest-cli-report/dev-notes.md`（验证命令、证据、缺口；quality.md §1、§6），完成后 refine-docs。
- 本轮：记录 `[SQL]` 省略实现要点与多行/单行测试证据。
- 完成条件：覆盖「验证层与预期证据」（含 P1-1、P1-4/P1-5、本轮 `[SQL]`；TTY `auto` 人工核对或缺口声明），可支撑短 Review/QA。

## 依赖与顺序

- 主线（已完成）：T1 → T2 → T3 → T5；T4 可与 T2/T3 并行，须在 T6 前完成；T6 依赖 T1–T5；T7、T8 最后。
- **本轮合入前增量执行顺序**：修订 T2（`[SQL]` 呈现）→ T6（多行带 ` ...` / 单行不加）→ 必要时 T7/T8 → 短 Review → QA 回归 → 回合并授权。
- 提交粒度：待用户合并授权后由 Manager 统一处理；**本阶段不 commit/merge**。

## 触碰路径

**本轮合入前增量（优先）：**

- `src/main/java/com/ggtest/cli/CliSession.java`（`[SQL]` 首行/` ...` 呈现）
- `src/test/java/com/ggtest/cli/`（报告/编排验收：多行带 ` ...`、单行不加）
- `agents/docs/features/ggtest-cli-report/dev-notes.md`（追加本轮证据）
- `README.md`（仅当失败示例需对齐 `[SQL]` 省略时）

**主线已触碰（基线，本轮不重开除非回归）：**

- `src/main/java/com/ggtest/cli/{CliArgumentParser,ParsedArguments,RuntimeConfigResolver,CliOptions,Main,CliSession}.java`
- `src/main/java/com/ggtest/cli/`（包内样式助手）
- `src/main/java/com/ggtest/normalize/ResultComparer.java`
- `src/test/java/com/ggtest/cli/{CorpusHardAcceptanceTest,MainOrchestrationTest,EnvConfigIntegrationTest,PostgresCliIntegrationTest}.java`
- `src/test/java/com/ggtest/normalize/{ResultComparerTest,NormalizeAcceptanceTest}.java`

**不触碰**：parser / runner / 执行器；`agents/docs/manager/*`；`spec.md`（已由 Analyst 回写）；Design；`examples/` 未跟踪语料；`.env*`；本轮亦不改 Diff/计数/彩色/退出码/路径列宽实现。

## 验收

见 [spec.md](./spec.md) P0-1…P0-4、P1-1…P1-5；布局以 Spec「目标输出样例」为准（成功/失败/混合/跳过；耗时可变；布局/标签/顺序不得偏离）。本轮重点：P0-2 / P1-2 的 `[SQL]` 首行/` ...` 规则。

### 验证层与预期证据

- **L3**。理由：进程级布局与退出码；`Main.run` + fixture 可在 `mvn test` 覆盖多行/单行 `[SQL]` 与既有 P0/P1。
- 预期证据：`mvn -q test` 零失败（含多行 `[SQL] <首行> ...`、单行无 ` ...`）；`mvn -q -DskipTests package` 成功；shaded jar 冒烟可选；TTY `auto` 可选人工或缺口声明。
- 无法验证时：无交互终端则声明 TTY `auto` 未真机验证；`[SQL]` 省略与 `always`/`never`/非 TTY 仍可自动化。恢复：本地交互终端补验。其余 JDK 17 + Maven 可完成。

## Review 门禁与进入 QA 的条件

- Review **required**：本轮 T2/T6（及必要时 T7/T8）+ L3 证据齐备后提交短审；核对 `[SQL]` 多行带 ` ...`、单行不加；范围外项未误改。
- **进入 QA**：Reviewer `Approve`。QA 依 Spec P0-2/P1-2（及回归）与本 Plan 独立验收；`review.md`/`qa-report.md` 按 git.md §1.4 提交。
- 交接：**本轮不另开 Plan 用户确认**（用户指令已明确合同）→ Manager 调度 Developer → 短 Reviewer Approve → QA 回归 → 用户授权合并。

## 文档影响

| 类别 | 更新路径或 N/A 理由 |
|---|---|
| 开发文档 | Javadoc 与输出同步（T7，若触及）；`dev-notes.md` 追加本轮（T8） |
| 用户文档 | `README.md`：失败示例 `[SQL]` 与 Spec 对齐（多行 `<首行> ...`）（T7）；归档 cli-corpus Spec 不改 |
| 运维文档 | N/A：无部署/监控变更；退出码不变 |

## Plan 确认事项

整份 Plan 此前已用户批准并完成主线实施。**本轮合入前增量**（`[SQL]` 省略）合同已由用户指令明确并回写 Spec；**Manager 不另开 Plan 用户确认门禁**；非新工作项。下列基线决议仍有效、本轮不重开：

1. **文件级计数边界**：硬错误计入 `failed`；退出码与计数独立。（本轮范围外）
2. **用法/配置错误样式**：维持 T5。（本轮范围外）
3. **彩色配置命名**：`--color` + `ggtest.color` + `GGTEST_COLOR`。（本轮范围外）
4. **状态行标签与混合样例**：`[PASSED]` / `[FAILED] in` / `[SKIPPED]`。（本轮范围外）
5. **`[SQL]` 呈现（本轮）**：首行；去除首行后仍有非空白 → `<首行> ...`；纯单行不加。落点：T2、T6（及必要时 T7）。

## 修订记录

| 日期 | 摘要 |
|---|---|
| 2026-07-25 | 初稿：T1–T8（含 Q-R5=(B)）、L3、Review required；已 refine-docs |
| 2026-07-25 | 修订：Q-R4 → `--color` + `CARGO_TERM_COLOR`（T4/T6/T7）；已 refine-docs |
| 2026-07-25 | 修订（Plan 确认意见）：硬错误计入 `failed`、退出码独立；`CARGO_TERM_COLOR` → `GGTEST_TERM_COLOR`；确认事项 + 仍待整份批准；已 refine-docs |
| 2026-07-25 | 修订（Q-R4 Java 生态）：`GGTEST_COLOR` + `ggtest.color`；优先级 CLI > 属性 > env > `auto`；弃用旧名；已 refine-docs |
| 2026-07-25 | 修订（状态行标签 + 混合样例）：T1 → `[PASSED] in` / `[FAILED] in` / `[SKIPPED]`（废 `PASS in`/`after`）；T3 混合顺序、Error 仅失败、成功行间无空块；T6/T7 同步；确认事项第 4 项；仍待整份批准；已 refine-docs |
| 2026-07-25 | 修订（成功标签）：T1、T6、T7、文档影响与确认事项第 4 项统一为 `[PASSED]`；禁止旧式成功标签；整份 Plan 仍待最终批准；已 refine-docs |
| 2026-07-25 | 合入前增量（非新工作项）：T2 `[SQL]` = 首行；去除首行后仍有非空白 → `<首行> ...`；纯单行不加；T6 覆盖多行带 ` ...`、单行不带；范围外 diff/计数/彩色/退出码/路径列宽；Design 仍 skipped；**不另开 Plan 用户确认门禁**；已 refine-docs |
