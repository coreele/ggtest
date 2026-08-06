# Spec: feat-cli-halt

> 需求与规格（Plan 之前完成）。任务拆解见后续同目录 `plan.md`。
>
> **feature-id**：`feat-cli-halt` · **sub-feature-id**：`feat-cli-halt`（未拆分）
> **适用对象**：Planner、Developer、Reviewer、QA、Manager。
> **前置条件**：工作项 [`docs/manager/feat-cli-halt.md`](../../manager/feat-cli-halt.md)；官方 `sqllogictest/src/sqllogictest.c`（`haltOnError`、`--halt`）；报告/退出码基线 [`docs/archive/2026/ggtest-cli-report/spec.md`](../../archive/2026/ggtest-cli-report/spec.md)、README Exit codes。
> **阅读顺序**：背景与目标 → 非目标 → 范围与可见行为 → 合同 → 验收 → 开放问题。
> **预期结果**：`--halt` 首错停跑合同已冻结；可进入 Plan（Design skipped）。
> **失败处理**：偏离本 Spec 须先修订 Spec；不得静默改变未启用 `--halt` 时的继续执行语义或退出码 `0`/`1`/`2` 含义。
>
> **Spec 状态**：可交接 Planner。**Spec 用户确认**：`not-required`。

## 背景与目标

### 现状

- 官方：`--halt` → `haltOnError`；循环 `while ((nErr==0 || !haltOnError) && findStartOfNextRecord(...))`；Help：`Stop when first error is seen`；**单脚本**。
- ggtest：断言失败后**文件内继续**；`CliSession` **多文件跑完**。语料 `halt` 仅中止**当前文件**后续（skipped），与 CLI 无关。
- 参数解析：精确长选项（`--url`、`--engine` 等）；**无** `strncmp` 前缀匹配、**无**单横杠长选项。

### 目标

CLI 增加与官方语义对齐的 **`--halt`**：见到**第一个错误**即停；默认关闭时行为与现状一致；报告与退出码兼容现有 `Main` / `CliSession`。多文件停跑范围见合同（官方无此模型，由本 Spec 冻结）。

## 非目标

- 不改语料 `halt` 记录语义。
- 不引入前缀匹配或短选项体系（含 `-halt`）。
- 不改断言/比较/规范化/parser；不改 `TOTAL` 文件级计数含义。
- 不新增结构化报告；不实现 `--verify` / `--trace` 等其他官方选项。
- 不写 Design（门禁 skipped）。

## 范围与可见行为

1. 增加布尔 **`--halt`**（默认关）。
2. 未传：与现状一致（文件内继续、多文件跑完；fatal 仅停当前文件后仍处理后续文件）。
3. 已传：本进程**首次**错误后，不执行后续记录、不启动尚未开始的后续文件；已发生失败仍按现有格式报告；未执行记录不得报假失败。
4. README（选项表）简述对齐 *Stop when first error is seen*。
5. 退出码仍为 `0`/`1`/`2`（见合同）。

## 合同

### API / 接口

| 项 | 合同 |
| --- | --- |
| 选项 | 精确 **`--halt`**（无值）。 |
| 短形式/前缀 | **禁止** `-halt`；**禁止**官方式前缀（`--hal`、`-ha` 等）。此类 → usage，退出码 `2`。 |
| 默认 | **关闭**；省略时不得改变继续跑完语义。 |
| 重复 | 多次 `--halt` ≡ 一次（允许，非 usage 错误）。 |
| 库 API | N/A。 |
| 文档 | README 选项表须列 `--halt`，简述对齐官方；无独立 `--help` 全文时以 README 为用户可见合同面。 |

### 数据 / 状态

#### 「错误」定义（触发停跑）

`--halt` 开启时，下列计入本文件失败/硬错误即触发停跑：

| 类别 | 示例 | 退出码 |
| --- | --- | --- |
| 断言失败 | `statement`/`query` 期望不符、执行失败、结果/哈希/label 不一致等 → `RecordOutcome.FAILED`（非 fatal） | **`1`**（本次无 hard error 时） |
| 硬错误 | 解析/IO/连接/schema 隔离或 teardown、`FatalDatabaseException` abort 等 → `FileOutcome.hardError` | **`2`** |

**不**单独触发 CLI `--halt` 停跑：

| 非触发 | 说明 |
| --- | --- |
| 语料 **`halt` 记录** | 中止当前文件后续并 skipped；**非**错误。仅执行 `halt` 且此前无错误 → **继续**后续文件。 |
| `skipif` / `onlyif`、`hash-threshold` 等 | 不计入错误。 |

官方对应：`nErr++` ≈ 上表触发项；语料 `halt` 为 `break` 且不 `nErr++` ≈ 非触发。

#### 停跑范围

| 维度 | 合同 |
| --- | --- |
| 文件内 | 首条触发错误按现有规则报告；其后记录**不得执行**（不得发 SQL），**不得**以 `FAILED` 出现。允许省略或标 skipped（原因须表明因 `--halt`/先前失败未执行）；**禁止**假失败。 |
| 跨文件 | **全局停跑**：触发后不得再打开/解析/执行尚未开始的后续文件；这些文件**不得**出现在状态行/`TOTAL`（非 PASSED/FAILED/SKIPPED）。 |
| 已完成文件 | 停跑前已跑完的文件，状态与计数不变。 |
| 当文件 | 记 `FAILED`（硬错误则 `hardError`）；明细仅含实际发生的失败。 |
| `TOTAL` | 仅计**已处理**文件；未启动文件不入任何桶。 |

#### 报告

| 项 | 合同 |
| --- | --- |
| 已发生失败 | 现有格式：`.. [FAILED] in N ms`、内联 `[WHY]`/`[SQL]`/可选 `[Diff]`、无缩进 `at`、`Error:` 列表、`TOTAL`（继承 cli-report / improve-multi-failure-report）。 |
| 未执行记录 | **禁止**失败块；**禁止**抬高虚假失败条数。 |
| 未启动文件 | **禁止**状态行与 `TOTAL` 计入。 |
| 凭据 | 禁止写入报告。 |

### 错误与约束

#### 退出码（`Main` / `CliSession`）

| 码 | 含义（不变） | 与 `--halt` |
| --- | --- | --- |
| `0` | 已处理文件无断言失败且无硬错误 | 无错误不产生非 0。 |
| `1` | 有断言失败文件、无硬错误 | 因断言失败停跑 → **`1`**。 |
| `2` | usage/配置/解析/连接/fatal 等 | 因硬错误停跑 → **`2`**；非法选项 usage → `2`。 |

优先级不变：曾有 hard error → `2`；否则有失败文件 → `1`；否则 `0`。

#### CLI `--halt` vs 语料 `halt`

| | CLI `--halt` | 记录 `halt` |
| --- | --- | --- |
| 位置 | 命令行 | 脚本正文 |
| 触发 | 首个断言失败或硬错误 | 执行到该指令（可被条件跳过） |
| 作用域 | 整次调用（含跨文件） | 当前文件 |
| 算错误 | 是 | 否 |
| 后续 | 不执行；不得假失败 | 不执行；计 skipped |

## 验收（Given-When-Then）

### P0

- **P0-1 默认关闭**  
  Given 含多条会失败 assertable 的单文件，argv **无** `--halt`  
  When 运行 ggtest  
  Then 后续失败仍执行并报告；无硬错误时退出码 `1`。

- **P0-2 单文件首错即停**  
  Given 同文件先一条断言失败、其后仍有记录，argv 含 `--halt`  
  When 运行 ggtest  
  Then 仅报告已发生失败；其后不执行且不以 `FAILED` 出现；退出码 `1`。

- **P0-3 多文件全局停**  
  Given 两文件，第一含断言失败、第二任意，argv 含 `--halt`  
  When 运行 ggtest  
  Then 第一文件失败报告；第二无状态行、不计入 `TOTAL`；退出码 `1`。

- **P0-4 硬错误 → 2**  
  Given `--halt`，某文件开跑后硬错误（fatal abort / 解析失败等）  
  When 运行 ggtest  
  Then 后续文件不启动；退出码 `2`；硬错误按现有方式呈现。

- **P0-5 选项解析**  
  Given 精确 `--halt` → 开启停跑。  
  Given `-halt` 或 `--hal` → usage，退出码 `2`，不连库。

- **P0-6 与语料 `halt` 区分**  
  Given 文件：成功记录 + 会执行的 `halt` + 其后记录；argv 含 `--halt`；另有第二文件  
  When 运行 ggtest  
  Then 第一文件因记录 `halt` 中止后续（skipped），**不**因此非 0；第二文件仍执行；无其他失败则退出码 `0`。

### P1

- **P1-1 文档**  
  Given README 选项表  
  When 查找 `--halt`  
  Then 存在且简述对齐 *Stop when first error is seen*。

- **P1-2 重复 `--halt`**  
  Given 两次 `--halt`  
  When 合法输入  
  Then 同单次 `--halt`（非 usage 错误）。

## 开放问题

无。短选项/前缀、跨文件全局停、错误定义与语料 `halt` 区分均已冻结。
