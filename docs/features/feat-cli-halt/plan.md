# Plan: feat-cli-halt

## 元信息

- 工作项标识: feat-cli-halt
- 依据 Spec: docs/features/feat-cli-halt/spec.md
- 依据 Design: N/A（Design 门禁为 skipped；沿用现有 CLI/runner 边界，无新模块选型）
- 路径等级: standard
- Review 门禁: required（standard）
- 最低验证层: L2（单元测试 + 内存 SQLite 集成测试）
  - 理由：`--halt` 是纯进程内控制流，不引入新外部系统。断言失败停跑、多文件全局停、解析硬错误停跑均可由 `:memory:` SQLite 与既有 `bad-parse.test` 类 fixture 覆盖；Postgres teardown 硬错误路径已被 `bad-parse`（解析失败）等价代表，无需 PG 环境。选项拒绝（`-halt`/`--hal` → usage）在 `CliArgumentParser` 单元层即可验证，且经 `Main.run` 验证不连库。
- 验证命令:
  - 全量：`mvn -q test`
  - 定向（开发迭代）：`mvn -q test -Dtest=CliArgumentParserTest,MainOrchestrationTest,CliReportAcceptanceTest,FileRunnerTest`
  - 构建打包（确认无回归）：`mvn -q clean package`

## 适用工程规范

- [文档工程](../../standards/documentation.md)
- [Git 协作](../../standards/git.md)（仅 Git 工作区）
- [质量与验证](../../standards/quality.md)
- [安全](../../standards/security.md)

## 目标摘要

依据已冻结 Spec `docs/features/feat-cli-halt/spec.md`，为 `ggtest` CLI 增加与官方 `sqllogictest --halt` 对齐的布尔选项 **`--halt`**（默认关闭）。未传时现有「文件内继续、多文件跑完、退出码 0/1/2」语义**完全不变**。开启后：本进程**首次错误**（断言失败或硬错误）即停——文件内后续记录不执行且不得报假失败；尚未开始的后续文件不得打开/解析/执行，不得出现在状态行或 `TOTAL`。退出码优先级不变：曾有 hard error → `2`；否则有失败文件 → `1`；否则 `0`。选项解析沿用现有精确长选项机制：重复 `--halt` ≡ 单次（非 usage 错误）；`-halt`/`--hal` 等前缀或短形式 → usage，退出码 `2`，不连库。语料 `halt` 记录语义不变（仅中止当前文件后续并 skipped，**非错误**，不触发 CLI `--halt` 停跑）。凭据永不入报告。

## 任务拆解

> 每项「完成条件」即为可由 Reviewer/QA 据以核验的客观证据。Spec 合同为权威，下列实现路径是实现建议，Developer 可在不变更合同的前提下调整内部结构。

1. **T1 选项解析：精确 `--halt`、重复等价、拒绝前缀/短形式**
   - 在 `CliArgumentParser.parse` 的 `switch` 中新增 `case "--halt"` 分支：设置布尔标志（重复设置等价单次，不抛 usage）。
   - `ParsedArguments` 增加 `boolean halt`（或 `Optional<Boolean>`，缺席即未传）字段并纳入 `record`。
   - 不引入任何 `startsWith` 前缀匹配；`-halt`、`--hal` 等落入既有 `default -> throw new UsageException("unknown option: " + arg)`。
   - 完成条件：`CliArgumentParserTest` 新增用例——`--halt` 解析为开启；`--halt --halt` 同单次（不抛异常）；`-halt`、`--hal` 各自抛 `UsageException` 且消息含未知选项语义。

2. **T2 运行时配置透传**
   - `CliOptions` 新增 `boolean halt` 字段；`toString()` 沿用现有凭据脱敏风格补充该字段（无密钥）。
   - `RuntimeConfigResolver.resolve` 将 `parsed.halt()` 透传进 `CliOptions`。Spec 未规定 `--halt` 的 env/`.env` 来源，**仅 CLI 直传**，不得从 `GGTEST_*` 推断。
   - 完成条件：`RuntimeConfigResolverTest` 新增/扩展用例确认 `--halt` 进入 `CliOptions.halt()`；未传时为 `false`；`CliOptions.toString()` 不含 `password` 明文（沿用既有断言）。

3. **T3 文件内首错即停（runner 层）**
   - `SqlLogicTestRunner` 增加 `haltOnFirstFailure` 控制（构造参数或 `run` 参数，由 Developer 选定；实例每文件新建，线程安全语义不变）。
   - 当 `haltOnFirstFailure` 为 true 且某条 assertable 记录判定为 `RecordOutcome.FAILED`（非 fatal）时：记录该失败，随后将剩余 assertable 记录标记为 `SKIPPED`，原因须表明因 `--halt`/先前失败未执行（新增专用原因常量，与既有 `SKIPPED_AFTER_HALT` 区分）。后续记录**不得执行 SQL**、**不得以 FAILED 出现**。
   - `FatalDatabaseException`（硬错误 abort）路径不变：仍 `break` 并计 `aborted`。
   - **不得**重载 `FileRunResult.halted` 的既有语义（该字段专指语料 `halt` 记录触发）。如需区分 CLI `--halt` 停跑，可由 Developer 决定是否新增字段；合同层面只要求未执行记录不出现在失败明细。
   - `FileRunner.runWithExecutor` 构造 runner 时传入 `options.halt()`。
   - 完成条件：`FileRunnerTest` 新增用例——多失败 fixture 在 `halt=true` 下只产出 1 个 `[WHY]`/`at` 失败块；其余记录不出现 FAILED 详情；`SqlLogicTestRunnerTest` 新增用例直接覆盖首错后剩余记录为 SKIPPED。

4. **T4 跨文件全局停（CliSession 层）**
   - `CliSession.execute` 在每个文件 `fileRunner.run` 返回后，若 `options.halt()` 且 `outcome.bucket() == FileBucket.FAILED`（涵盖断言失败与硬错误两类，二者均映射到 `FAILED` 桶），则 `break` 跳出文件循环：后续文件不被打开/解析/执行，不打印状态行，不计入 `TOTAL`。
   - 退出码逻辑**不变**：`hardError` 标志仍由当前文件累积；循环提前结束后既有的 `if (hardError) return 2; if (totalFailed>0) return 1; return 0;` 自然满足合同。
   - 完成条件：`MainOrchestrationTest`（或新增 `HaltAcceptanceTest`）用例——第一文件断言失败 + 第二文件，加 `--halt`：第二文件路径不出现在 stdout，`TOTAL passed/failed/skipped` 仅计已处理文件，退出码 `1`。

5. **T5 默认关闭不变性回归**
   - 不传 `--halt` 时所有现有行为保持。既有 `CliReportAcceptanceTest.multiFailureLayoutUsesBlankSeparatorsAndFlushAt`（单文件多失败全报）、`p1_3_hardErrorCountsFailedAndExitsTwo`（硬错误后仍跑后续文件）、`parseErrorExitsTwoContinuesOtherFiles` 已覆盖核心回归。
   - 完成条件：上述既有用例在不修改被测代码路径的前提下继续通过；新增一条显式断言「不带 `--halt` 跑多失败文件 → 全部失败均报告、退出码 1」以固化 P0-1。

6. **T6 硬错误停跑 → 退出码 2**
   - 复用 `bad-parse.test`（解析硬错误）+ `pass.test`，加 `--halt`。
   - 完成条件：新增用例断言 `pass.test` 不被启动（stdout 不含其路径、不计 TOTAL），退出码 `2`，硬错误以既有格式呈现。

7. **T7 与语料 `halt` 区分**
   - 新增 fixture：成功记录 + 会执行的语料 `halt` + 其后记录；外加第二文件。argv 含 `--halt`。
   - 完成条件：新增用例断言第一文件因记录 `halt` 中止后续（skipped，**非错误**），**不**因此产生非 0；第二文件仍执行；无其他失败则退出码 `0`。

8. **T8 README 选项表更新（用户文档）**
   - `README.md` 选项表新增 `--halt` 行；CLI synopsis 行同步补充 `[--halt]`。简述对齐官方 *Stop when first error is seen*（默认关闭）。
   - `README.zh-CN.md` 选项表与 synopsis 同步更新。
   - 完成条件：两份 README 选项表均含 `--halt`，且语义与 Spec §API/接口一致（默认关、首错停跑）。

## 依赖与顺序

- T1（解析）→ T2（配置透传）：`ParsedArguments.halt` 必须先存在。
- T2 → T3、T4：`CliOptions.halt()` 是 runner 与 session 的输入。
- T3 与 T4 可并行实现（文件内 / 跨文件两层相互独立），但都依赖 T2。
- T5、T6、T7 为验证任务，依赖 T1–T4 实现完成；T5 同时充当不变性回归。
- T8（README）与代码任务无强依赖，可在实现同时更新，Review 前必须完成。
- 建议实施顺序：T1 → T2 → T3 → T4 → T5/T6/T7 → T8。

## 触碰路径

- 选项解析（T1）：`src/main/java/com/ggtest/cli/CliArgumentParser.java`、`src/main/java/com/ggtest/cli/ParsedArguments.java`
- 配置透传（T2）：`src/main/java/com/ggtest/cli/RuntimeConfigResolver.java`、`src/main/java/com/ggtest/cli/CliOptions.java`
- 文件内首错停（T3）：`src/main/java/com/ggtest/cli/FileRunner.java`、`src/main/java/com/ggtest/runner/SqlLogicTestRunner.java`（如 Developer 决定新增区分字段，另含 `src/main/java/com/ggtest/runner/FileRunResult.java`）
- 跨文件全局停（T4）：`src/main/java/com/ggtest/cli/CliSession.java`
- 用户文档（T8）：`README.md`、`README.zh-CN.md`
- 测试（T1–T7）：
  - `src/test/java/com/ggtest/cli/CliArgumentParserTest.java`
  - `src/test/java/com/ggtest/cli/RuntimeConfigResolverTest.java`
  - `src/test/java/com/ggtest/cli/FileRunnerTest.java`
  - `src/test/java/com/ggtest/cli/MainOrchestrationTest.java`（或新增 `src/test/java/com/ggtest/cli/HaltAcceptanceTest.java`）
  - `src/test/java/com/ggtest/runner/SqlLogicTestRunnerTest.java`
  - 新增 fixture（建议置于 `src/test/resources/fixtures/cli/halt/`）：多失败单文件、首文件失败 + 次文件、硬错误 + 次文件、含语料 `halt` 记录 + 次文件

## 验收

> 合同权威：`docs/features/feat-cli-halt/spec.md` §验收（P0-1…P0-6、P1-1…P1-2）。下列映射把每条验收落到验证手段与预期证据。

- **P0-1 默认关闭**：不带 `--halt` 跑多失败 fixture → 全部失败均报告（既有 `multiFailureLayoutUsesBlankSeparatorsAndFlushAt` + T5 新增显式用例）；退出码 `1`。
- **P0-2 单文件首错即停**：多失败 fixture + `--halt` → 仅 1 个失败详情块，其余记录不以 FAILED 出现；退出码 `1`。（T3 单元 + 集成用例）
- **P0-3 多文件全局停**：首文件失败 + 次文件 + `--halt` → 次文件无状态行、不入 `TOTAL`；退出码 `1`。（T4 集成用例）
- **P0-4 硬错误 → 2**：`bad-parse.test` + `pass.test` + `--halt` → `pass.test` 不启动；退出码 `2`；硬错误按现有方式呈现。（T6 用例）
- **P0-5 选项解析**：精确 `--halt` 开启（T1 解析用例）；`-halt`、`--hal` 经 `Main.run` 返回 `2` 且 stdout 无 `[PASSED]`/`TOTAL`（不连库，参照既有 `missingUrlExitsTwoWithoutRunning` 断言模式）。（T1 用例 + Main 层用例）
- **P0-6 与语料 `halt` 区分**：含 `halt` 记录 + 次文件 + `--halt` → 第一文件因记录 `halt` 中止（skipped，非错误），次文件仍执行，退出码 `0`。（T7 用例）
- **P1-1 文档**：README.md / README.zh-CN.md 选项表均含 `--halt` 且简述对齐官方 *Stop when first error is seen*。（T8，Reviewer/QA 文档验收）
- **P1-2 重复 `--halt`**：`--halt --halt` 同单次，非 usage 错误。（T1 解析用例）

预期证据：`mvn -q test` 全绿；`mvn -q clean package` 成功；README 选项表含 `--halt`（grep 可验证）。开发者验证结果记入 `docs/features/feat-cli-halt/dev-notes.md`。

## 文档影响

| 类别 | 更新路径或 N/A 理由 |
|---|---|
| 开发文档 | 代码 Javadoc 更新点：`CliArgumentParser`（选项清单）、`CliOptions`（新字段）、`SqlLogicTestRunner`（「失败不中断文件」语义需补充 `--halt` 例外）、`CliSession`（多文件循环的提前退出条件）。无独立开发文档需新增。 |
| 用户文档 | `README.md`（§CLI synopsis + 选项表）、`README.zh-CN.md`（同）新增 `--halt` 行；简述默认关闭、首错即停、对齐官方。 |
| 运维文档 | N/A。`--halt` 为面向用户的运行时开关，不改变部署、监控、备份恢复、排障流程；退出码集合与优先级不变，既有运维告警阈值不受影响。 |

## 进入 QA 的条件

- Review 门禁为 `required`（standard）：进入 QA 前必须取得 Reviewer `Approve`。
- Reviewer 须按 `docs/standards/quality.md` §3 检查：测试有效性（覆盖 P0/P1 关键路径、无恒真断言、含边界）、文档影响（与本 Plan「文档影响」一致）、安全影响（凭据不入报告——沿用既有脱敏，无新增泄露面）。
- 取得 Approve 后，由 Manager 将状态推进至 QA 调度；QA 依据 Spec + 本 Plan 独立验收，结论写入 `docs/features/feat-cli-halt/qa-report.md`。

## 验证风险与恢复

- 当前无已知无法执行的验证项。`mvn -q test` 所需仅为 JDK 17 + Maven + 内存 SQLite，仓库已具备。
- 若 CI/本地因 Maven 仓库不可达、JDK 缺失等导致 `mvn test` 无法执行：记录具体障碍到 `dev-notes.md` 与工作项记录「阻塞原因/恢复条件」，评估风险（无法证明 P0/P1 通过），声明恢复条件（环境就绪后补跑 `mvn -q test` 与定向用例）。在补跑完成前不得进入 QA Pass 或合并。
- Postgres 硬错误停跑（P0-4 的 PG teardown 路径）不在最低验证层：以 `bad-parse.test`（解析硬错误）等价覆盖「硬错误 → 不启动后续文件 → 退出码 2」合同；如需 PG 实测，须设 `GGTEST_PG_URL`，属可选增强而非必需。

## 交接顺序

1. **实施（Developer）**：按 T1→T2→T3→T4→T5/T6/T7→T8 顺序实现；执行 `mvn -q test` 与 `mvn -q clean package`；将验证结果与偏差记入 `docs/features/feat-cli-halt/dev-notes.md`。
2. **Review（Reviewer）**：依本 Plan「验收」「文档影响」「进入 QA 的条件」逐项核验，给出 Approve/Change-request；standard 路径 Approve 是 QA 前置。
3. **QA**：Reviewer Approve 后由 Manager 调度；QA 依 Spec P0/P1 + 本 Plan 最低验证层独立验收，产出 `qa-report.md`（Pass/Fail/Blocked）。
4. **合并**：QA Pass 后，依工作项记录「用户授权记录」等待用户合并授权；Planner/Developer/Reviewer/QA 均不擅自 merge。

## 修订记录

| 日期 | 摘要 |
|---|---|
| 2026-08-07 | 初稿：依据 spec.md 冻结合同制定任务拆解、触碰路径、验收映射与文档影响。 |
