# Review: ggtest-core / cli-corpus

> **工作项**：`ggtest-core` · **sub-feature-id**：`cli-corpus`  
> **路径**：full · **Review 门禁**：required · **Design**：N/A（skipped）  
> **审阅版本**：`466c6f1`（实现；前置 docs `0ed8a95`）；分支 `ggtest-core-cli-corpus` → `main`；Plan T1–T6  
> **依据**：[`spec.md`](./spec.md)、[`plan.md`](./plan.md)、[`dev-notes.md`](./dev-notes.md)；`documentation.md` / `quality.md` / `security.md` / `git.md`

## 审阅范围

- 实现：`com.ggtest.cli`（`Main`、`CliArgumentParser`、`CliOptions`、`UsageException`、`TestFileCollector`、`CliSession`）；`bin/ggtest`；`pom.xml`（jar/shade、`Main-Class`）
- 测试：`src/test/java/com/ggtest/cli/*`；`src/test/resources/fixtures/cli/*`（自造小文件）
- 文档：`README.md`、CLI Javadoc、`dev-notes.md`
- Git：源分支 `ggtest-core-cli-corpus`；提交 `0ed8a95`、`466c6f1`
- 未改：`parser` / `normalize` / `runner` / `db` 行为源码（相对 `main`/`5a61e2d` diff 为 0）
- 未审改：未提交的 `docs/manager/STATUS.md` / `ggtest-core.md`（Manager 状态机，非本切片实现）

## 结论

**Approve**

无阻塞实现/测试/文档/安全项，可进 QA。P0-1/P1-5 硬验收记为**未执行（阻塞）**（非通过、非默示豁免）；缺口 alone 不构成 `Request changes`。QA 须补跑或继续记阻塞。

## 实现正确性

| 要求 | 证据 | 结果 |
|---|---|---|
| CLI 入口 `ggtest` | `Main` + `bin/ggtest` + shade `Main-Class=com.ggtest.cli.Main`；独立 `./bin/ggtest` smoke 退出码 0 | 通过 |
| 参数与默认 | `--url` 必填；`--engine` 默认/仅 `sqlite`；`--hash-threshold` 默认 8；≥1 位置参数；用法错 → 2 | 通过 |
| 目录收集 | 递归 `*.test`/`*.slt`，绝对路径字典序；单文件不强制扩展名 | 通过 |
| 报告 | 分文件 `passed/failed/skipped` + `TOTAL`；`FAILURE` 四要素；解析 `ERROR` 后继续 | 通过 |
| 退出码 0/1/2 | 全过→0；断言失败→1；用法/解析/连接→2（独立 smoke） | 通过 |
| 跨文件重置 | 每文件新建 `SqlLogicTestRunner(executor, cliHashThreshold)`；跨文件阈值测试 | 通过 |
| 组装上游不改行为 | 相对 `5a61e2d` 无 parser/normalize/runner/db 源码 diff | 通过 |
| P1-1 / P1-6（fixtures） | nested 目录；`same-content.slt` ≡ `.test` | 通过 |
| P0-1 / P1-5 硬验收 | 无语料；`CorpusHardAcceptanceTest` Skipped×2；`dev-notes` 记未执行 | **未执行（证据缺口）** |

独立验证：

```text
JAVA_HOME=OpenJDK 17.0.20 (Homebrew)
PATH=~/tools/apache-maven-3.9.16/bin:$PATH
mvn clean test
→ BUILD SUCCESS
→ Tests run: 110, Failures: 0, Errors: 0, Skipped: 3
  （cli≈26：参数8+收集5+编排9+JAR/脚本2+硬验收可选2；
    Skipped：硬验收×2；Manifest×1 @ clean test 无 JAR；上游 84 无回归）

mvn -q clean package → BUILD SUCCESS
./bin/ggtest …/pass.test → 0；…/nested → 0+分文件+TOTAL；…/fail.test → 1；…/bad-parse.test → 2
缺位置参数 → 2；stderr 不含测试口令
```

### 硬验收证据缺口（未验证项 → 原因 → 风险 → 恢复 → 复测）

| 项 | 内容 |
|---|---|
| 未验证项 | P0-1（select1）、P1-5（select1/2/3）；L4 官方语料零豁免 |
| 原因 | `GGTEST_CORPUS_DIR` 未设；审阅环境无可用语料 |
| 风险 | 官方语料失败数=0、退出码=0 尚未证实 |
| 恢复 | 提供含 select1/2/3 的目录 → Plan 硬验收命令或 `GGTEST_CORPUS_DIR=… mvn test` → 补 `dev-notes`/`qa-report` |
| 复测范围 | P0-1、P1-5（及可选自动化硬验收） |
| 处置 | 未标通过、未默示豁免；其余 CLI 合同已证 → Review Approve；QA 补证或记 Blocked |

## 测试有效性

- Plan T1–T5 fixtures：用法/收集/退出码/报告四要素/跨文件阈值/目录与 `.slt`/凭据不落盘；硬验收无语料时 Assume skip（不假绿）。
- 可失败性：缺 `--url`、非法 engine、坏解析、结果不匹配、只读缺失库、空目录。
- 上游 84/84 Pass；TDD：测试类对齐 T1–T4，`dev-notes` 声明先红后绿。
- L4：CLI+内存 SQLite 已证；官方语料层缺口见上表。
- 非阻塞：编排断言偏松；`--engine` 未覆写 `executor.engineName()`（首期仅 sqlite，语义一致）。

## 文档影响核对

| Plan 声明 | 一致 | 备注 |
|---|---|---|
| 开发文档：README 构建/打包、Javadoc、`dev-notes.md` | 是 | L4/硬验收缺口与恢复条件完整 |
| 用户文档：README CLI/退出码/目录/语料自备/凭据勿入报告 | 是 | 含 P0-1 形态示例与 `GGTEST_CORPUS_DIR` |
| 运维文档 N/A | 是 | 本地 CLI，无部署面 |

## 安全影响核对

检查范围：CLI 接受 `--user`/`--password` 与 JDBC URL；执行用户 SQL 文件；目录递归读文件；无新增运行时依赖（沿用 sqlite-jdbc）；无出站网络（测试内存库）。

| 检查项 | 结果 | 备注 |
|---|---|---|
| 敏感信息 | 无发现 | fixtures/README/提交无真实凭据；报告路径不写 password；用法错消息不含口令值 |
| 官方大语料入库 | 无 | fixtures 仅自造小文件（cli 目录约 36K）；无 select1/2/3 官方原文入库 |
| 认证与授权 | N/A | 凭据仅传入 `DriverManager` |
| 输入处理 | 可接受 | 有意执行用户语料 SQL；路径存在性校验；目录 walk 仅收集扩展名 |
| 文件操作 | 可接受 | 只读收集/解析；无写回 |
| 依赖变更 | 可接受 | shade/jar 插件；无新库坐标 |
| 处置状态 | 无需处置 | 允许进入 QA |

## Git 合规

| 检查项 | 结果 |
|---|---|
| 工作分支 | `ggtest-core-cli-corpus`（非 main） |
| 提交 | `0ed8a95` docs；`466c6f1` feat；Conventional Commits |
| 禁止提交项 | 无密钥/`.env`/官方大语料；`target/` 已 ignore |
| 越界 | 未改上游行为源码；实现提交未改 Spec/Plan |

## 必修项

| ID | 位置 | 问题 | 状态 |
|---|---|---|---|
| — | — | 无 | — |

## 非阻塞建议

| ID | 位置 | 建议 |
|---|---|---|
| N1 | `CliOptions`（record） | 默认 `toString` 会含 password；若未来日志打印 options，宜覆写并脱敏 |
| N2 | `CliSession.sanitize` | 注释称防凭据回显，实际仅 `strip`；可与 N1 一并加固 |
| N3 | `CliOptions.engine` | 解析后未传入 Runner（engine 来自 `SqliteJdbcExecutor`）；首期仅 sqlite 可接受；多引擎时须接线 |
| N4 | `MainOrchestrationTest` | 可收紧对 `FILE:`/`TOTAL: failed=N` 的精确断言 |

## 后续动作

1. Manager：调度 **QA**（`qa-report.md`）。
2. QA：fixtures 验收 P1-1/P1-6 与退出码；**P0-1/P1-5** 在有语料时补跑，否则按 Plan 记阻塞（原因/风险/恢复），禁止标通过或默示豁免；`mvn -q clean test`（JDK 17）+ 可选 `package`/`./bin/ggtest`。
3. Developer：N1–N4 可选，不阻断 QA；语料到位后补硬验收证据。
4. 复审：无（Approve；若 QA Fail 或硬验收暴露实现缺陷再审）。
