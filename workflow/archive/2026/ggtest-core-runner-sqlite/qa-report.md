# QA Report: ggtest-core / runner-sqlite

> **工作项**：`ggtest-core` · **sub-feature-id**：`runner-sqlite`  
> **路径**：full · **Review 门禁**：required（入口前已 Approve）  
> **Design**：required（`design.md` 已用户 approved）  
> **源分支** → **目标分支**：`ggtest-core-runner-sqlite` → `main`

## 轮次

| 轮次 | 日期 | 实现版本 / 范围 | 环境 | 结论 |
|---|---|---|---|---|
| 1 | 2026-07-25 | `5cf84fc`（前置 docs `e359f05`）；Plan T1–T5；P0-3/P0-6/P0-8/P1-2/P1-4；parser/normalize 回归 | macOS aarch64；JDK 17.0.20；Maven 3.9.16 | Pass |

## 入口门禁核验（轮次 1）

| 条件 | 证据 | 结果 |
|---|---|---|
| Plan 已用户确认并持久化 | Manager 调度声明；`workflow/archive/2026/ggtest-core/ggtest-core.md` | 满足（Manager 已核验） |
| Spec 已用户确认 | 同记录；Spec approved | 满足 |
| Review required 且 Approve | `review.md` 结论 Approve；无阻塞必修项 | 满足 |
| 可验收实现与 Plan 验证 | `com.ggtest.db` / `db.sqlite` / `runner` + fixtures；L3：`mvn -q clean test` | 满足 |

## 环境与命令（轮次 1）

- 工作区：`/Users/zhougangjie/Space/ggtest`；分支 `ggtest-core-runner-sqlite` @ `5cf84fc`
- JDK：OpenJDK 17.0.20（Homebrew）
- Maven：`~/tools/apache-maven-3.9.16/bin/mvn`（3.9.16）
- 命令：`mvn -q clean test` → 退出码 0；`mvn clean test`（明细）→ 退出码 0 / BUILD SUCCESS
- Surefire：Tests run: **84**, Failures: **0**, Errors: **0**, Skipped: **0**
  - runner 31（依赖隔离 3 + 验收 7 + 状态机 21）+ db 17（抽象 7 + SQLite 10）
  - 回归：parser 10 + normalize 26 = 36，0 失败
- 驱动：`org.xerial:sqlite-jdbc:3.53.2.0`；验收库 `jdbc:sqlite::memory:`；本轮依赖已缓存，未用代理

## 覆盖（对照 Spec 验收 + Plan 验证）

### Spec 验收

| ID | 要求 | 结果 | 证据 |
|---|---|---|---|
| P0-3 | `statement ok` 与 `statement error` 均通过；error 条换合法 SQL 后该条失败 | 通过 | `p0-3-statement-assertions.test` → 4× PASSED（`p0_3_statementOkAndStatementErrorBothPass`）；`p0-3-statement-error-made-valid.test` 末条 FAILED（`p0_3_statementErrorWithValidSqlFails`）；真库 `SqliteJdbcExecutor` |
| P0-6 | engine=`sqlite`：`skipif sqlite` / `onlyif sqlite` / `onlyif postgresql` → 第一、三条 skipped，第二条执行 | 通过 | `p0-6-conditions.test`：SKIPPED/PASSED/SKIPPED；被跳过 SQL 指向缺失表；换 `duckdb` 假执行器后条件判定改变（`runnerDrivesAnyExecutorImplementation`） |
| P0-8 | runner 仅依赖执行器抽象；SQLite 可整体替换而不改 parser/runner 源码 | 通过 | 独立检查 runner 主源码：仅 `com.ggtest.db` + model/normalize，无 `db.sqlite`/`java.sql`/`org.sqlite`/`org.xerial`；`db` 顶层无 JDBC；`RunnerDependencyIsolationTest` 3 Pass；Reviewer Approve |
| P1-2 | `halt` 后记录不执行且计 skipped | 通过 | `p1-2-halt.test`：PASSED + 2× SKIPPED；`halted()` 真；原因含 `halt`；halt 后 SQL 指向缺失表 |
| P1-4 | 同 label 结果冲突 → 后出现记录失败并指明 label 冲突 | 通过 | `p1-4-label-conflict.test`：末条 FAILED；原因含 `label` 与 `samevals`（`label conflict: label 'samevals' ...`） |

### Plan 验证

| 项 | 要求 | 结果 | 证据 |
|---|---|---|---|
| 最低验证层 | L3（单元/组件 + SQLite JDBC 内存库 + 构建） | 通过 | 假执行器编排 + 真库验收 + BUILD SUCCESS |
| 验证命令 | `mvn -q clean test`（Java 17） | 通过 | JDK 17；退出码 0 |
| 预期证据 | BUILD SUCCESS；Surefire 全过（含 parser/normalize） | 通过 | 84/84；Failures/Errors/Skipped: 0 |
| T1–T4 | 执行器抽象、Runner、SQLite JDBC、fixtures/验收 | 通过 | 包边界与上表；无 CLI/退出码/目录收集/官方语料硬验收 |
| T5 开发文档 | README + Javadoc + `dev-notes.md` | 通过 | 见文档验收 |

### 回归

| 范围 | 结果 | 说明 |
|---|---|---|
| parser 10 | 通过 | 同次 Surefire；0 失败 |
| normalize 26 | 通过 | 同次 Surefire；0 失败 |
| runner/db 新增 48 | 通过 | 31 + 17 |

### 文档验收

| 类别 | Plan 声明 | 结果 | 证据 |
|---|---|---|---|
| 开发文档 | README + Javadoc + `dev-notes.md` | 通过 | README「Runner usage」「Supporting another database」含 `jdbc:sqlite::memory:` 与 `DatabaseExecutor` 示例；Javadoc；`dev-notes.md` 含 L3 证据与驱动版本 |
| 用户文档 | N/A | 通过（N/A） | 无 CLI（属 cli-corpus） |
| 运维文档 | N/A | 通过（N/A） | 无部署面 |

### 安全验收（`security.md`）

| 检查项 | 结果 | 备注 |
|---|---|---|
| 范围 | 执行语料 SQL；调用方持有 `Connection`；新增 `sqlite-jdbc`；错误摘要进 `RecordResult` | 无认证授权；内存库；无出站；fixtures 只读 |
| 敏感信息 | 无发现 | 代码/测试/fixtures/文档无凭据；摘要不含连接串 |
| 输入处理 | 可接受 | 有意执行语料 SQL；首期无 message/正则匹配 |
| 文件操作 | 可接受 | classpath fixtures 只读 |
| 依赖变更 | 可接受 | 钉死 `org.xerial:sqlite-jdbc:3.53.2.0`（compile） |
| 处置状态 | 无需处置 | 允许请求合并授权；本轮不执行合并 |

## 缺陷

| ID | 严重度 | 摘要 | 状态 | 处理说明 / 验证证据 |
|---|---|---|---|---|
| — | — | 无 | — | — |

非阻塞观察（不阻断 Pass；对齐 Reviewer N1–N2）：

- N1：`SqliteJdbcExecutor` 致命判定含消息启发式；cli-corpus 若遇误中止可收紧
- N2：无「多条 skipif+onlyif 叠加」真库专项 fixture（假执行器已覆盖）

## 结论（轮次 1）

- **总体：Pass**
- 恢复条件：N/A
- 合并：不合并（本调度禁止；禁止将工作项置 `done`）
- 质量条件：Plan 确认 + Review Approve + QA Pass + 源/目标分支已声明 → 已满足请求合并授权的前提
- 建议后续：Manager 请求用户合并授权 → 源分支置 `done` → Merge Executor 将 `ggtest-core-runner-sqlite` 合入 `main`
