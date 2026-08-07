# QA Report: feat-cli-halt

> 独立验收报告。依据 `spec.md`（P0-1…P0-6 / P1-1 / P1-2 为验收基线）与 `plan.md`（最低验证层 L2）。
> QA 不复述 Developer/Reviewer 自述，所有结论基于本分支独立执行取证。

## 轮次

| 轮次 | 日期 | 实现版本 | 结论 |
|---|---|---|---|
| 1 | 2026-08-07 | `feat-cli-halt` 分支 HEAD `3bbf40d`（4 commits：`7dce363`→`13a7713`→`dd5266b`→`3bbf40d`；基线 `main`） | **Pass** |

## 环境与验收范围

- OS：Microsoft Windows NT 10.0.26200.0
- JDK：Eclipse Adoptium 17.0.19；Maven 3.9.8
- git：`core.autocrlf=true`（Windows 默认，与 pre-existing CRLF 失败相关）
- 分支：`feat-cli-halt`（源）→ `main`（目标）
- Review 门禁：required（standard）— Reviewer 已 `Approve`（见 `review.md`）
- 验收范围：Spec P0-1…P0-6 / P1-1 / P1-2 逐项；Plan L2 验证命令；独立 CLI 级回归；文档与安全验收

## QA 入口门禁核对

1. Plan 已用户确认且持久化 ✓（`agents/manager/feat-cli-halt.md` 进度笔记 2026-08-07）
2. standard Review 门禁 required → Reviewer `Approve` 已取得 ✓（`review.md` 轮次 1）
3. 存在可验收实现 ✓（4 commits 在 `feat-cli-halt` 分支）

入口条件全部满足。

## Spec 验收条件逐项核对

> 每条：要求摘要 → 独立执行命令与证据 → 结论。CLI 级取证使用构建产物 `target/ggtest-0.1.0-SNAPSHOT.jar`，fixture 取自 `src/test/resources/fixtures/cli/`。

### P0-1 默认关闭

- 要求：argv 无 `--halt` 跑多失败单文件 → 后续失败仍执行并报告；退出码 `1`。
- 命令：`java -jar target/ggtest-0.1.0-SNAPSHOT.jar --url jdbc:sqlite::memory: multi-fail.test`（3 个失败 query）
- 证据：stdout 含 3 个 `[WHY]` 块（apple/banana/cherry 均报告）；`TOTAL: passed=0 failed=1 skipped=0`；`EXIT CODE: 1`。
- 结论：**Pass**

### P0-2 单文件首错即停

- 要求：argv 含 `--halt` → 仅报告首条失败，其后不执行且不以 FAILED 出现；退出码 `1`。
- 命令：`java -jar … --halt --url jdbc:sqlite::memory: multi-fail.test`
- 证据：stdout 仅 1 个 `[WHY]` 块（apple）；banana/cherry 不出现；`TOTAL: passed=0 failed=1 skipped=0`；`EXIT CODE: 1`。
- 结论：**Pass**

### P0-3 多文件全局停

- 要求：首文件断言失败 + 次文件 + `--halt` → 次文件无状态行、不计 `TOTAL`；退出码 `1`。
- 命令：`java -jar … --halt --url jdbc:sqlite::memory: multi-fail.test pass.test`
- 证据：`pass.test started: False`；`[PASSED] in output: False`；`TOTAL: passed=0 failed=1 skipped=0`（仅计首文件）；`EXIT CODE: 1`。
- 结论：**Pass**

### P0-4 硬错误 → 2

- 要求：`--halt` + 硬错误文件 → 后续文件不启动；退出码 `2`；硬错误按现有方式呈现。
- 命令：`java -jar … --halt --url jdbc:sqlite::memory: bad-parse.test pass.test`
- 证据：stdout 含 `[WHY] parse error: unknown record type: this`（现有格式）；`pass.test started: False`；`TOTAL: passed=0 failed=1 skipped=0`；`EXIT CODE: 2`。
- 结论：**Pass**

### P0-5 选项解析

- 要求：精确 `--halt` 开启；`-halt`/`--hal` → usage，退出码 `2`，不连库。
- 命令（a）：`java -jar … -halt --url jdbc:sqlite::memory: pass.test` → `EXIT: 2`；`Error: usage` + `[WHY] unknown option: -halt`；`hasTOTAL: False`（未连库、未跑文件）。
- 命令（b）：`java -jar … --hal --url jdbc:sqlite::memory: pass.test` → `EXIT: 2`；`[WHY] unknown option: --hal`；`hasTOTAL: False`。
- 结论：**Pass**（精确解析见 `CliArgumentParser.java:60` `case "--halt"`；前缀/短形式落 `default` 抛 `UsageException`）

### P0-6 与语料 halt 区分

- 要求：成功记录 + 会执行的语料 `halt` + 其后记录 + 第二文件，argv 含 `--halt` → 第一文件因记录 `halt` 中止（skipped，非错误），第二文件仍执行；退出码 `0`。
- 命令：`java -jar … --halt --url jdbc:sqlite::memory: halt/corpus-halt.test pass.test`
- 证据：`pass.test started: True`；`TOTAL: passed=2 failed=0 skipped=0`；`EXIT CODE: 0`。
- 结论：**Pass**（`SqlLogicTestRunner` 中 `halted`（语料）与 `haltedOnFirstFailure`（CLI）为两个独立局部标志；`FileRunResult.halted` 语义未重载）

### P1-1 文档

- 要求：README 选项表含 `--halt` 且简述对齐 *Stop when first error is seen*。
- 证据：`README.md:46` synopsis 含 `[--halt]`；`README.md:58` 选项表含 `--halt` 行（默认 off，首错即停，含退出码优先级不变与语料 halt 区分说明）。`README.zh-CN.md:34` synopsis 含 `[--halt]`；`README.zh-CN.md:47` 选项表含 `--halt` 行（见首个错误即停）。语义与 Spec §API 一致。
- 结论：**Pass**

### P1-2 重复 --halt

- 要求：两次 `--halt` 同单次（非 usage 错误）。
- 命令：`java -jar … --halt --halt --halt --url jdbc:sqlite::memory: multi-fail.test`
- 证据：`EXIT: 1`（非 usage 的 `2`）；仅 1 个 `[WHY] query` 失败块；`TOTAL: passed=0 failed=1 skipped=0`。与单次 `--halt` 行为一致。
- 结论：**Pass**

## Plan 验证命令执行结果

| 命令 | 结果摘要 |
|---|---|
| 定向：`mvn test "-Dtest=CliArgumentParserTest,RuntimeConfigResolverTest,FileRunnerTest,MainOrchestrationTest,SqlLogicTestRunnerTest,CliReportAcceptanceTest"` | Tests run: **100**, Failures: 0, Errors: 0, Skipped: 1（FileRunnerTest PG 门控）；BUILD SUCCESS |
| 全量：`mvn test` | Tests run: **250**, Failures: 0, Errors: 1（NormalizeAcceptanceTest，pre-existing），Skipped: 16（PG/corpus 门控）；BUILD FAILURE（仅因 pre-existing） |
| 打包：`mvn clean package -Dtest='!NormalizeAcceptanceTest'` | Tests run: **244**, Failures: 0, Errors: 0, Skipped: 18；BUILD SUCCESS；`target/ggtest-0.1.0-SNAPSHOT.jar`（13,537,888 bytes ≈ 13.5 MB） |

> 全量 `mvn test` 的 1 个 Error 为 pre-existing（见下「NormalizeAcceptanceTest 处置」）；定向与打包（排除 pre-existing）全绿。证据与 `dev-notes.md` 计数一致（100 / 250 / 244）。

## 独立 CLI 级回归

覆盖既有受影响行为，不依赖测试框架自述：

| 行为 | 命令（摘要） | 结果 |
|---|---|---|
| 默认关闭跑完 | `multi-fail.test` 不带 `--halt` | 3 个失败全报告，exit 1 ✓ |
| 首错停跑（单文件） | `--halt multi-fail.test` | 仅 1 个失败，exit 1 ✓ |
| 多文件全局停 | `--halt multi-fail.test pass.test` | pass.test 不启动，exit 1 ✓ |
| 硬错误停跑 | `--halt bad-parse.test pass.test` | pass.test 不启动，exit 2 ✓ |
| 选项拒绝 | `-halt` / `--hal` | usage，exit 2，未连库 ✓ |
| 语料 halt 区分 | `--halt halt/corpus-halt.test pass.test` | 两文件均跑，exit 0 ✓ |
| 重复 --halt | `--halt --halt --halt multi-fail.test` | 同单次，exit 1 ✓ |
| 退出码集合 | 各场景 | 0/1/2 优先级不变 ✓ |
| 报告格式 | 各场景 stdout | `[FAILED]`/`[WHY]`/`[SQL]`/`[Diff]`/`at`/`Error:`/`TOTAL` 沿用现有格式 ✓ |

## NormalizeAcceptanceTest 处置（pre-existing）

**判定：与本工作项无关（pre-existing），不判为本次 Fail。**

独立取证：

| 维度 | 证据 |
|---|---|
| 改动范围 | `git diff --name-only main..HEAD` 与 `git log main..HEAD -- src/**/normalize/**` 均**空** → normalize 包（代码 + 测试 + fixture）零改动 |
| 失败复现 | 独立 `mvn test` → `NormalizeAcceptanceTest.p0_2_hashMatchesSelect1CorpusExcerpt` ERROR `IllegalArgument fixture missing values section` @ `extractValuesSection:120` |
| 根因 | `core.autocrlf=true`（Windows 默认），git 检出将 fixture `p0-2-select1-hash.txt` 的 LF 转 CRLF（实测工作区 37 个 CRLF）；测试以 `indexOf("values:\n")` 查找标记，CRLF 下匹配失败 |
| 影响面 | 仅 `normalize` 包，与 `--halt`（cli/runner）无代码或数据依赖 |
| 风险 | 不影响 `--halt` 合同与回归；仅 Windows CRLF 检出环境影响该 normalize 用例 |
| 处置 | 建议作为独立 chore 工作项修复 `.gitattributes`（强制测试资源 LF，如 `*.txt text eol=lf`）；超出本工作项范围，不在本工作项实施 |

依据 `quality.md` §6 与 `qa.md`「pre-existing 失败处理」：零 diff 双证 + 根因独立于改动范围 → 判定 pre-existing，不阻塞本工作项。

## 文档验收

| 项 | 结论 | 证据 |
|---|---|---|
| `README.md` synopsis + 选项表 | Pass | synopsis 含 `[--halt]`；选项表 `--halt` 行简述对齐官方 *Stop when first error is seen*，含默认关、首错即停、退出码优先级不变、语料 halt 区分 |
| `README.zh-CN.md` synopsis + 选项表 | Pass | 同步更新；语义与 Spec §API 一致 |
| Javadoc（开发文档） | Pass | `CliArgumentParser` / `CliOptions` / `SqlLogicTestRunner` / `CliSession` / `ParsedArguments` 五处 Javadoc 均补充 `--halt` 行为/例外（diff 可见） |
| 运维文档 | N/A | `--halt` 为运行时开关；退出码集合与优先级不变；部署/监控/备份/排障无影响 |

## 安全验收

检查范围与变更实际影响面（cli/runner 控制流 + 选项解析 + 脱敏字段）一致。

| 检查项 | 结论 | 证据 |
|---|---|---|
| 凭据不入报告 | Pass | `CliOptions.toString()` 仅 + `halt=…`（无密钥）；既有 `password`→`***` 脱敏未改（`CliOptions.java:47`）；`FileRunner.sanitize` 沿用 `CredentialRedaction.redactMessage`；`CredentialRedactionTest` 5 用例通过 |
| 无 env/.env 来源 | Pass | `--halt` 仅从 `ParsedArguments.halt()` 透传（`RuntimeConfigResolver.java:98`）；`haltIsNotInferredFromProcessEnvOrDotEnv` 断言 `GGTEST_HALT=true`（env + .env）被忽略，`options.halt()` 保持 false |
| 无新增泄露面 | Pass | 仅新增布尔旗标解析与控制流；无新路径遍历/外联/反序列化/依赖变更（`pom.xml` 零改动） |
| 认证/授权/敏感数据 | N/A | 无认证模型或 PII 变更 |

无未解决安全问题。允许合并。

## 缺陷与阻塞

- **缺陷清单：无**（本工作项范围内无未解决缺陷）。
- **阻塞：无**。
- **非本工作项范围（仅记录，不实施）**：Windows CRLF 致 `NormalizeAcceptanceTest` 既有失败 — 见上「NormalizeAcceptanceTest 处置」。

## 本轮最终结论

**Pass**

Spec P0-1…P0-6 / P1-1 / P1-2 全部通过；Plan L2 验证命令全部执行（定向 100、全量 250、打包 244 全绿，唯一 Error 为 pre-existing 与本工作项无关）；独立 CLI 级回归覆盖默认关闭不变性、退出码、报告格式、选项拒绝、跨文件停跑、语料 halt 区分；文档与安全验收通过；无未解决缺陷或阻塞。满足请求合并授权的质量条件。

## 后续

- 报告已满足请求合并授权的质量条件，**等待用户合并授权**。
- 按角色职责，本报告留工作区；QA 未执行 `git add`/`commit`/`push`；待用户授权后由 Manager 置 `done` 并与未入库报告一次提交，再行合入。
