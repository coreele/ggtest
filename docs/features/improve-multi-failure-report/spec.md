# Spec: improve-multi-failure-report

> 需求与规格（Plan 之前完成）。任务拆解见后续同目录 `plan.md`。
>
> **feature-id**：`improve-multi-failure-report` · **sub-feature-id**：`improve-multi-failure-report`（未拆分）
> **适用对象**：Planner、Developer、Reviewer、QA、Manager。
> **前置条件**：工作项 `[docs/manager/improve-multi-failure-report.md](../../manager/improve-multi-failure-report.md)`；归档基线 `[docs/archive/2026/ggtest-cli-report/spec.md](../../archive/2026/ggtest-cli-report/spec.md)`（失败内联、`TOTAL` 文件级计数、退出码；**未**定义单文件多失败分隔）。
> **阅读顺序**：背景与目标 → 非目标 → 范围与可见行为 → 合同 → 验收 → 开放问题。
> **预期结果**：多失败呈现合同已冻结；可进入 Plan。
> **失败处理**：偏离冻结合同须先修订本 Spec；不得改 TOTAL 文件级计数或退出码。
>
> **Spec 状态**：**用户已拍板**（2026-08-06）。Q1–Q8 已决。建议 Manager：`Spec approved` → 调度 **planner**。

## 背景与目标

### 用户现象

同文件多条断言失败时，连续 `[WHY]` 块难扫读：块间无空行，且 `at` 与体同缩进，难分边界。

### 根因（代码现状）

| 位置 | 行为 |
| --- | --- |
| `FileRunner.runWithExecutor` | 每个 `FAILED` → `formatFailureDetailLines` 后 `detailLines.addAll`；**块间无空行** |
| `CliSession` | 打印该文件全部 `detailLines` 后**仅一次**空行（文件级） |
| `ReportWriter.detailLines` | 缩进 `[WHY]` / `[SQL]` / 可选 `[Diff]` / **缩进** `at file:line` |
| 归档 ggtest-cli-report | `TOTAL.failed` = **文件数**；**未**冻结多失败分隔 |

非 fatal abort 时 runner 继续跑文件内其余记录，可累积多条失败——呈现未跟上。

### 目标

只改报告呈现：**块间空行（S2）** + **`at <file>:<line>` 无前导缩进**。相对 ggtest-cli-report 的增量合同。不改断言/溢出/精度语义与文件级汇总。

### 策略映射（已决）

**仅 S2 + 无缩进 `at`**。拒绝 S1（`[i/N]`）、S3（`N failures in file`）、S4（折叠）、S5（行号优先标题）。

## 非目标

- **禁止**纳入 WI-2 `fix-aggfunc-sum-overflow` / WI-3 `fix-aggfunc-total-precision`。
- **禁止**改变「断言失败后继续跑完文件内其余记录」的 runner 语义（fatal abort / halt 不变）。
- **禁止**把 `TOTAL.failed` 改为记录/失败条数；退出码语义不变。
- **禁止** S1/S3/S4/S5 与新增 CLI 标志（细则见合同）。
- 不新增 JSON/JUnit 等结构化报告；不改 parser / 比较 / 规范化 / 执行器业务逻辑。
- 不重写 README 全文；Plan 可同步「报告」小节多失败样例。

## 范围与可见行为

1. **N≥2**：相邻失败块之间插入**恰好一空行**；每块为缩进 `[WHY]` / `[SQL]` / 可选 `[Diff]` + **无前导缩进** `at <file>:<line>`。
2. **N=1**：同样无缩进 `at`；无块间空行。
3. **继承**：状态行 `.. [FAILED] in N ms`；相对 CWD；失败内联；`Error:` 仅列失败文件；彩色不变。
4. **硬错误**：多段 `detailLines` 套用同一规则。
5. **测试**：同步布局断言（如 `ReportWriterTest` / `FileRunnerTest` / 编排类），以冻结样例为准。

## 合同

> **增量**合同；未列项继承 ggtest-cli-report。下列为**已冻结**呈现规则。

### 冻结期望样例（权威）

```text
examples/multi.slt                                           .. [FAILED] in 40 ms
    [WHY] query execution failed: ... integer overflow ...
    [SQL] ...
at examples/multi.slt:480

    [WHY] query execution failed: ... integer overflow ...
    [SQL] ...
at examples/multi.slt:484

    [WHY] query result mismatch:
    [SQL] ...
    [Diff] (-expected|+actual)
        ...
at examples/multi.slt:491

Error: some test case failed:
[
    "examples/multi.slt",
]
```

### API / 接口

| 项 | 合同 |
| --- | --- |
| 库 API | N/A |
| CLI 标志 | **禁止**新增 |
| 彩色 | 继承基线；不另立新标签语义 |

### 数据 / 状态

| 项 | 合同 |
| --- | --- |
| Runner | **不改变**继续执行/abort/halt；只改失败明细格式化与拼接 |
| `TOTAL.failed` | **仍为文件数**（同文件 3 条失败 → `failed=1`） |
| 退出码 | **不变**：断言失败 → `1`；硬错误 → `2` |
| 失败完备性 | 每条须含：原因、SQL 首行（基线）、可选 Diff、相对路径与行号 |

### 错误与约束（呈现）

| 项 | 合同 |
| --- | --- |
| 块间分隔 | N≥2 时相邻失败块之间**必须**一空行；N=1 无块间空行 |
| `at` 行 | `at <file>:<line>`（无行号则 `at <file>`）**必须**相对报告左对齐、**无前导缩进**；仅改 `at` 行 |
| 体缩进 | `[WHY]` / `[SQL]` / `[Diff]` **保持**现有缩进 |
| 禁止项 | **禁止** `[i/N]`、`N failures in file`、折叠、行号优先标题、新 CLI 标志 |
| 单失败 | 同样无缩进 `at`；无索引、无摘要 |
| 凭据 | 禁止写入报告 |
| 兼容 | 新布局 + 同步改测试 |

### 现状 vs 目标

| 场景 | 现状 | 目标（已冻结） |
| --- | --- | --- |
| 单文件 1 失败 | 缩进体 + 缩进 `at` | 缩进体 + **无缩进** `at` |
| 单文件 N≥2 | 无间隔；缩进 `at` | 块间空行 + 无缩进 `at`；无索引/摘要 |
| `TOTAL.failed` | 文件数 | 仍为文件数 |
| 溢出/精度文案 | 如实报告 | **只改排版** |

## 验收（Given-When-Then）

**前置**：JDK 17+；可运行 `ggtest`；fixture 可自造。布局以权威样例为准。

### P0

- **P0-1 多失败块间空行 + 无缩进 `at`**  
Given 同文件恰好 **3** 条断言失败  
When 运行 CLI  
Then 退出码 `1`；`TOTAL.failed=1`；相邻失败块之间各有一空行；每块含缩进 `[WHY]`/`[SQL]`（及可选 `[Diff]`）与**无前导缩进**的 `at <file>:<line>`；**禁止** `[i/N]` 与 `N failures in file`；禁止整段单行 `reason=`。

- **P0-2 单失败无缩进 `at`、无回归**  
Given 1 文件、1 条断言失败  
When 运行 CLI  
Then 满足基线完备性；`at` 无前导缩进；无索引、无摘要、无块间空行；`TOTAL.failed=1`；退出码 `1`。

- **P0-3 计数与退出码不变**  
Given 2 个失败文件（其一多条记录失败，另一 1 条）  
When 运行 CLI  
Then `TOTAL.failed=2`；退出码 `1`；`Error:` 仅列这 2 个路径。

- **P0-4 测试同步**  
Given 依赖失败块拼接或 `at` 缩进形态的测试  
When 合并前跑相关测试  
Then 已按冻结合同更新且通过。

### P1

- **P1-1 硬错误多段**  
Given 硬错误产生多段 detail  
When 运行 CLI  
Then 块间空行与无缩进 `at` 与断言失败一致；计入 `TOTAL.failed`；退出码 `2`。

## 开放问题

**无。** Q1–Q8 已决：

| 编号 | 决议 |
| --- | --- |
| Q1 | `TOTAL.failed` **仍为文件数** |
| Q2 | **仅 S2 + 无缩进 `at`**；拒绝 S1/S3/S4/S5 |
| Q3 | 单失败：无索引/摘要；**同样**无缩进 `at` |
| Q4 | **不做**折叠（S4） |
| Q5 | **不新增** CLI 标志 |
| Q6 | 硬错误多段 **套用同一规则** |
| Q7 | 无新摘要/索引文案；现有标签保持英文 |
| Q8 | **不采用** S5 |

---

**交接**：`docs/features/improve-multi-failure-report/spec.md`。建议下一步：`Spec approved` → **planner**（Design skipped）。不启动 WI-2/WI-3。
