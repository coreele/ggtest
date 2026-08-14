# Design: feat-override-results

> 技术设计（模块边界、分层、技术选型）。任务拆解见同目录 `plan.md`。
>
> **行为合同权威**：[`spec.md`](./spec.md)（已 approved）。本文件**不**重述 API 形状 / 数据约束 / 错误约定 / 验收，仅决策「需要决策的技术问题」并给出模块改动面。
> **范围**：`full` · Design 门禁 `required` · Review 门禁 `required`。
> **依据调研**：`cli`（`CliArgumentParser`/`ParsedArguments`/`RuntimeConfigResolver`/`CliOptions`/`CliSession`/`FileRunner`/`Main`/`ReportWriter`/`ReportStyle`/`FileOutcome`）、`runner`（`SqlLogicTestRunner`/`RecordResult`/`RecordOutcome`/`FileRunResult`）、`parser`（`SqlLogicTestParser`）、`model`（`QueryRecord`/`StatementRecord`/`SourceLocation`）、`normalize`（`ResultComparer`）、`examples/demo.slt`、先例 `archive/2026/feat-cli-halt/`。

## 设计目标

为 `--override`（golden-update）提供一条**最小侵入、行为可证**的实现路径：在既有「parse → run → report」管线上**新增**一条「判定 in-scope mismatch → 透传 golden 文本 → 按源区间原样重写文件其余字节 → 原子落盘」的旁路，不改变 parser / 比较 / 规范化语义，不改变默认关闭时的任何可观察行为。

设计原则（与 Spec 一致）：

1. **复用既有 actualView**：override 不重新计算 golden，只搬运 runner 已有的 `ResultComparer.CompareResult.actualView()` 与 `StatementResult.errorSummary()`。
2. **区间定位以 parser 为权威**：源文件字节级重写须精确知道每条记录 expected 区间；该信息在 parser 逐行解析时天然可知，交由 parser 携带，不在写回时重扫。
3. **默认关闭 = 现状字节级一致**：任何输出 / 文件 / 退出码变化仅在 `--override` 开启时发生。

## 模块改动面概览

| 包 · 类 | 改动 | 职责边界（不变） |
|---|---|---|
| `cli.CliArgumentParser` | 修改：`switch` 新增 `case "--override"` | argv 精确长选项解析；不读 env/文件 |
| `cli.ParsedArguments` | 修改：新增 `boolean override` | argv-only 结果 |
| `cli.RuntimeConfigResolver` | 修改：透传 `parsed.override()` → `CliOptions` | 合并 CLI/env/.env；`--override` 仅 CLI 来源 |
| `cli.CliOptions` | 修改：新增 `boolean override`；`toString` 补字段（脱敏风格不变） | 运行时配置 |
| `parser.SqlLogicTestParser` | 修改：为 `QueryRecord`/`StatementRecord` 计算并填充 expected 区间 | 解析与记录边界唯一权威；不连库、不比较 |
| `model.QueryRecord` | 修改：新增 expected 区间字段 + 保留旧签名的次构造器 | query 记录值类型 |
| `model.StatementRecord` | 修改：新增 error 消息列号字段 + 保留旧签名的次构造器 | statement 记录值类型 |
| `runner.RecordOutcome` | 修改：枚举新增 `OVERRIDDEN` | assertable 记录判定 |
| `runner.RecordResult` | 修改：新增 override 文本载荷 + `overridden(...)` 工厂 | 单记录判定 + 报告材料 |
| `runner.FileRunResult` | 修改：新增 `overriddenCount()` | 文件级汇总 |
| `runner.SqlLogicTestRunner` | 修改：新增 `overrideEnabled`；in-scope 判定产 `OVERRIDDEN`（复用 actualView） | 单文件执行与判定；不改比较/规范化语义 |
| `cli.FileBucket` | 修改：枚举新增 `OVERRIDDEN` | 文件级桶 |
| `cli.FileOutcome` | 修改：新增 `overridden(...)` 工厂 | 文件级结果 |
| `cli.ReportStyle` | 修改：新增 `overriddenTag()` | 状态行 tag 着色 |
| `cli.ReportWriter` | 修改：`printTotal` 支持条件追加 `overridden=N` | 报告排版 |
| `cli.FileRunner` | 修改：运行后收集 override → 调 `OverrideWriter` → 桶判定 | 单文件接入点（parse/JDBC/runner/写回） |
| `cli.CliSession` | 修改：`OVERRIDDEN` 桶分支 + `totalOverridden` + 条件 TOTAL | 多文件编排 / 退出码 |
| `cli.OverrideWriter`（**新增**） | 新增：文本重写 + 原子落盘 | 唯一触碰磁盘重写的类 |
| `cli.Main` | 无改动（`options` 已贯穿） | 入口 |
| `README.md` / `README.zh-CN.md` | 修改：选项表 + synopsis 新增 `--override` | 用户文档 |

新增类仅 1 个（`OverrideWriter`）；其余为既有类的字段 / 分支 / 工厂扩展。模型类以**次构造器保留旧签名**，使 `SqlLogicTestRunnerTest` / `ReportWriterTest` 等既有构造点零改动编译通过（见 D1）。

## 关键技术决策

### D1 源区间定位 — 选「parser 携带区间」（方案 A），最小化模型面

**选型**：parser 在逐行解析时为每条记录计算 expected 区间并填入记录，写回器据此做字节级替换。

- `QueryRecord` 新增两字段（1-based，`hasExpectedResults == false` 时均为 `0`）：
  - `expectedHeaderLine`：`----` 头所在行号；
  - `expectedBodyEndLine`：expected 块最后一行行号（body 起始 = `expectedHeaderLine + 1`；块为空时 `bodyStart > expectedBodyEndLine`，表示插入位）。
- `StatementRecord` 新增一字段：
  - `errorMsgStartColumn`：在**去掉行尾 CR 的原始 header 行**中，`error` 之后消息片段起始的 0-based 字符列号（保留前导空白与 token 间原始分隔）；无消息（`statement error` 后无片段）时为 `-1`。

**理由**：parser 是记录边界的唯一权威；区间在解析时天然可知（`LineBuffer` 已带 1-based 行号），零额外扫描；字节级精确，天然满足 P0-5（文件其余字节不变）与换行风格保留（P0-5/合同）。判定逻辑只读区间，不依赖内容匹配，杜绝重复内容碰撞。

**最小化模型面**：`QueryRecord` / `StatementRecord` 各**新增一个保留旧签名的次构造器**（缺省区间字段委托 canonical 构造器），使 `SqlLogicTestParser`（主）填区间、而 `SqlLogicTestRunnerTest` / `ReportWriterTest` 既有 `new QueryRecord(...)` / `new StatementRecord(...)` 调用点零改动。

**备选（否决）**：
- 方案 B「写回前用原始行重定位」：需在写回器里复制 parser 的记录边界识别逻辑，且内容匹配在多记录 / 重复行 / 空块时脆弱，违背「最小侵入」与 P0-5 的可证性。
- 混合（parser 仅给行号、写回器仍按内容校验）：徒增复杂度，无额外保证。

### D2 golden 文本透传与 overridden 判定 — 新增 `RecordOutcome.OVERRIDDEN` + `RecordResult` 载荷；判定在 runner

**选型**：

- `RecordOutcome` 枚举新增 `OVERRIDDEN`（与 `PASSED`/`FAILED`/`SKIPPED` 区分）。
- `RecordResult` 新增 `Optional<String> overrideText`（统一字符串：query = actualView 各行以 `\n` 拼接；statement = 实际 error summary），并新增工厂 `RecordResult.overridden(record, overrideText)`。
- `FileRunResult` 新增 `overriddenCount()`（仿 `passedCount()`/`failedCount()`）。
- `SqlLogicTestRunner` 构造器新增 `boolean overrideEnabled`（位置与 `haltOnFirstFailure` 一致）；**in-scope 判定在 runner 内**，因为它持有 `comparison.actualView()` 与 `result.errorSummary()`，且能区分失配组件。

**in-scope 判定规则**（仅当 `overrideEnabled`；否则全部维持现状 `FAILED`）：

| 来源 | override 关闭 | override 开启 |
|---|---|---|
| query 纯 result mismatch（`hasExpectedResults && !comparison.passed()`，无 label 冲突 / 无执行失败 / 无类型签名错） | FAILED | **OVERRIDDEN**（载荷 = `actualView` 行集合） |
| query 同时含 label 冲突 | FAILED | FAILED（无单一 golden，scope 外） |
| query 执行失败 / 类型签名不匹配 | FAILED | FAILED（scope 外） |
| `statement error <msg>` 消息子串失配 | FAILED | **OVERRIDDEN**（载荷 = 实际 error summary） |
| `statement error` 实际成功 / `statement ok` 失败（极性） | FAILED | FAILED（scope 外，极性翻转非 golden） |
| execute-only query（无 `----`） | 不比较 | 不比较；不插入 expected 块 |

**实现要点**：`runQuery` 当前把多种失败原因收进同一 `failures` 列表；改为在 `overrideEnabled` 时**分支判定**——仅当失败集合**只含** result mismatch（且 `hasExpectedResults`）才产 `OVERRIDDEN`，否则维持 `FAILED`。`runStatement` 的 `ERROR` 分支类似：仅「实际失败 + expectedMsg 非空且子串失配」才 `OVERRIDDEN`。比较 / 规范化 / 哈希渲染**完全不变**（复用 `comparison.actualView()`）。

**理由**：枚举值 + 载荷字段是最小且类型安全的方式把 golden 文本从 runner 流到写回器；判定放 runner 复用 actualView、零新计算，满足「不改 parser/比较语义」（D8）。不引入新 outcome 类型无法干净表达「该记录非 FAILED 且需写回」，且会让报告/计数分支化复杂。

**备选（否决）**：复用 `FAILED` + 布尔标记会污染既有失败计数与 `--halt` 判定（`haltOnFirstFailure` 按 `FAILED` 触发），需在多处特判，更易错。

### D3 写回接入点与执行时机 — `FileRunner` 文件级「运行后、一次重写、原子落盘」

**选型**：写回唯一接入点在 `FileRunner.runWithExecutor`（持有文件 `Path` 与 `FileRunResult`），紧随 `runner.run` 之后、返回 `FileOutcome` 之前。流程（仅当 `options.override()`）：

1. 若 `result.aborted()` → **不写回**（信息不完整），按既有路径产 `hardFailure`（P1-6）。
2. 从 `result.recordResults()` 收集所有 `OVERRIDDEN` → 列表 `[(record, overrideText)]`。
3. 列表非空时：
   a. 重新读取源文件原始文本（UTF-8；parser 已读过但未保留原文，写回时现读保证一致）；
   b. 调 `OverrideWriter.rewrite(content, overrides)` 得新文本（D4/D7）；
   c. `OverrideWriter.writeAtomically(path, newText)` 原子落盘（D4）。失败 → `hardFailure`（细节含「写回失败」并列出本应 override 的记录），原文件不动（P1-4/P1-7）。
4. 桶判定（顺序）：
   - 既有 `hardFailure`（parse/IO/connection/abort/写回失败）→ `FAILED` + `hardError`；
   - `failedCount() > 0`（剩余 scope 外 FAILED）→ `FAILED`（assertion）；**仍写回**已 override 部分（Spec 文件级规则）；
   - 否则 `overriddenCount() > 0` → **`OVERRIDDEN`**；
   - 否则 `passedCount() == 0 && skippedCount() > 0` → `SKIPPED`；
   - 否则 `PASSED`。
5. 列表为空（无 in-scope mismatch）→ **不打开写、不改 mtime**（P0-4）。

**与 `--halt` 的交互**（实现层）：
- `OVERRIDDEN != FAILED`，故 runner 的 `haltOnFirstFailure`（按 `RecordOutcome.FAILED` 触发）与 `CliSession` 的 `bucket == FAILED` 停跑检查**都自然忽略** in-scope override（Spec：in-scope 不触发 `--halt`）。
- scope 外 `FAILED` 仍按既有 `--halt` 合同在文件级与跨文件级触发停跑。
- 「停跑前已 override 的记录仍写回」：写回发生在 `FileRunner`（文件级、`runner.run` 返回后），先于 `CliSession` 的跨文件停跑决策；同文件内 `haltOnFirstFailure` 停跑后 `runner.run` 仍正常返回（含已产出的 `OVERRIDDEN`），写回照常进行（P1-5）。

**与 aborted 的交互**：第 1 步短路，**任何已计算的 override 都不落盘**（P1-6）。

**理由**：`FileRunner` 是文件级自然接入点；「收集 → 一次重写 → 原子落盘」满足「每文件至多一次写回」（合同）。不在 `CliSession` 写回，避免跨文件状态与多文件顺序耦合。

### D4 原子写机制 — 同目录 temp file + `Files.move(ATOMIC_MOVE)`，失败不损原文件

**选型**（`OverrideWriter.writeAtomically`）：

1. 在**目标文件所在目录**创建 temp 文件（`Files.createTempFile(dir, prefix, suffix)`）——同目录保证同文件系统，`rename(2)` 可原子。
2. 写入新文本（UTF-8）；`flush`/`fsync` 视需要。
3. `Files.move(temp, target, StandardCopyOption.ATOMIC_MOVE)`。
4. 捕获 `AtomicMoveNotSupportedException`（部分 FS / 跨 FS）时回退 `Files.move(temp, target, REPLACE_EXISTING)`；同目录场景下 Linux 始终走 ATOMIC。
5. 任意异常 → `try-finally` 删除 temp（若存在）并向上抛 `IOException`；**原文件从未被打开写**，故完整无损（P1-4/P1-7）。

**只读 FS / 权限不足**：temp 创建或 move 抛 `IOException` → `FileRunner` 转 `hardFailure`（D3 第 3c 步）；原文件可被原样解析。

**理由**：temp+rename 是 POSIX 原子替换的标准手段，落盘前原文件字节不动；同目录避免跨 FS 非原子；失败可观察为硬错误且不损坏原文件，恰好满足 P1-4/P1-7。

**备选（否决）**：直接 `Files.writeString(target, ...)`（非原子，中断残留半截文件，违 P1-7）；`FileLock`（跨进程语义复杂、非必要）。

### D5 CLI 接线 — 仿 `--halt`，精确无值布尔

**选型**：与 `--halt` 完全同构：
- `CliArgumentParser`：`switch` 新增 `case "--override" -> override = true;`（重复设置等价单次，非 usage 错误）。
- `ParsedArguments`：新增 `boolean override`。
- `RuntimeConfigResolver`：`parsed.override()` 直传 `CliOptions`（**不**从 `GGTEST_*` / `.env` 推断；CLI-only）。
- `CliOptions`：新增 `boolean override`；`toString` 末尾补 `override=...`，脱敏风格不变（无密钥）。

**短形式 / 前缀**：不引入任何 `startsWith` 匹配；`-override`、`--over` 等落入既有 `default -> throw new UsageException("unknown option: " + arg)`，由 `Main` 映射为退出码 `2` 且不连库、不写文件（P0-7）。

**理由**：`--halt` 已是冻结先例（spec 调研发现 #5），复用其解析路径与测试模式，零新机制、最低风险。

### D6 报告 / 退出码 — `[OVERRIDDEN]` tag + 条件 `overridden=N`；退出码优先级不变

**选型**：
- `FileBucket` 新增 `OVERRIDDEN`；`FileOutcome.overridden()`。
- `ReportStyle.overriddenTag()` → `[OVERRIDDEN]`（建议 **CYAN**，与 `SKIPPED` 的 YELLOW 区分；最终色由 Developer 定，须与 PASSED/FAILED/SKIPPED 视觉可辨）。
- `CliSession`：
  - 状态行 `switch` 新增 `case OVERRIDDEN`：打印 `overriddenTag()` + 计时，**不计入 `totalFailed`**，计入 `totalOverridden`，**不打印失败细节块**（overridden 非失败）；
  - `TOTAL` 仅当 `options.override()` 为真时追加 `overridden=N`（N = 跨文件 `overriddenCount()` 之和）；**默认关闭时不追加**，保持 `TOTAL: passed=X failed=Y skipped=Z` 字节级不变（保护 P0-1 与既有冻结样本 / 既有断言）。
- 退出码：逻辑与顺序**不变**——`hardError → 2`；否则 `totalFailed > 0 → 1`；否则 `0`。`OVERRIDDEN` 不抬高失败计数（override-only 无剩余 FAILED → `0`）。

**理由**：文件级 tag + 跨文件记录计数与既有 PASSED/FAILED/SKIPPED 架构一致；`overridden=N` 条件化以严格守「默认关闭 = 现状字节级一致」。

### D7 statement error header 重写与统一抽象 — 区间+新文本，按记录类型取区间

**统一抽象**：一条 override = `(record, newText)`；`OverrideWriter.rewrite` 按 `record` 类型取区间（D1）并落到行表编辑：

- **query**：替换 `lines[bodyStart-1 .. expectedBodyEndLine-1]`（0-based 闭区间）为 actualView 各行（用文件既有 EOL 连接）；`----` 头（`expectedHeaderLine`）与块后空行分隔**原样保留**。
- **statement error**：取 `lines[location.startLine-1]`，保留前缀 `header.substring(0, errorMsgStartColumn)`（即 `statement error` + 原始分隔），后缀替换为实际 error summary；极性 `error` 与 SQL 正文不动。

**多记录同文件**：所有 override 在同一行表上**按行号倒序**应用（query 区间替换 / statement 单行编辑），避免索引漂移；非 override 区域（注释、空白、SQL、其它记录、header）逐字节保留。

**换行与编码**：检测文件既有 EOL（含 `\r\n` → 用 `\r\n`，否则 `\n`）；以检测到的 EOL 切分 / 重连，保留文件尾是否带换行；统一 UTF-8。混合 EOL（罕见）会被归一为检测值——合同为「不强制改换行符」，可接受；若需更强保证，可在实现时按行保留 CR（实现选项，不改变合同）。

**理由**：query 块重写与 statement 行内重写统一为「区间替换」，复用同一行表管线；保留极性与前缀符合 Spec 文件改写合同。

### D8 不改 parser / 比较 / 规范化语义

override 仅新增「判定（D2）+ 写回（D3/D4/D7）」；actualView 经 `ResultComparer.renderActual`（按当前 `hash-threshold` 产生明文或 `N values hashing to <md5>`）原样透传，error summary 经 `StatementResult.errorSummary()` 原样透传。**不**修改 parser 的记录识别、`ResultComparer` 的比较 / 哈希、`ResultSorter` 的排序 / 规范化。因此「重跑幂等」（P0-3）由「写回内容 = 本次 actualView = 下次 expected」自然成立。

## 数据流（override 开启）

```
argv --override
 → CliArgumentParser(--override) → ParsedArguments.override
 → RuntimeConfigResolver → CliOptions.override
 → CliSession → FileRunner.run(parser, file, display)
   → parser.parse(file)  [records 携带 expected 区间，D1]
   → SqlLogicTestRunner(overrideEnabled=true).run(records)
       [in-scope mismatch → RecordResult.overridden(record, actualView/errorSummary)，D2]
   → 收集 OVERRIDDEN → OverrideWriter.rewrite(原文, overrides) [D7]
   → OverrideWriter.writeAtomically(path, newText) [D4]  （失败→hardFailure，原文件不动）
   → FileOutcome(OVERRIDDEN | FAILED | PASSED | SKIPPED) [D3 桶判定]
 → CliSession：状态行 [OVERRIDDEN] tag；累加 totalOverridden；条件 TOTAL overridden=N；退出码优先级不变 [D6]
```

## 边界与不变性（实现须遵守）

- 默认关闭（无 `--override`）：不产 `OVERRIDDEN`、不写文件、TOTAL 无 `overridden=` 段、退出码语义不变——与现状字节级一致。
- 无 in-scope mismatch：不打开写、不改 mtime（P0-4）。
- 文件其余字节：注释 / 空白 / SQL / 其它记录 / header / 换行风格 / 文件尾换行 逐字节保留（P0-5）。
- 原子性：写回失败不留半截 / 空文件，原文件可原样解析（P1-4/P1-7）。
- 凭据：override 载荷仅为 actualView / error summary；不引入新脱敏面，沿用 `CredentialRedaction`。
- 不改 parser / 比较 / 规范化语义（D8）。

## 留待实现（不属合同，Developer 可定）

- `OverrideWriter` 的具体行表切分 / 重连细节（含混合 EOL 处理强度）。
- `overriddenTag` 最终配色（须与既有四态可辨）。
- runner 构造器参数顺序（`overrideEnabled` 与 `haltOnFirstFailure` 并列，新增重载保留旧签名兼容）。
- 次构造器字段缺省值（区间缺省 = 「不可 override」哨兵，如 query `0/0`、statement `-1`）。
