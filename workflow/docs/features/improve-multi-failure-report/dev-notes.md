# Dev notes: improve-multi-failure-report

- 工作项: improve-multi-failure-report（未拆分）
- 分支: `improve-multi-failure-report`（自 `main` 创建并检出）
- 约束: Spec/Plan approved；**未** commit / push / merge

## 实现摘要

- `ReportWriter.detailLines`：`at <file>[:<line>]` 无前导缩进；`[WHY]`/`[SQL]`/`[Diff]` 仍四空格。
- `FileRunner.runWithExecutor`：下一条 `FAILED` 明细前，若已有明细则追加恰好一空行。
- 测试 + fixture `multi-fail.test`（3 断言失败）：无缩进 `at`、块间空行、禁止 `[i/N]` / `failures in file` / `reason=`；P0-3 两失败文件。
- README（EN/ZH）报告样例同步。

未改：继续执行 / abort / halt；`TOTAL.failed` 文件计数；退出码；CLI 标志；WI-2/WI-3；`pom.xml`；`sqllogictest/`。

## 变更路径

| 路径 | 变更 |
|---|---|
| `src/main/java/com/ggtest/cli/ReportWriter.java` | `at` 无缩进 |
| `src/main/java/com/ggtest/cli/FileRunner.java` | 失败块间空行 |
| `src/test/java/com/ggtest/cli/ReportWriterTest.java` | 布局断言 |
| `src/test/java/com/ggtest/cli/FileRunnerTest.java` | 多失败 / flush `at` / 硬错误共享路径 |
| `src/test/java/com/ggtest/cli/CliReportAcceptanceTest.java` | P0-1/P0-2/P0-3 |
| `src/test/resources/fixtures/cli/multi-fail.test` | 三失败 fixture |
| `README.md` / `README.zh-CN.md` | 报告样例 |

## 验证

| 步骤 | 命令 | 结果 |
|---|---|---|
| T0 基线 | `mvn -q test` | tests=228 failures=0 errors=0 skipped=16 |
| T1 Red | `mvn -q test -Dtest=ReportWriterTest,FileRunnerTest,CliReportAcceptanceTest` | 失败指向缩进 `at` / 缺块间空行 |
| T2 Green | 同上 + `MainOrchestrationTest` | Failures=0（RW 6；FR 5/skip1；CLI 14；Main 10） |
| T3 | `mvn -q clean test` | tests=233 failures=0 errors=0 skipped=18 |
| T3 | `mvn -q -DskipTests package` | SUCCESS（`target/ggtest-0.1.0-SNAPSHOT.jar`） |

无关 Skipped：PG/语料等环境依赖；T3 skipped 相对基线 +2 为环境波动，非本项失败。

## P0 / P1

| ID | 结果 | 证据 |
|---|---|---|
| P0-1 | Pass | `multi-fail` CLI/FileRunner：3 块、块间一空行、无缩进 `at`；exit 1；`failed=1`；无禁止文案 |
| P0-2 | Pass | `p0_2` + `ReportWriterTest`：无缩进 `at`；单 `at`；exit 1；`failed=1` |
| P0-3 | Pass | `twoFailedFiles…`：`failed=2`；exit 1；`Error:` 仅两路径 |
| P0-4 | Pass | 布局测已同步；T3 Failures=0 |
| P1-1 | 部分 | 硬错误共用 `detailLines`（无缩进 `at`）；断言多失败覆盖块间空行。残差：无多段纯硬错误独立 fixture |

## §6 缺口

| 未验证项 | 原因 | 风险 | 恢复条件 | 复测范围 |
|---|---|---|---|---|
| 多段纯硬错误块间空行 | parse/connection 通常单段；abort 前已有 FAILED 则走断言拼接 | 非 FAILED 循环的多段硬错误拼接可能缺空行 | 可稳定复现的多段 hard-error fixture | `FileRunnerTest` + CLI 硬错误 |

## Review 修复回执

| 缺陷 ID | 处理结果 | 修复摘要 | 验证证据 | 建议复测范围 |
|---|---|---|---|---|
| R1 | 已修复 | `git restore pom.xml`：去掉无关 `maven-compiler-plugin` 改动（Plan 禁止）；本项 diff **不含** `pom.xml` | `git status` 无 `M pom.xml`；`mvn -q test -Dtest=ReportWriterTest,FileRunnerTest,CliReportAcceptanceTest` exit 0 | reviewing 复审：确认 diff 无 `pom.xml` |

未 commit（按 Manager 指示）。

## 开放问题

无。

## 建议后续

reviewing（复审 R1）→ Approve → QA。
