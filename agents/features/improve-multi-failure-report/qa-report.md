# QA Report: improve-multi-failure-report

## 轮次

| 轮次 | 日期 | 范围 | 结论 |
|---|---|---|---|
| 1 | 2026-08-06 | 首测：Spec P0/P1 + Plan L3（独立复跑） | Pass |

## 环境与命令

- 分支：`improve-multi-failure-report`；实现**未** commit（工作区相对 `HEAD`=`10021b7` / `main`）
- 环境：Linux；OpenJDK 17.0.19；Maven 3.6.3
- QA 入口：Plan **approved**；Review **Approve**（轮次 2；门禁 required）；路径 standard；状态 `qa`
- 本项实现：`ReportWriter.java`、`FileRunner.java`；`ReportWriterTest` / `FileRunnerTest` / `CliReportAcceptanceTest`；`fixtures/cli/multi-fail.test`；`README.md` / `README.zh-CN.md`；本目录文档
- 不计入本项 / 勿入库：`agents/manager/*`、未跟踪 `sqllogictest/`；**`pom.xml` ≡ `main`（本项无 diff）**

| 命令 | 结果 |
|---|---|
| `mvn test -Dtest=ReportWriterTest,FileRunnerTest,CliReportAcceptanceTest,MainOrchestrationTest` | exit 0；**35/0/0/1**（RW 6；FR 5/skip1；CLI 14；Main 10） |
| `mvn -q clean test`（字面 Plan） | **编译失败**：默认 `maven-compiler-plugin:3.1` → Source/Target option 5（不识别 `maven.compiler.release=17`）。`main` 基线问题，非布局回归。见 §6 |
| `mvn clean test -Dmaven.compiler.source=17 -Dmaven.compiler.target=17`（等价；未改 pom） | exit 0；**233/0/0/18** |
| `mvn -q -DskipTests package -Dmaven.compiler.source=17 -Dmaven.compiler.target=17` | exit 0；`target/ggtest-0.1.0-SNAPSHOT.jar` |
| `git diff main -- pom.xml` | 空 |

## 覆盖（对照 plan 最低验证层 + spec 验收）

Spec approved P0/P1 + Plan L3。合同：无缩进 `at`；N≥2 块间一空行；禁 `[i/N]`/摘要/折叠；`TOTAL.failed` 文件级；本项不改 `pom.xml`。

| ID | 条目 | 结果 | 证据 |
|---|---|---|---|
| P0-1 | 同文件 3 失败：块间空行 + 无缩进 `at`；exit 1；`failed=1`；禁索引/摘要/`reason=` | Pass | `multi-fail` CLI/Runner：3×`at `、块间 `""`→`[WHY]`；无 `[1/`/`failures in file`/`reason=`；`failed=1` exit 1 |
| P0-2 | 单失败无缩进 `at`；完备性；exit 1；`failed=1` | Pass | `fail.test`：`assertFlushAtAndBodyIndent`；单 `at `；exit 1；`failed=1` |
| P0-3 | 两失败文件 → `failed=2`；exit 1；`Error:` 仅两路径 | Pass | `twoFailedFiles…`：`failed=2`；Error 块恰两路径 |
| P0-4 | 布局测已同步且通过 | Pass | 定点 35/0/0/1；全量等价 233/0/0/18 |
| P1-1 | 硬错误多段同规则；计入 failed；exit 2 | Pass（残差 §6） | 共用 `detailLines`（无缩进 `at`）；断言多失败覆盖块间空行；无多段纯硬错误独立 fixture |
| L3 | Plan 最低验证层 | Pass | 定点绿；全量 Failures=0（等价命令）；package SUCCESS；字面 clean 见 §6 |
| 排除 | 无 WI-2/WI-3；无本项 `pom`；无新 CLI 标志 | Pass | diff 无 overflow/precision/`pom.xml`；产品无禁止文案 |

文档：README EN/ZH「报告」与 Spec 同构；`dev-notes.md` 含验证/P0-P1/§6/R1。运维 N/A。

安全：范围=报告格式化与测断言；无认证/授权/新路径/出站；无本项依赖变更；无凭据写入。无安全阻塞；允许合并（待用户授权；QA 不执行合并）。

回归：定点四测类 + 全量 Maven（既有 skip）。

### quality.md §6

| 未验证项 | 原因 | 风险 | 恢复条件 | 复测范围 |
|---|---|---|---|---|
| 字面 `mvn -q clean test` | Maven 3.6.3 默认 compiler 3.1 忽略 `release`；Plan/R1 禁止本项改 `pom`；`pom` ≡ `main` | 字面 clean 失败易误判为本项回归；布局证据已由 `-Dmaven.compiler.source/target=17` 覆盖 | 独立 chore：在 `main` 声明支持 `release` 的 compiler 插件，或文档化必需 `-D`；**勿**借本项改 `pom` | 字面 `mvn -q clean test` + package |
| P1-1 多段纯硬错误块间空行独立 fixture | parse/connection 通常单段 | 非 FAILED 循环的多段硬错误可能缺空行 | 可稳定复现的多段 hard-error fixture | `FileRunnerTest` + CLI 硬错误 |

## 缺陷

| ID | 严重度 | 摘要 | 状态 |
|---|---|---|---|
| — | — | 无 | — |

## 结论

- 总体: **Pass**
- 恢复条件: N/A
- 合并: 待用户授权（本轮 **未** commit / push / merge；`qa-report.md` 留工作区；**停 merge-auth**）
- 残余风险:（1）字面 clean 依赖基线 toolchain（§6）；（2）`agents/manager/*` 由 Manager 择机入库；（3）`sqllogictest/` 勿入库；（4）P1-1 残差不阻塞
- 建议下一步: **merge-auth**（Manager 停授权；QA 不执行合并）；授权后 Manager 置 `done` 并与未入库报告/实现一次提交，再合入 `main`
