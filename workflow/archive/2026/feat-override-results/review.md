
# Review: feat-override-results
- 审阅日期: 2026-08-10 / 审阅人: Reviewer (subagent)
- 路径等级: full / 依据: spec.md / design.md / plan.md

## 结论: Approve

## 验证结果
### 定向测试 (mvn test -Dtest=...)
BUILD SUCCESS | Tests run: 203, Failures: 0, Errors: 0, Skipped: 1 (PG gate)
### 全量测试 (mvn test)
BUILD SUCCESS | Tests run: 321, Failures: 0, Errors: 0, Skipped: 16 (PG/Corpus gate)

## 审阅明细

### 1. 行为合同符合性 (spec.md §范围与可见行为、§合同)
**结论: 通过。** 所有 P0/P1 验收项均有实现覆盖。

| 验收项 | 实现位置 | 证据 |
|---|---|---|
| P0-1 默认关闭 | `FileRunner.java:140` (aborted 检查后, override 才进入写回), `ReportWriter.java:59-65` (showOverridden 条件), `SqlLogicTestRunner.java:253` (overrideEnabled 守卫) | `MainOrchestrationTest.java:268-276` `defaultOff_totalHasNoOverriddenSegment` |
| P0-2 query mismatch 被重写 | `SqlLogicTestRunner.java:237-255` (in-scope→OVERRIDDEN), `FileRunner.java:141-147` (收集→写回), `OverrideWriter.java:111-124` (区间替换) | `MainOrchestrationTest.java:256-265`, `FileRunnerTest.java:167-188` |
| P0-3 重跑幂等 | OVH 内容=actualView，下次解析即为 expected 文本，自然幂等 | `MainOrchestrationTest.java:318-336` `overrideThenRerun_isIdempotent` |
| P0-4 无 mismatch 不写 | `FileRunner.java:142` (overrides.isEmpty()→跳过写回) | `MainOrchestrationTest.java:339-358`, `FileRunnerTest.java:214-236` |
| P0-5 文件其余字节不变 | `OverrideWriter.java:112-124` (仅替换 body 区间), `SqlLogicTestParser.java:230-233` (parser 精确区间) | `MainOrchestrationTest.java:361-388` `overrideEnabled_restOfFileByteIdenticalExceptBody` |
| P0-6 退出码优先级 | `CliSession.java:107-112` (hard→2, failed>0→1, else 0), `FileRunner.java:150-159` (桶判定) | `MainOrchestrationTest.java:279-297` `overrideEnabled_mixedOverrideAndScopeOutFailed_exitsOne` |
| P0-7 选项解析 | `CliArgumentParser.java:62` (精确 `--override`), `CliArgumentParser.java:63` (default→UsageException) | `CliArgumentParserTest.java:98-150`, `MainOrchestrationTest.java:300-315` |
| P1-1 statement error 消息重写 | `SqlLogicTestRunner.java:198-203` (overrideEnabled→OVERRIDDEN), `OverrideWriter.java:126-138` (列号前缀保留) | `MainOrchestrationTest.java:391-404` |
| P1-2 scope 外不覆盖 | `SqlLogicTestRunner.java:253-255` (仅 resultMismatch+!labelConflict→OVERRIDDEN), `SqlLogicTestRunner.java:217-218` (exec fail→FAILED early-return) | `SqlLogicTestRunnerTest.java:552-598` (4个用例), `MainOrchestrationTest.java:406-443` |
| P1-3 execute-only 不生成 | `SqlLogicTestRunner.java:237` (hasExpectedResults 条件) | `MainOrchestrationTest.java:446-460`, `SqlLogicTestRunnerTest.java:610-618` |
| P1-4 只读 FS | `OverrideWriter.java:77-100` (temp+ATOMIC_MOVE, 失败删temp), `FileRunner.java:179-193` (IOException→hardFailure) | `OverrideWriterTest.java:156-174`, `FileRunnerTest.java:269-301`, `MainOrchestrationTest.java:495-511` |
| P1-5 --override + --halt | `SqlLogicTestRunner.java:170-174` (haltOnFirstFailure 仅按 FAILED), `CliSession.java:98-100` (bucket==FAILED 停跑) | `SqlLogicTestRunnerTest.java:644-658` `overrideEnabled_doesNotTriggerHalt`, `MainOrchestrationTest.java:463-492` |
| P1-6 致命中止不写 | `FileRunner.java:129-138` (aborted→hardFailure 在 override 代码前) | **代码审查确认正确** (见问题 #1) |
| P1-7 原子性 | `OverrideWriter.java:82-99` (temp file→write→ATOMIC_MOVE, finally 删 temp) | `OverrideWriterTest.java:156-174` |
| P1-8 文档 | `README.md:46,59`, `README.zh-CN.md:34,48` | grep 确认两文件均含 `--override` 及语义 |

### 2. 关键不变性

| 不变性 | 验证结论 | 证据 |
|---|---|---|
| 默认关闭=现状字节级一致 | 通过 | `FileRunner.java:140` (`options.override()` 守卫); `ReportWriter.java:54-65` (条件 showOverridden); 默认 TOTAL 行无 `overridden=`; 无 `--override` 时不产 OVERRIDDEN |
| 无 in-scope mismatch→不改 mtime | 通过 | `FileRunner.java:142` (overrides.isEmpty()→跳过); `FileRunnerTest.java:214-236`, `MainOrchestrationTest.java:339-358` (mtime 断言) |
| 文件其余字节逐字节保留 | 通过 | `OverrideWriter.java:111-138` (仅替换 interval 内); `MainOrchestrationTest.java:361-388` |
| 原子写不损原文件 | 通过 | `OverrideWriter.java:82-99` (temp+ATOMIC_MOVE, try-finally 删 temp); `OverrideWriterTest.java:156-174` |
| 不改 parser/比较/规范化 | 通过 | parser 仅新增 interval 字段填充 (输出不额外影响解析); runner 复用 `comparison.actualView()`, 未修改 `ResultComparer`/`ResultSorter` |
| in-scope 范围 仅 query 纯 result mismatch + statement error 消息失配 | 通过 | `SqlLogicTestRunner.java:237-256` (仅 resultMismatch+!labelConflict), `SqlLogicTestRunner.java:200-203` (仅实际失败+子串失配) |

### 3. 区间定位 (Parser, OverrideWriter)
**结论: 通过。**

- `expectedHeaderLine` (1-based): `SqlLogicTestParser.java:230` 记录 `----` 头行号 → `SqlLogicTestParserTest.java:673-679`, `OverrideWriter.java:112`: `from=headerLine` (恰好等于 0-based body start)
- `expectedBodyEndLine` (1-based): `SqlLogicTestParser.java:247`=`expectedHeaderLine + expected.size()` → `SqlLogicTestParserTest.java:674,679,704-706`
- `errorMsgStartColumn`: `SqlLogicTestParser.java:155-163` 在 de-CR header 行上计算 → `SqlLogicTestParserTest.java:720,734`
- 区间哨兵: execute-only→0/0 (`parser:692-693`), statement ok→-1 (`parser:761`)

### 4. 多 override 同文件
**结论: 通过。** `OverrideWriter.java:57-61`: 按 `location().startLine()` **倒序**排序后逐条应用，避免行号漂移。已验证 0-based/1-based 偏移一致，多 query + statement 混合正确（`OverrideWriterTest.java:72-91`）。

### 5. --halt 交互
**结论: 通过。** `OVERRIDDEN != FAILED`, 故 runner 的 `haltOnFirstFailure`（`SqlLogicTestRunner.java:170-174`）与 `CliSession` 的 `bucket==FAILED` 停跑（`CliSession.java:98-100`）均不触发于 in-scope override。`SqlLogicTestRunnerTest.java:644-658` 验证 OVERRIDDEN 不触发 halt。同文件内 scope 外 FAILED 仍触发 halt，文件级写回在 runner.run 返回后、跨文件停跑前完成（`FileRunner.java:118-148`），满足"停跑前已override的记录仍写回"。

### 6. Aborted 不写
**结论: 通过（代码逻辑正确，测试覆盖有轻微缺口）。** `FileRunner.java:129-138` 在 `options.override()` 检查之前先行返回 `hardFailure`，任何已计算的 OVERRIDDEN 不会进入 write-back。Runner 层 aborted 行为在 `SqlLogicTestRunnerTest.java:362-376` 已验证。**缺**一个 `--override` + `FatalDatabaseException` 的集成端对端测试（见问题 #1）。

### 7. CLI 解析
**结论: 通过。**
- 精确 `--override` → `CliArgumentParser.java:62` ✓
- `-override` → `CliArgumentParser.java:63` default→`UsageException("unknown option: -override")` → 退出码2 ✓
- `--over` → 同上 ✓
- 重复 `--override --override` ≡ 单次 ✓
- 不读 env/`.env` → `RuntimeConfigResolver.java:99` CLI-only 透传 ✓
- `CliOptions.toString()` 含 `override=...` (`RuntimeConfigResolverTest.java:312`) ✓

### 8. 测试有效性
**结论: 通过。** 全部使用真实断言（无恒真）、字节级比较、文件内容/mtime/退出码/stderr 多维度验证。

| 测试类 | 新增/总计 | 覆盖 |
|---|---|---|
| SqlLogicTestParserTest | +7 / 45 | 区间字段、多记录、execute-only、空块、statement error 列号 |
| SqlLogicTestRunnerTest | +10 / 44 | 所有 in-scope/scope-out 组合、OVERRIDDEN 不触发 halt、默认关闭 |
| OverrideWriterTest | +14 / 14 | CRLF/LF/无尾换行/空块/多记录/只读FS/原子性/UTF-8 |
| FileRunnerTest | +5 / 11 | override 开/关/全 PASSED/scope 外+in-scope 混合/写回失败 |
| CliArgumentParserTest | +6 / 17 | 默认/精确/重复/短形式/前缀/组合 |
| RuntimeConfigResolverTest | +4 / 30 | 默认/CLI 透传/不读 env/.env/toString |
| MainOrchestrationTest | +14 / 28 | P0-2~P0-7, P1-1~P1-5 集成面 |

### 9. 安全
**结论: 通过。**
- `OverrideWriter.writeAtomically` 仅写 `FileRunner` 传入的目标 `Path`（来源: `TestFileCollector.collect` → positional CLI inputs），无用户可控路径拼接。
- temp 文件创建在目标文件所在目录 (`Files.createTempFile(dir, ".ggtest-override-", ".tmp")`)，prefix 固定，无路径遍历风险。
- override payload 仅为 `actualView`（含 hash 形式）及 `errorSummary`，无凭据、无用户自定义文本注入。
- `CredentialRedaction.redactMessage` 已应用于写回失败详情 (`FileRunner.java:181`)。
- 无新增脱敏面。

### 10. 文档
**结论: 通过。** `README.md:46,59` 与 `README.zh-CN.md:34,48`：synopsis 行含 `[--override]`, 选项表含完整行 → 语义与 Spec §CLI 接口一致。

## 问题清单

| # | 严重程度 | 描述 | 位置 | 建议 |
|---|---|---|---|---|
| 1 | 低 | P1-6 (aborted+override 不写文件) 缺集成端对端测试——仅依靠 `FileRunner.java:129-138` 的 abort 短路逻辑，无「开启 override→触发 FatalDatabaseException→文件不变+退出码2」的测试。Runner 层 aborted 行为已验证 | `FileRunner.java:129-138` | 可在 QA 阶段追加一个 sqlite fixture 触发 `FatalDatabaseException`（如通过 `FakeDatabaseExecutor.fatalOn` 注入）验证 override 下 abort 不写，或作为已知低风险缺口记录到 qa-report |

## 备注

1. **OverrideWriter query 区间计算**：`headerLine` (1-based) 恰好等于 0-based body start 索引（因 body start = headerLine + 1 (1-based) = headerLine (0-based)），数学上自然成立，但代码中缺少注释说明。不影响正确性。
2. **OVERIIDENT tag 配色**：为 **CYAN**（`ReportStyle.java:38`），与 PASSED(GREEN)、FAILED(RED)、SKIPPED(YELLOW) 四态可辨。✓
3. **次构造器兼容**：`QueryRecord` 和 `StatementRecord` 保留旧签名次构造器（区间哨兵 0/0, -1），`CliOptions` 保留旧签名次构造器（`override=false`），`SqlLogicTestRunner` 保留旧重载链。所有既有调用点零改动编译通过。✓
4. **混合 EOL**：`OverrideWriter.rewrite` 按「全文是否含 `\r\n`」检测 EOL 并归一，Dev 确认符合「不强制改换行符」合同。混合 EOL 罕见——可接受。✓
5. **实现总计新增 59 tests**（基线 262→321），零回归。

### 关键不变性各一句核验
- **默认关闭=现状字节一致**：`options.override()` 守卫所有 override 路径，无 `--override` 时不产 OVERRIDDEN、不写文件、TOTAL 无 `overridden=`、退出码不变 → 通过。
- **无 in-scope mismatch→不改 mtime**：`overrides.isEmpty()` 时跳过写回，全 PASSED 文件 mtime 断言 → 通过。
- **文件其余字节逐保留**：行表倒序替换，仅触碰 expected 区间 → 通过。
- **原子写不损原文件**：temp+ATOMIC_MOVE+finally 删 temp，原文件从未被打开写 → 通过。
- **不改 parser/比较/规范化**：未触碰 `ResultComparer`/`ResultSorter`/parse 识别逻辑 → 通过。
- **in-scope 范围**：仅 query 纯 result mismatch + statement error 消息失配 → 通过。

### 阻塞项
无 blocking 项。

### 是否满足进 QA 条件
是。唯一缺口 P1-6 为低严重度（代码逻辑正确但缺集成测试），可在 QA 阶段补验或记录为已知低风险缺口。
