# Plan: improve-multi-failure-report

## 元信息

- 工作项标识: improve-multi-failure-report（未拆分，sub-feature-id = feature-id）
- 依据 Spec: [workflow/workflow/docs/features/improve-multi-failure-report/spec.md](./spec.md)（Spec approved）
- 依据 Design: N/A（Design 门禁 skipped）
- 路径等级: standard
- Review 门禁: required（进入 QA 前须 Reviewer `Approve`）
- 最低验证层: L3（单元 + CLI 验收测 + 构建）
- 验证命令: 见「验证」节
- 源分支: `improve-multi-failure-report` → 目标 `main`
- 本轮约束: 停用户 Plan 确认；未确认前不得实施；不要 commit / push / merge（除非父会话另说）

## 适用工程规范

- [文档工程](../../standards/documentation.md)
- [Git 协作](../../standards/git.md)
- [质量与验证](../../standards/quality.md)
- [安全](../../standards/security.md)

## 目标摘要

按已批准 Spec 只改 CLI 失败明细呈现：**N≥2 相邻失败块之间恰好一空行**；**`at <file>:<line>` 无前导缩进**（单/多失败均适用）；`[WHY]`/`[SQL]`/`[Diff]` 保持现有缩进。不改 runner 继续执行语义、`TOTAL.failed` 文件级计数、退出码。

### 明确排除

| 项 | 指令 |
|---|---|
| WI-2 `fix-aggfunc-sum-overflow` / WI-3 `fix-aggfunc-total-precision` | 禁止改其语义相关代码/测试/文档 |
| `pom.xml` 无关改动 | 禁止（本项无依赖变更） |
| `sqllogictest/` 入库 | 禁止强行入库语料/临时 select* |
| 新 CLI 标志；JSON/JUnit 报告 | 禁止 |
| `[i/N]`、`N failures in file`、折叠、行号优先标题（S1/S3/S4/S5） | 禁止 |

## 任务拆解

### T0 — 分支与基线

- 做：确认源分支 `improve-multi-failure-report`（相对 `main`）；记录 `mvn -q test` 基线（含已知无关 skip）。
- 不做：改产品代码；改 `workflow/workflow/docs/manager/*`；启动 WI-2/WI-3。
- 完成条件：分支正确；基线可写入 `dev-notes.md`。

### T1 — Red：布局契约测试

先写/改断言，定点跑测须因布局合同失败（非编译错误）。

1. **`ReportWriterTest`**：`[WHY]`/`[SQL]`/`[Diff]` 仍四空格缩进；**`at` 行无前导空白**（以 `at ` 开头）；有行号 `at <file>:<line>`，无行号 `at <file>`。
2. **多失败**（`FileRunnerTest` 和/或 `CliReportAcceptanceTest`）：新 fixture（建议 `src/test/resources/fixtures/cli/multi-fail.test`）同文件恰好 **3** 条断言失败；相邻块间**恰好一空行**；每块缩进体 + 无缩进 `at`；禁止 `[i/N]`、`N failures in file`、整段 `reason=`；CLI：exit `1`，`TOTAL.failed=1`。
3. **单失败**（现有 `fail.test`）：加强无缩进 `at`；无索引/摘要/块间空行；`failed=1`；exit `1`。
4. **P0-3**：两失败文件（其一多失败、其一单失败）→ `failed=2`；exit `1`；`Error:` 仅两路径。
5. **P1-1**：硬错误共用 `ReportWriter.detailLines`（无缩进 `at`）；多段明细与断言失败共用 `FileRunner` 块间空行（`runWithExecutor` 聚合后再可能 `hardFailure`）。能造多段硬错误则补测；否则共享路径 + 多失败测覆盖，notes 记残差。

- 完成条件：测已入库；定点 **Red**（失败指向仍缩进 `at` 和/或块间无空行）。

### T2 — Green：最小实现

1. **`ReportWriter.detailLines`**：`"    at …"` → `"at …"`；不改体缩进与彩色标签。
2. **`FileRunner.runWithExecutor`**：`FAILED` 的 `addAll` 前若 `detailLines` 非空，先追加**一个**空行；不改 abort/halt/继续执行；不改 `TOTAL`/退出码（仍在 `CliSession`）。
3. 同步仅因旧缩进失败的弱断言；保留完备性与禁止项。

- 完成条件：T1 测 **Green**；无新 CLI 标志；无禁止文案。

### T3 — Verify

```bash
mvn -q test -Dtest=ReportWriterTest,FileRunnerTest,CliReportAcceptanceTest,MainOrchestrationTest
mvn -q clean test
mvn -q -DskipTests package
```

- 完成条件：Failures=0；package SUCCESS；证据入 notes。

### T4 — README 与 notes

1. `README.md` / `README.zh-CN.md`「报告」：单失败样例 `at` 无前导缩进；建议补与 Spec 同构的多失败（块间空行）示意。
2. 本目录 `dev-notes.md`：实现摘要、验证表、P0/P1 对照、§6 缺口。
3. 不做：改 `workflow/workflow/docs/manager/*` / `STATUS.md`；不改 Spec（实现无法满足则停并回 Analyst）。

- 完成条件：README 与冻结合同一致；notes 可供 Reviewer/QA 复现。

## 依赖与顺序

```text
T0 → T1(Red) → T2(Green) → T3(Verify) → T4(notes/README)
```

禁止跳过 Red 直接改产品代码。

## 触碰路径

| 任务 | 预期路径 |
|---|---|
| T0 | 分支状态（只读） |
| T1 | `ReportWriterTest.java`；`FileRunnerTest.java` 和/或 `CliReportAcceptanceTest.java`；`fixtures/cli/multi-fail.test`（名可微调）；必要时 `MainOrchestrationTest.java` |
| T2 | `ReportWriter.java`（`detailLines` 的 `at`）；`FileRunner.java`（`runWithExecutor` 块间空行） |
| T3 | 无新路径 |
| T4 | `README.md`；`README.zh-CN.md`；`workflow/workflow/docs/features/improve-multi-failure-report/dev-notes.md` |
| 禁止 | WI-2/WI-3；无关 `pom.xml`；`sqllogictest/` 入库；`workflow/workflow/docs/manager/*`；`design.md` |

## 验收

对齐 Spec P0/P1：

| ID | 要求（摘要） | 证据 |
|---|---|---|
| P0-1 | 同文件 3 失败：块间空行 + 无缩进 `at`；exit 1；`failed=1`；禁止索引/摘要/`reason=` | T1 fixture + CLI/Runner 测；T3 |
| P0-2 | 单失败：无缩进 `at`；完备性无回归；exit 1；`failed=1` | 加强 `CliReportAcceptanceTest` / `ReportWriterTest` |
| P0-3 | 两失败文件 → `failed=2`；exit 1；`Error:` 仅两路径 | 新/扩验收测 |
| P0-4 | 旧布局依赖测已同步且通过 | T1–T3 |
| P1-1 | 硬错误多段同规则；计入 failed；exit 2 | 共享路径 + 既有 hard-error；能造多段则补测 |

## 验证

### 命令

```bash
mvn -q test -Dtest=ReportWriterTest,FileRunnerTest,CliReportAcceptanceTest,MainOrchestrationTest
mvn -q clean test
mvn -q -DskipTests package
```

### 最低验证层理由

公开 stdout 布局变更在 `cli` 格式化与明细拼接 → **L3**。不要求 L4 语料硬验收；禁止借机推进 WI-2/WI-3。

### 预期证据

| 验证 | 通过时 |
|---|---|
| T1 Red | 失败指向旧 `at` 缩进和/或块间无空行 |
| T2/T3 | Failures=0；无关 Skipped 入 notes |
| `package` | BUILD SUCCESS |
| P0-1 | 布局匹配 Spec 权威样例（空行与 `at`） |
| 禁止项 | 无 `[i/N]`、`N failures in file` |

### 无法验证（quality.md §6）

JDK/Maven 不可用：未验证项 → 原因 → 风险 → 恢复条件 → 复测范围。P1-1 多段硬错误难造：记共享路径覆盖与残差；恢复条件为可稳定复现的多段 hard-error fixture。禁止静默跳过或编造 Pass。

## Review 门禁与进入 QA

- Review：`required`。
- 进入 QA：T0–T4 完成；L3 绿（或 §6 已记）；`dev-notes.md` 齐；**Reviewer `Approve`**。未 Approve 不得进 QA。

## 文档影响

| 类别 | 更新路径或 N/A |
|---|---|
| 开发文档 | 本目录 `dev-notes.md`；本 `plan.md` |
| 用户文档 | `README.md`、`README.zh-CN.md`「报告」（无缩进 `at`；建议多失败空行样例） |
| 运维文档 | N/A（无部署/排障变更） |

## 交接顺序

1. Planner：本 plan → Manager；**停 Plan 确认**（不改状态、不调度 Developer）。
2. Manager：确认持久化 → `planned` → Developer。
3. Developer：分支执行 T0–T4；写 notes；勿擅自 commit。
4. Reviewer：Spec + Plan + notes → Approve / 回退。
5. QA：`qa-report.md` Pass/Fail/Blocked。
6. 合并：仅用户授权后由 Manager 处理。

## 开放问题

**无。**

## 修订记录

| 日期 | 摘要 |
|---|---|
| 2026-08-06 | 初稿并 refine：T0→Red→Green→Verify→notes；Design skipped；冻结合同入任务/验收 |
