# Spec: ggtest-cli-report

> 需求与规格（Plan 之前完成）。任务拆解见后续同目录 `plan.md`。
>
> **feature-id**：`ggtest-cli-report` · **sub-feature-id**：`ggtest-cli-report`（未拆分）
> **适用对象**：Planner、Developer、QA、Manager（合同已冻结；无需再向用户确认开放问题）。
> **前置条件**：工作项 [`agents/docs/manager/ggtest-cli-report.md`](../../manager/ggtest-cli-report.md)（权威决议 Q-R1…Q-R8）；归档 [`agents/docs/archive/2026/ggtest-core/ggtest-core-cli-corpus/spec.md`](../../archive/2026/ggtest-core/ggtest-core-cli-corpus/spec.md)（纯文本；失败须含文件、行号、SQL 首行、失败原因）；基线 `CliSession`、`ResultComparer#buildDiffSummary`。
> **阅读顺序**：背景与目标 → 非目标 → 范围与可见行为 → 合同 → 验收 → 开放问题。
> **预期结果**：人类可读成功/失败报告合同已冻结（含状态行 `[PASSED]`/`[FAILED] in`/`[SKIPPED]`、`[SQL]` 首行/` ...` 规则、混合样例、文件级计数、`--color` / `ggtest.color` / `GGTEST_COLOR`）；可进入 / 修订 Plan。
> **失败处理**：实现或 Plan 偏离本 Spec 须回退 Spec/用户确认；不得静默改变退出码或计数语义。
>
> **Spec 状态**：已批准。本轮合入前增量（失败块 `[SQL]` 多行省略标记；用户已拍板）并入 Plan 再审；**不要**再要求整份 Spec / 开放问题重新确认——**请用户再审 Plan**。

## 背景与目标

### 用户现象（现状）

成功（绝对路径、`FILE:` 前缀、**query 级**计数）：

```text
FILE: /Users/zhougangjie/Space/ggtest/examples/demo.slt passed=8 failed=0 skipped=0
TOTAL: passed=8 failed=0 skipped=0
```

失败（`reason=` 将 expected/actual 挤在单行）：

```text
FILE: .../examples/demo.slt passed=7 failed=1 skipped=0
FAILURE: file=.../demo.slt line=27 summary=SELECT id FROM items ORDER BY id reason=result mismatch: expected (3 lines):   1   2   30 actual (3 lines):   1   2   3 first difference at line 3: expected=30 actual=3
TOTAL: passed=7 failed=1 skipped=0
```

### 根因（变更前）

- 成功/总计：`FILE: <path> passed=N failed=N skipped=N` + `TOTAL: ...`（**N = query 数**）。
- 失败：`FAILURE: ... reason=<单行>`；`buildDiffSummary` 已多行，但 `CliSession#singleLine()` 压入 `reason=`。
- 硬错误：`ERROR: ...`；退出码 `2`。退出码语义：`0`/`1`/`2`（全通过 / 有失败 / 硬错误）。凭据不得进入输出。

### 目标

提升成功与失败报告可读性；保留归档完备性（文件、行号、SQL 首行、原因）；以用户确认样例为冻结合同；**计数改按文件**（边界见「文件级计数与退出码」）；彩色经 `--color` / 系统属性 `ggtest.color` / env `GGTEST_COLOR`（默认 `auto`：TTY 彩色、非 TTY 降级）；Q-R5=(B) 同步改测试。

## 非目标

- 改变退出码 `0`/`1`/`2` 语义（Q-R6 已确认不变）。
- 结构化报告（JSON、JUnit XML 等）。
- 除 `--color` 外默认不新增其他 CLI 标志。
- 改变比较/规范化/排序/哈希语义（只改呈现；禁止破坏可读性的单行折叠）。
- 改动 parser / runner / 执行器业务逻辑。
- 重写 README 全文或归档 cli-corpus Spec（Plan 可声明同步用户文档示例）。
- 提交 `examples/` 未跟踪语料/demo；创建或提交真实 `.env`。

## 范围与可见行为

### 路径（Q-R1）

报告路径为**相对 CWD**（如 `examples/demo.slt`）。

### 状态行标签与介词（Q-R2、Q-R3；输出合同增量 2026-07-25）

覆盖此前 `PASS in N ms` / `[FAILED] after N ms` / 短暂使用的 `[OK]`：

| 状态 | 状态行合同 | 对应 TOTAL 键 |
|---|---|---|
| **PASSED（成功）** | `<path> .. [PASSED] in N ms`（路径列对齐；`..` + 方括号标签） | `passed=` |
| **FAILED** | `<path> .. [FAILED] in N ms`（介词一律 **`in`**，**禁止** `after`） | `failed=` |
| **SKIPPED** | `<path> .. [SKIPPED]`（**不写耗时**） | `skipped=` |

三组标签与 TOTAL 键一一对应：`PASSED`/`passed`、`FAILED`/`failed`、`SKIPPED`/`skipped`。`TOTAL:` 行本身格式不变。**禁止**状态行使用 `[OK]`。

### 成功与 TOTAL（Q-R2）

- 逐文件一行；**无 `FILE:`**；状态行见上表 `[PASSED]` / `[SKIPPED]`。
- 末尾 `TOTAL: passed=N failed=N skipped=N`。
- **关键：计数按文件**（非 query）；验收/测试须按文件断言。边界与退出码见下节。

### 文件级计数与退出码（Q-R2、Q-R6、Q-R8；Plan 确认意见 2026-07-25）

覆盖此前 Plan 推导「硬错误文件不计入 passed/failed/skipped」：

| 桶 | 规则 |
|---|---|
| **failed** | 含 ≥1 条断言失败记录的文件，**或**发生硬错误的文件 |
| **passed** | 执行完毕且无断言失败、**无硬错误**的文件 |
| **skipped** | 文件内全部可断言记录被 skip，**且无硬错误** |
| **TOTAL** | `passed`/`failed`/`skipped` 均为文件数；硬错误文件计入 `failed` |

**退出码与计数独立判定**（Q-R6）：

- 存在硬错误（含用法/配置/解析/连接/隔离等）→ 退出码 **`2`**（即使 `TOTAL.failed` 已计入该文件）。
- 无硬错误、仅有断言失败 → 退出码 **`1`**。
- 无硬错误且 `failed=0` → 退出码 **`0`**。

### 失败与混合顺序（Q-R3、Q-R7）

- 状态行见上表 `[FAILED]`（无 `FILE:`）。
- **紧随**该文件内联缩进块：`[WHY]`、`[SQL]`、`[Diff] (-expected|+actual)`（未变无前缀、期望 `-`、实际 `+`，带上下文）、`at <file>:<line>`。
- **`[SQL]` 呈现**（合入前增量 · 2026-07-25）：取 SQL **第一行**（去尾随空白）显示；若去除首行后仍存在非空白内容（多行或后续有意义文本），在显示首行末尾追加一个空格 + `...`，即 `<首行> ...`；纯单行 SQL（无后续非空内容）**不加** ` ...`。
- **禁止**整段单行 `reason=`。
- **输出顺序**：按发现/参数顺序逐文件状态行；失败详情紧跟该文件；成功/跳过行间**不插额外空块**；末尾 `Error: some test case failed:` + **仅列失败文件** → 再 `TOTAL:`。

### statement 失败

同失败视觉体系：`[WHY]`/`[SQL]`（无结果 diff 时可省略 `[Diff]`）；含相对路径与行号；`[SQL]` 同合同呈现规则。不改变「只断言失败事实、不做消息/正则匹配」语义。

### 彩色（Q-R4）

- CLI：`--color <WHEN>`，`WHEN` ∈ `auto` \| `always` \| `never`，**默认 `auto`**。
- 系统属性：`ggtest.color`（`-Dggtest.color=auto|always|never`）。
- 环境变量：`GGTEST_COLOR`（同取值）。
- **优先级**：显式 CLI `--color` > 系统属性 `ggtest.color` > env `GGTEST_COLOR` > 默认 `auto`。
- `auto`：TTY 彩色；非 TTY（管道/重定向/CI）降级纯文本。
- `always`：强制 ANSI，不做 TTY 探测。
- `never`：强制纯文本，无 ANSI。
- 曾用名 `GGTEST_TERM_COLOR`、`CARGO_TERM_COLOR` 已弃用，不作现行合同。

### 硬错误（Q-R8）

一并重排为与失败块一致的视觉体系（多行头 + 相对路径）；硬错误文件**计入** `TOTAL.failed`；退出码仍 `2`（与计数独立）；配置/用法错误**不得**冒充成功统计，且无单独样例基线（用户已确认维持此方案；验收以 P1-3 为准）。

### 安全

纯文本或按 Q-R4 含 ANSI；stdout/stderr **禁止**凭据明文。

## 合同

### API / 接口

| 项 | 合同 |
|---|---|
| 库 API | N/A（不新增对外库 API）。 |
| CLI `--color` | `--color <auto\|always\|never>`；默认 `auto`。 |
| 系统属性 | `ggtest.color`（`-Dggtest.color=auto\|always\|never`）。 |
| 环境变量 | `GGTEST_COLOR`（`auto\|always\|never`）。 |
| 彩色优先级 | 见「彩色」：CLI > `ggtest.color` > `GGTEST_COLOR` > `auto`。 |

### 数据 / 状态

N/A（不改变执行/隔离语义；只改 stdout/stderr 布局与**文件级**汇总计数）。

### 错误与约束

| 项 | 合同 |
|---|---|
| 退出码（Q-R6） | `0` 全通过；`1` 仅断言失败、无硬错误；`2` 用法/配置/解析/连接/隔离等硬错误。**不变**，且与计数**独立**判定（硬错误 → 码 `2`，即使已计入 `TOTAL.failed`）。 |
| 计数（Q-R2） | `TOTAL` 与摘要中 `passed`/`failed`/`skipped` = **文件数**（相对现状 query 数为语义变更）。边界：failed = 含失败记录或硬错误的文件；passed = 执行完毕且无失败、无硬错误；skipped = 全部可断言记录被 skip 且无硬错误。 |
| 路径（Q-R1） | 相对 CWD。 |
| 状态行标签 | 成功 `.. [PASSED] in N ms`；失败 `.. [FAILED] in N ms`（禁止 `after`）；跳过 `.. [SKIPPED]`（无耗时）。禁止旧式 `[OK]` / `PASS in` / `after` / `FILE:`。 |
| 失败完备性 | 文件、行号、SQL 首行（见 `[SQL]` 呈现）、失败原因。继承归档 cli-corpus。 |
| `[SQL]` 呈现 | 首行（去尾随空白）；去除首行后仍有非空白 → 显示 `<首行> ...`；纯单行不加 ` ...`。 |
| 差异（Q-R3） | git 风格 `(-expected|+actual)`；禁止整段单行折叠。 |
| 顺序（Q-R7） | 按发现/参数顺序：每文件状态行 → 该文件详情（若有）→ 成功/跳过行间无额外空块 → `Error:` **仅列失败文件** → `TOTAL:`。 |
| 彩色（Q-R4） | 见「彩色」与 API：`auto` TTY 彩色/非 TTY 纯文本；`always` 强制 ANSI；`never` 强制纯文本。 |
| 硬错误（Q-R8） | 同视觉体系；文件**计入** `TOTAL.failed`；退出码仍 `2`；不得冒充成功统计。 |
| 纯文本 | 不要求 JSON/JUnit XML。 |
| 凭据 | 禁止写入报告或日志。 |
| 比较语义 | 规范化与比较结果不变；只改呈现。 |
| 兼容（Q-R5） | **(B)**：新格式 + **同步修改**受影响测试。 |

### 现状 vs 目标

| 场景 | 现状 | 目标（已决） |
|---|---|---|
| 成功 | `FILE: <abs>` + query 计数 | 相对路径；无 `FILE:`；`.. [PASSED] in N ms`；**文件计数** + `TOTAL:` |
| 结果不匹配 | `FAILURE: ... reason=<单行>` | `.. [FAILED] in N ms` + `[WHY]`/`[SQL]`（首行；多行 `<首行> ...`）/`[Diff]` + `at file:line`；Error **仅失败文件** + `TOTAL:` |
| 混合 | （旧单行混排） | 发现/参数顺序；失败内联；成功行间无空块；Error 仅失败；再 TOTAL |
| 跳过 | （含于 `FILE:` 计数） | `.. [SKIPPED]`（无耗时）；计入 `TOTAL.skipped` |
| statement | 单行 `reason=` | 同失败视觉体系（`[FAILED] in`）；`[SQL]` 同首行/` ...` 规则 |
| 硬错误 | `ERROR:` / `FILE: ...(error)`；码 `2` | 同视觉体系；计入 `TOTAL.failed`；码仍 `2`；勿误报成功 |
| 彩色 | 无开关 | `--color` + `ggtest.color` + `GGTEST_COLOR`；默认 `auto` |
| 测试 | 旧 `FILE:`/`TOTAL` query 语义 | Q-R5=(B) 改断言为新格式与文件计数 |

### 目标输出样例（合同基线；用户确认）

冻结合同：耗时可变；布局/标签/顺序不得偏离。

**成功：**

```text
examples/demo.slt                                            .. [PASSED] in 5 ms
examples/demo2.slt                                           .. [PASSED] in 6 ms

TOTAL: passed=2 failed=0 skipped=0
```

`passed=2` = **2 个文件**通过（与文件内 query 数无关）。

**失败（参考 sqllogictest-rs；`[SQL]` 与 fixture 对齐：`SELECT name` 换行 `FROM items` → 多行省略）：**

```text
examples/demo.slt                                            .. [FAILED] in 18 ms
    [WHY] query result mismatch:
    [SQL] SELECT name ...
    [Diff] (-expected|+actual)
        apple
    -   bananad
    +   banana
        cherry
    at examples/demo.slt:22

examples/demo2.slt                                           .. [FAILED] in 3 ms
    ...

Error: some test case failed:
[
    "examples/demo.slt",
    "examples/demo2.slt",
]

TOTAL: passed=0 failed=2 skipped=0
```

`failed=2` = **2 个文件**失败；第二文件块中的 `...` = 同结构占位（非 `[SQL]` 省略标记）。

**混合（台账冻结；顺序规则见 Q-R7）：**

```text
examples/demo.slt                                            .. [FAILED] in 18 ms
    [WHY] query result mismatch:
    [SQL] SELECT name ...
    [Diff] (-expected|+actual)
        apple
    -   bananad
    +   banana
        cherry
    at examples/demo.slt:22

examples/demo2.slt                                           .. [PASSED] in 6 ms
examples/select1.test                                        .. [PASSED] in 142 ms

Error: some test case failed:
[
    "examples/demo.slt",
]

TOTAL: passed=2 failed=1 skipped=0
```

`Error` 仅 `"examples/demo.slt"`；`passed=2 failed=1` 为文件计数。

**跳过（示意）：**

```text
examples/skip-me.slt                                         .. [SKIPPED]
```

**statement 失败（下例 SQL 为单行，故不加 ` ...`；若 statement SQL 多行则加）：**

```text
examples/demo.slt                                            .. [FAILED] in 4 ms
    [WHY] statement expected to succeed but failed: <engine error summary>
    [SQL] INSERT INTO items VALUES (...)
    at examples/demo.slt:15

Error: some test case failed:
[
    "examples/demo.slt",
]

TOTAL: passed=0 failed=1 skipped=0
```

### 兼容策略（Q-R5=(B)）

| 测试 / 文档 | 耦合点 | 动作 |
|---|---|---|
| `CorpusHardAcceptanceTest` | `TOTAL:.*failed=(\d+)`；`TOTAL:`、文件名 | 保留 `TOTAL:`；断言改**文件计数**；去旧 `FILE:` / `PASS in` / `[OK]` 依赖 |
| `MainOrchestrationTest` | `countFailures`/`extractPassed` 等 | 按新格式（`[PASSED]`/`[FAILED] in`）与文件计数更新 |
| `EnvConfigIntegrationTest` | 配置错误 `!stdout.contains("FILE:")` | 按新合同调整；仍断言不得冒充成功统计 |
| `ResultComparerTest` | `diffSummary` 含 `expected`/`actual` | 比较器语义不变；git 风格或上移报告层，呈现断言按需改 |
| 归档 cli-corpus Spec | 文件、行号、SQL、原因 | 新布局须仍完备 |

Plan **必须**含任务：「识别并修改受影响测试」。

## 验收（Given-When-Then）

**前置**：JDK 17+、可运行 `ggtest`；SQLite 内存库；本地 `examples/*.slt` 或自造 fixture（**勿**提交未跟踪语料）。布局以本合同样例为准。

### P0

- **P0-1 成功、文件计数、退出码 0**
  - Given 全通过文件（可多文件）+ SQLite 内存库
  - When `ggtest --url jdbc:sqlite::memory: <路径…>`
  - Then 退出码 `0`；逐文件无 `FILE:` 的 `.. [PASSED] in N ms`（相对 CWD）；`TOTAL: passed=N failed=0 skipped=0` 中 **N = 通过文件数**

- **P0-2 结果不匹配、退出码 1**
  - Given 恰好 **1 个文件**结果不匹配
  - When 同一命令
  - Then 退出码 `1`；`.. [FAILED] in N ms` 后内联 `[WHY]`/`[SQL]`/`[Diff] (-expected|+actual)` 与 `at <file>:<line>`；`[SQL]` = 首行（去尾随空白），多行/后续非空 → `<首行> ...`，纯单行不加；`Error: some test case failed:` 含该路径；`TOTAL.failed` 为文件数；**不得**整段单行 `reason=`；**不得**使用介词 `after`

- **P0-3 凭据不泄漏**
  - Given 带密码的连接配置（不入库）
  - When 任意运行
  - Then stdout/stderr 无密码明文

- **P0-4 测试同步（Q-R5=(B)）**
  - Given 依赖旧格式或 query 计数的测试（至少 `CorpusHardAcceptanceTest`、`MainOrchestrationTest`、`EnvConfigIntegrationTest`；必要时 `ResultComparerTest`）
  - When 合并前跑相关测试
  - Then 已改断言新格式与**文件计数**且全部通过

### P1

- **P1-1 混合场景：顺序、Error 仅失败、混合 TOTAL**
  - Given 多文件既有成功也有失败（可含 skipped）
  - When 执行
  - Then 按发现/参数顺序输出各文件状态行（成功为 `.. [PASSED] in N ms`，失败为 `.. [FAILED] in N ms`，跳过为 `.. [SKIPPED]`）；失败则紧跟内联详情；成功/跳过行间**无额外空块**；`Error: some test case failed:` **仅列失败文件**（不含成功/跳过路径）；`TOTAL` 的 `passed`/`failed`/`skipped` 为文件计数且与实际一致；退出码 `1`（无硬错误时）

- **P1-2 statement 失败**
  - Given `statement ok` 实际失败或 `statement error` 实际成功
  - When 执行
  - Then `.. [FAILED] in N ms` + `[WHY]`/`[SQL]` + 相对路径与行号；`[SQL]` 同呈现规则（单行不加 ` ...`）；退出码 `1`；`TOTAL.failed` 按文件计

- **P1-3 硬错误（Q-R8）**
  - Given 解析/连接/配置/用法错误（含有文件上下文的硬错误文件）
  - When 执行
  - Then 硬错误文件**计入** `TOTAL.failed`；退出码 `2`（与计数独立，即使该文件已计入 `failed`）；相对路径多行硬错误（同视觉体系）；**无**假装全通过统计

- **P1-4 彩色：`always` / `never` / `auto` 非 TTY（Q-R4）**
  - Given 同一失败用例
  - When `--color always` → Then 含 ANSI（不做 TTY 探测）
  - When `--color never` → Then 无 ANSI
  - When 默认或 `--color auto` 且非 TTY → Then 无 ANSI；标签仍符合样例

- **P1-5 彩色：env / 系统属性与优先级（Q-R4）**
  - Given 未显式 `--color`、未设 `ggtest.color`，且 `GGTEST_COLOR=never`
  - When 执行同一失败用例 → Then 无 ANSI
  - Given 未显式 `--color`，`GGTEST_COLOR=always`，且 `-Dggtest.color=never`
  - When 执行 → Then 无 ANSI（属性优于 env）
  - Given env 或系统属性为 `never`，且显式 `--color always`
  - When 执行 → Then 以 CLI 为准（含 ANSI）

## 开放问题

无（全部已决）。本轮合入前增量：失败块 `[SQL]` 多行省略（用户已拍板；不要求整份 Spec 重新确认）。摘要：

| 编号 | 决议 |
|---|---|
| Q-R1 | 路径相对 CWD |
| Q-R2 | 成功 `.. [PASSED] in N ms`（与 `passed=` 成对）；跳过 `.. [SKIPPED]`（无耗时）；无 `FILE:`；`TOTAL:`；**按文件计数**（边界：failed 含硬错误文件；passed/skipped 均要求无硬错误）。禁止旧式 `[OK]` / `PASS in` / `after` / `FILE:` |
| Q-R3 | `.. [FAILED] in N ms`（禁止 `after`）+ `[WHY]`/`[SQL]`/`[Diff]` git 风格 + `at file:line`；`[SQL]` = 首行，多行时 `<首行> ...`，纯单行不加 |
| Q-R4 | `--color` + `ggtest.color` + `GGTEST_COLOR`；优先级 CLI > 属性 > env > `auto`；弃用 `GGTEST_TERM_COLOR` / `CARGO_TERM_COLOR` |
| Q-R5 | **(B)** 新格式 + 同步改测试 |
| Q-R6 | 退出码 `0`/`1`/`2` 不变；与文件计数**独立**判定 |
| Q-R7 | 按发现/参数顺序；失败内联；成功行间无空块；`Error:` **仅列失败文件** → `TOTAL:`（混合样例已冻结） |
| Q-R8 | 硬错误同视觉体系；文件计入 `TOTAL.failed`；退出码仍 `2`；不得冒充成功统计 |

---

**状态**：已批准。增量并入 Plan 再审；**不要**再要求整份 Spec 重新确认。后续：Manager 调度 **Planner** 修订 `plan.md` 后**请用户再审 Plan**。
