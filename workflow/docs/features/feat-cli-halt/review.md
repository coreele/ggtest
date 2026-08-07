# Review: feat-cli-halt

## 轮次

| 轮次 | 范围 | 版本 | 结论 |
|---|---|---|---|
| 1 | 首审实现（cli/runner 代码 + 测试 + README + fixture） | `feat-cli-halt` 分支 `13a7713` 及 docs 提交 `dd5266b` / `3bbf40d`（基线 `main`） | **Approve** |

## 审阅范围

- 工作项：`feat-cli-halt`（未拆分；sub-feature-id = feature-id；standard；Review required）
- 源分支：`feat-cli-halt` → 目标 `main`；实现已在该分支提交
- 依据：`spec.md` / `plan.md`（均冻结）、`dev-notes.md`、`workflow/docs/manager/feat-cli-halt.md`；`documentation.md` / `quality.md` / `security.md` / `git.md`
- Design：N/A（门禁 skipped）
- 实现版本：4 commits（`7dce363` plan/status → `13a7713` feat 实现 → `dd5266b` README → `3bbf40d` dev-notes；基线 `main`）
- 改动范围：主代码 7（cli 6 + `SqlLogicTestRunner`）/ 测试 6 + fixture `corpus-halt.test` / README 2 / 切片文档 3；逐文件 diff 见各核对节
- 排除：`workflow/docs/manager/*`、`STATUS.md` 属 Manager，不计入实现验收

## 结论

**Approve**

合同 P0-1…P0-6 / P1-1…P1-2 全部满足；默认关闭语义零回归；`FileRunResult.halted` 既有语义未重载；选项解析精确拒绝前缀/短形式；凭据脱敏沿用既有机制无新增泄露面；Git 合规。满足 standard 路径进 QA 的 Review 门禁。

## 必修项

| ID | 位置 | 问题 | 状态 |
|---|---|---|---|
| — | — | 无阻塞项 | — |

> 无开放阻塞项。下表发现项均为非阻塞建议。

## 实现正确性

独立基于 diff 与定点复跑作出（不复述 Developer 自述）。

| 合同点（Spec） | 判断 | 证据 |
|---|---|---|
| **P0-1 默认关闭不变** | Pass | `haltOnFirstFailure` 默认 `false`（`SqlLogicTestRunner` 2-arg 构造器委托 3-arg 传 `false`）；`CliSession` break 条件 gated 于 `options.halt()`；既有 `multiFailureLayout…` / `p1_3_hardError…` / `parseErrorExitsTwoContinuesOtherFiles` 路径未改 + 新增 `defaultOffReportsAllFailuresAndRunsLaterFiles`（whyCount=3，第二文件仍跑，exit 1） |
| **P0-2 单文件首错即停** | Pass | run 循环首条非 fatal `FAILED` 后置 `haltedOnFirstFailure=true`；其后 assertable 标 `SKIPPED_AFTER_FAILURE_HALT`，**不执行 SQL**（`executedSql` 断言仅含 before+BOOM）、**不以 FAILED 出现**；`haltStopsAfterFirstFailureInOneFile` 仅 1 个 `[WHY]`/`at` 块；`haltOnFirstFailureSkipsRemainingAssertables…` 剩余 SKIPPED |
| **P0-3 多文件全局停** | Pass | `CliSession.execute:93` `if (options.halt() && outcome.bucket()==FAILED) break;`（break 在状态行/明细/TOTAL 计数**之后**，故失败文件计入、后续文件不启动）；`haltStopsAfterFirstFailingFileAndDoesNotStartLaterFiles`：`pass.test` 不在 stdout、`TOTAL.passed=0`、exit 1 |
| **P0-4 硬错误 → 2** | Pass | `bad-parse.test`（解析硬错误）映射 FAILED 桶 → break；`haltWithHardErrorExitsTwoAndDoesNotStartLaterFiles`：`pass.test` 不启动、exit 2、`[FAILED]`+`[WHY]` 既有格式 |
| **P0-5 选项解析** | Pass | `case "--halt" -> halt = true`（精确）；`-halt`/`--hal` 落 `default -> throw UsageException("unknown option: " + arg)`；`singleDashHalt…`/`haltPrefixLongOption…` 解析层断言抛 `UsageException`。退出码 2 + 不连库经既有 `UsageException→2` 映射（`missingUrlExitsTwoWithoutRunning` 同路径）成立（见发现项 F1） |
| **P0-6 与语料 halt 区分** | Pass | `halted`（语料）与 `haltedOnFirstFailure`（CLI）为两个独立局部标志；`FileRunResult(results, halted, aborted, abortReason):159` **未传** `haltedOnFirstFailure` → `halted` 语义未重载；`haltOnFirstFailureDoesNotStopOnCorpusHaltRecord`：语料 halt 仍 set `halted`、0 failures；`corpusHaltRecordDoesNotTriggerCliHalt`：第二文件仍执行、exit 0 |
| **P1-2 重复 ≡ 单次** | Pass | `halt = true` 幂等；`repeatedHaltFlagIsEquivalentToSingle`（3×`--halt`）非 usage 错误 |
| 退出码优先级不变 | Pass | `CliSession:102-108` 逻辑零改动（`hardError→2; totalFailed>0→1; else 0`） |
| `FileRunResult.halted` 未重载 | Pass | 见 P0-6；唯一赋值点 `HaltRecord` 分支 `:131` |
| 越界检查 | Pass | `git diff --name-only main..HEAD` 仅 cli/runner + 测试 + README + 切片文档；normalize/parser 零改动 |

### 复跑（Reviewer 独立）

`mvn test "-Dtest=CliArgumentParserTest,RuntimeConfigResolverTest,FileRunnerTest,MainOrchestrationTest,SqlLogicTestRunnerTest"` → **Tests run: 86, Failures: 0, Errors: 0, Skipped: 1**（FileRunnerTest PG 门控），**BUILD SUCCESS**。与 dev-notes 计数一致（12+26+6+14+28=86）。

## 测试有效性

| 项 | 判断 | 证据 |
|---|---|---|
| P0-1…P0-6 / P1-1 / P1-2 覆盖 | Pass | 见上「实现正确性」逐项映射；每条验收均有定向用例 |
| 可因错误实现失败（非恒真） | Pass | runner 用例断言 `executedSql`（首错后不连库会被检出）；FileRunner/Main 用例断言 `[WHY]`/`at` 计数与 `pass.test` 是否出现；parser 用例断言 `halt()` 真值与 `UsageException`；`result.halted()` 显式断言语料语义未重载 |
| L2 最低验证层 | Pass | 单元（parser/resolver/runner）+ 内存 SQLite 集成（FileRunner/MainOrchestration）；PG 硬错误路径以 `bad-parse.test`（解析硬错误）等价代表（Plan 已声明） |
| 不变性回归 | Pass | 既有 `multiFailureLayout…` / `p1_3_hardError…` / `parseErrorExitsTwoContinuesOtherFiles` 未改路径 + 新增 `defaultOffReportsAllFailuresAndRunsLaterFiles` 固化 P0-1 |

## 文档影响核对

| Plan 声明 | 实现是否一致 | 备注 |
|---|---|---|
| 开发文档：Javadoc（CliArgumentParser / CliOptions / SqlLogicTestRunner / CliSession） | 是 | 四处 Javadoc 均补充 `--halt` 行为/例外（diff 可见）；`ParsedArguments.halt`/`CliOptions.halt` 字段 Javadoc 完整 |
| 用户文档：`README.md` / `README.zh-CN.md` synopsis + 选项表 | 是 | 两份 synopsis 均 + `[--halt]`；选项表新增 `--halt` 行，简述对齐 *Stop when first error is seen*（默认关、首错即停、退出码优先级不变、语料 halt 区分）—— 与 Spec §API 一致 |
| 运维文档：N/A | 是 | `--halt` 为运行时开关；退出码集合与优先级不变；部署/监控/备份/排障无影响 |

## 安全影响核对

| 检查项 | 结果 | 备注 |
|---|---|---|
| 敏感信息 | Pass | `CliOptions.toString()` 仅 + `halt=…`（无密钥）；既有 `password`→`***` 脱敏未改；`RuntimeConfigResolverTest` 断言 `halt=false`/`halt=true` 出现且密码不出现；报告路径无凭据新增面 |
| 认证与授权 | N/A | 无认证模型变更 |
| 输入与文件操作 | Pass | 仅新增布尔旗标解析与控制流；无新路径遍历/外联/反序列化面；`--halt` 不读 env/.env（`haltIsNotInferredFromProcessEnvOrDotEnv` 断言 `GGTEST_HALT=true` 被忽略） |
| 依赖变更 | Pass | 无 `pom.xml` / 依赖改动 |
| 敏感数据 | N/A | 无 PII 等 |

检查范围与变更实际影响面（cli/runner 控制流 + 选项解析 + 脱敏字段）一致。无未解决安全问题。

## 特别核验：NormalizeAcceptanceTest（pre-existing）

**判定：与本工作项无关（pre-existing，非回归）。**

| 维度 | 结论 |
|---|---|
| 改动范围 | `git diff --name-only main..HEAD` 与 `git log main..HEAD -- …/normalize` 均**空** → normalize 包（代码 + 测试 + fixture）零改动 |
| 失败复现 | Reviewer 独立 `mvn test "-Dtest=NormalizeAcceptanceTest"` → `p0_2_hashMatchesSelect1CorpusExcerpt` ERROR `IllegalArgument fixture missing values section` @ `extractValuesSection:120`，与 dev-notes 描述逐字一致（Windows CRLF 致 `indexOf("values:\n")` 失配） |
| dev-notes 证据链 | 成立：`git stash` 复现 + 零 diff 双证；属工程基线（`.gitattributes` 缺失），建议作独立 chore 处理，不在本工作项实施 |
| 风险 | 不影响 `--halt` 合同与回归；仅 Windows CRLF 检出环境影响该 normalize 用例 |

## Git 合规

- 分支 `feat-cli-halt`（非 `main`），命名含 feature-id；未在受保护分支实施 ✓
- 4 commits 原子（plan/status → feat → README → dev-notes），Conventional Commits 格式（`feat(cli):` / `docs(halt):`）✓
- `git diff --name-only` 无 `.env` / 凭据 / `target/` 构建产物 / IDE 临时文件 ✓
- 本报告未执行 `git add`/`commit`/`push`（留在工作区，交 Manager 择机提交）✓

## 发现项

| 级别 | 位置 | 说明 | 建议/处置 |
|---|---|---|---|
| 非阻塞（建议） | `MainOrchestrationTest`（P0-5） | Plan 验收提及「Main 层用例」验证 `-halt`/`--hal` → exit 2 + 不连库；当前仅 parser 层断言 `UsageException`，Main 层 exit-2 由既有 `missingUrlExitsTwoWithoutRunning`（同 `UsageException→2` 路径）等价覆盖，合同已满足 | 可选：补一条 `singleDashHaltExitsTwoWithoutRunning` 显式固化 P0-5 Main 层；不阻 QA |
| 非阻塞（记录） | `NormalizeAcceptanceTest` | Windows CRLF 既有失败 | 独立 chore 工作项（`.gitattributes` 强制测试资源 LF）；超出本工作项范围 |

无阻塞项；无未解决安全问题。

## 后续动作与复审范围

1. **Manager**：本结论满足 standard 路径 Review 门禁，可调度 **QA**（依 Spec P0/P1 + Plan L2 独立验收，产出 `qa-report.md`）。
2. **复审范围**：N/A（无阻塞项）。若 QA Fail 或 Developer 采纳 F1 补测，仅复审相关切片。
3. F1/F2 均为非阻塞建议，不构成返工。
