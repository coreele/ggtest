# QA Report: ggtest-core / cli-corpus

> **工作项**：`ggtest-core` · **sub-feature-id**：`cli-corpus`  
> **路径**：full · **Review 门禁**：required（入口前已 Approve）  
> **Design**：skipped（CLI/退出码已在 Spec 写死）  
> **源分支** → **目标分支**：`ggtest-core-cli-corpus` → `main`

## 轮次

| 轮次 | 日期 | 实现版本 / 范围 | 环境 | 结论 |
|---|---|---|---|---|
| 1 | 2026-07-25 | 实现 `466c6f1`（docs `0ed8a95`、`3376b5a`）；Plan T1–T6；P0-1/P1-1/P1-5/P1-6；L4 | macOS aarch64；JDK 17.0.20；Maven 3.9.16 | **Blocked** |
| 2 | 2026-07-25 | 实现 `466c6f1`；HEAD `cf8e2c9`（含管理文档）；回归复测 P0-1/P1-5 + fixtures；L4 零豁免 | 同左；`GGTEST_CORPUS_DIR=/Users/zhougangjie/Space/sqllogictest/test` | **Fail** |

## 入口门禁核验（轮次 1）

| 条件 | 证据 | 结果 |
|---|---|---|
| Plan 已用户确认并持久化 | Manager 调度；`docs/manager/ggtest-core.md` Plan approved | 满足 |
| Spec 已用户确认 | 同记录；`spec.md` approved | 满足 |
| Review required 且 Approve | `review.md` Approve；版本 `466c6f1`；无阻塞必修项 | 满足 |
| 可验收实现与 Plan 验证 | `com.ggtest.cli` + `bin/ggtest` + fixtures；L4 可执行 | 满足（硬验收语料见阻塞） |

## 环境与命令（轮次 1）

- 工作区：`/Users/zhougangjie/Space/ggtest`；分支 `ggtest-core-cli-corpus`；HEAD `3376b5a`（实现 `466c6f1`）
- JDK：OpenJDK 17.0.20（Homebrew）；Maven `~/tools/apache-maven-3.9.16`（3.9.16）
- `mvn -q clean test` → 退出码 0；Surefire：**Tests run: 110, Failures: 0, Errors: 0, Skipped: 3**
  - cli 26（参数 8 + 收集 5 + 编排 9 + JAR/脚本 2 + 硬验收可选 2）；Skipped：硬验收×2 + Manifest×1（`clean test` 无 JAR）
  - 回归：db 17 + normalize 26 + parser 10 + runner 31 = 84，0 失败
- `mvn -q clean package` → 退出码 0；`Main-Class: com.ggtest.cli.Main`；`target/ggtest-0.1.0-SNAPSHOT.jar` 存在
- `./bin/ggtest --url jdbc:sqlite::memory: <fixtures>` smoke：见 Spec/退出码证据
- 代理：本轮未用（依赖已缓存）

### 语料可用性（独立确认）

| 检查 | 结果 |
|---|---|
| `GGTEST_CORPUS_DIR` | **unset** |
| 常见路径 | `$HOME/{,src/,Space/,Downloads/}sqllogictest`、`/opt|/usr/local/share/sqllogictest`、仓库旁/内 `sqllogictest`、`$HOME/.cache/sqllogictest` → 均无 `select1.test` |
| 工作区 / `$HOME` 浅层检索 | 无官方 `select1.test` / `select2.test` / `select3.test` |

官方三文件**不可得** → P0-1/P1-5 **未执行**（非 Pass、非豁免）。

## 覆盖（对照 Spec 验收 + Plan 验证）

### Spec 验收

| ID | 要求 | 结果 | 证据 |
|---|---|---|---|
| P0-1 | 空白 SQLite + 官方 `select1.test` → 失败数=0、退出码=0 | **未执行（阻塞）** | 无语料；禁止标 Pass / 默示豁免（Q8） |
| P1-1 | 目录递归 `.test`/`.slt`，分文件+总计 | **Pass** | `./bin/ggtest … fixtures/cli/nested` → FILE `a.test` + `sub/b.slt`；`TOTAL: passed=6 failed=0 skipped=0`；退出码 0 |
| P1-5 | 官方 select1/2/3 批量 → 失败数=0、退出码=0、分文件+总计 | **未执行（阻塞）** | 同 P0-1 |
| P1-6 | 合法 `.slt` 与同等 `.test` 统计/退出码一致 | **Pass** | `same-content.slt` ≡ `.test`：二者 `TOTAL: passed=3 failed=0 skipped=0`、退出码 0 |

### Plan 验证（L4）

| 项 | 要求 | 结果 | 证据 |
|---|---|---|---|
| `mvn -q clean test` | BUILD SUCCESS；Surefire 全过（含上游） | **Pass** | 110 run；Failures/Errors 0；Skipped 3 |
| `mvn -q clean package` | 成功且可启动 `ggtest` | **Pass** | package 退出码 0；`./bin/ggtest` 可用 |
| 退出码 0/1/2 | 全过→0；断言失败→1；用法/解析→2 | **Pass** | `pass.test`→0；`fail.test`→1（FAILURE 四要素）；`bad-parse.test`→2；缺位置参数/缺 `--url`→2 |
| 凭据不落盘 | stderr/报告不含 password | **Pass** | `--password SECRET_PASS_XYZ` + 缺位置参数 → 退出码 2；输出无该口令 |
| P0-1 / P1-5 硬验收 | 失败数=0、退出码=0、零豁免 | **Blocked** | 语料不可得；见阻塞表 |

### 回归

| 范围 | 结果 | 说明 |
|---|---|---|
| parser 10 / normalize 26 / runner+db 48 | Pass | 同次 Surefire，0 失败 |
| cli fixtures/编排 | Pass | 除硬验收 skip 外全过 |

### 文档验收

| 类别 | Plan 声明 | 结果 | 证据 |
|---|---|---|---|
| 开发文档 | README 构建/打包、Javadoc、`dev-notes.md` | Pass | README JAR；`dev-notes` 含 L4 与硬验收缺口 |
| 用户文档 | CLI 用法、参数、退出码、目录收集、语料自备、凭据勿入报告 | Pass | README 参数表/退出码/`.slt`、P0-1 示例、`GGTEST_CORPUS_DIR` |
| 运维文档 | N/A | Pass（N/A） | 本地 CLI |

### 安全验收（`security.md`）

| 检查项 | 结果 | 备注 |
|---|---|---|
| 范围 | `--user`/`--password`/JDBC URL；执行用户 SQL；目录只读收集 | 无出站；内存库/自造 fixtures |
| 敏感信息 | 无发现 | 无真实凭据；口令 smoke 未泄漏 |
| 官方大语料入库 | 无 | fixtures 仅自造小文件 |
| 输入/文件/依赖 | 可接受 | 有意执行语料 SQL；shade/jar；无新库坐标 |
| 处置状态 | 无需处置 | 安全不阻断；总体因硬验收 Blocked → **不可**请求合并授权 |

## 缺陷

| ID | 严重度 | 摘要 | 状态 | 处理说明 / 验证证据 |
|---|---|---|---|---|
| — | — | 无 | — | 缺口为环境语料，非实现缺陷 |

## 阻塞（未验证项 → 原因 → 风险 → 恢复 → 复测）

| 项 | 内容 |
|---|---|
| 未验证项 | **P0-1**（`select1.test`）；**P1-5**（`select1.test`+`select2.test`+`select3.test`）；L4 零豁免硬验收 |
| 原因 | `GGTEST_CORPUS_DIR` unset；常见路径与检索均无上述三文件 |
| 风险 | 失败数=0、退出码=0 未证实；不得默示豁免（Q8） |
| 恢复条件 | 用户提供含 **`select1.test`、`select2.test`、`select3.test`** 的目录，设 **`GGTEST_CORPUS_DIR=<目录>`**（或给出 `$SELECT1`/`$SELECT2`/`$SELECT3` 绝对路径）；补跑 `ggtest --url jdbc:sqlite::memory: "$SELECT1"` 与三文件批量（或 `GGTEST_CORPUS_DIR=… mvn test`） |
| 恢复后目标状态 | 仍为 **`qa`**；本报告追加回归轮次，复测 P0-1/P1-5 |
| 处置 | P0-1/P1-5 不得标 Pass；**禁止**请求合并授权 |

## 结论（轮次 1）

- **总体：Blocked**
- 分项：P1-1/P1-6 Pass；`mvn test`/`package`/fixtures 退出码 Pass；**P0-1/P1-5 Blocked（无语料）**
- 缺陷：无；不合并；不请求合并授权
- 后续：用户提供语料/`GGTEST_CORPUS_DIR` → 保持 `qa` → QA 回归硬验收 → 最新轮次 Pass 后方可请求合并授权

---

## 轮次 2（回归；语料已恢复）

### 入口门禁核验（轮次 2）

| 条件 | 证据 | 结果 |
|---|---|---|
| Plan / Spec 用户确认 | Manager 持久化；`spec.md` / Plan approved | 满足 |
| Review Approve | `review.md` Approve；实现 `466c6f1` | 满足 |
| 状态与语料 | Manager `blocked`→`qa`；`GGTEST_CORPUS_DIR` 下 select1/2/3 存在 | 满足 |

### 环境与命令（轮次 2）

- 工作区：`/Users/zhougangjie/Space/ggtest`；分支 `ggtest-core-cli-corpus`；HEAD `cf8e2c9`；实现 **`466c6f1`**
- JDK 17.0.20；Maven 3.9.16；`GGTEST_CORPUS_DIR=/Users/zhougangjie/Space/sqllogictest/test`
- **零豁免**：未对失败标豁免或默示通过

| 命令 | 退出码 | 摘要 |
|---|---|---|
| `GGTEST_CORPUS_DIR=… mvn -q clean test` | **1** | Tests run: **110**, Failures: **1**, Errors: 0, Skipped: **1**；`CorpusHardAcceptanceTest.p1_5_…` 期望 exit 0 实际 1 |
| `GGTEST_CORPUS_DIR=… mvn -q clean package` | **1** | Surefire 失败阻断 package（未 `-DskipTests`） |
| P0-1 CLI | **0** | `TOTAL: failed=0` |
| P1-5 CLI | **1** | `TOTAL: failed=4151` |

### 硬验收命令与证据

**P0-1**（要求：失败数=0、退出码=0）：

```bash
GGTEST_CORPUS_DIR=/Users/zhougangjie/Space/sqllogictest/test
./bin/ggtest --url jdbc:sqlite::memory: "$GGTEST_CORPUS_DIR/select1.test"
```

- 退出码 **0**；`FILE: …/select1.test passed=1031 failed=0 skipped=0`；`TOTAL: passed=1031 failed=0 skipped=0` → **Pass**

**P1-5**（要求：失败数=0、退出码=0、分文件+总计）：

```bash
GGTEST_CORPUS_DIR=/Users/zhougangjie/Space/sqllogictest/test
./bin/ggtest --url jdbc:sqlite::memory: \
  "$GGTEST_CORPUS_DIR/select1.test" \
  "$GGTEST_CORPUS_DIR/select2.test" \
  "$GGTEST_CORPUS_DIR/select3.test"
```

- 退出码 **1**
- 分文件：select1 `failed=0`（passed=1031）；select2 `failed=942`（passed=89）；select3 `failed=3209`（passed=142）
- 总计：`TOTAL: passed=1262 failed=4151 skipped=0`
- 首败：select2/select3 line=3 `CREATE TABLE t1` → `table t1 already exists`
- 对照：单文件 select2 / select3 → 各 exit **0**、failed=**0**（passed=1031 / 3351）
- `CliSession` 整会话共享 JDBC 连接；Plan T3 要求每文件 JDBC → run → **关闭连接** → **Fail**（DEF-CLI-001；Q8 零豁免）

### Spec / Plan 验收（轮次 2）

| ID | 要求 | 结果 | 证据 |
|---|---|---|---|
| P0-1 | 官方 `select1.test` → 失败数=0、退出码=0 | **Pass** | CLI exit 0；failed=0 |
| P1-1 | 目录递归 `.test`/`.slt`，分文件+总计 | **Pass** | `fixtures/cli/nested` → FILE `a.test`+`sub/b.slt`；`TOTAL: passed=6 failed=0`；exit 0 |
| P1-5 | 官方 select1/2/3 批量 → 失败数=0、退出码=0 | **Fail** | 批量 exit 1、failed=4151；单文件均 Pass（DEF-CLI-001） |
| P1-6 | `.slt` ≡ 同等 `.test` | **Pass** | `same-content.slt`/`.test` 均 `passed=3 failed=0`、exit 0 |
| `mvn -q clean test` | BUILD SUCCESS；Surefire 全过 | **Fail** | exit 1；Failures: 1（P1-5） |
| `mvn -q clean package` | 成功且可启动 `ggtest` | **Fail** | exit 1；`-DskipTests package` 仅用于产出本轮 CLI JAR，**不计** Plan Pass |
| 退出码 0/1/2 | fixtures 0/1/2 | **Pass** | `pass.test`→0；`fail.test`→1；`bad-parse.test`→2 |

### 回归

| 范围 | 结果 | 说明 |
|---|---|---|
| P1-1 / P1-6 / 退出码 smoke | Pass | 独立 `./bin/ggtest` |
| Surefire 其余用例 | Pass | 110 run 中仅 P1-5 硬验收失败 1；Skipped 1（Manifest） |
| 上游模块 | 无新增失败信号 | 失败限于 cli 批量硬验收路径 |

### 文档与安全（轮次 2）

| 项 | 结果 | 备注 |
|---|---|---|
| 用户/开发文档 | Pass（沿用） | 本轮未改文档；失败属实现 |
| 运维 N/A | Pass（N/A） | |
| 安全（语料只读、无入库、无新凭据泄漏） | 无新发现 | 安全不单独阻断；总体 Fail → **禁止**请求合并授权 |

### 缺陷（轮次 2）

| ID | 严重度 | 摘要 | 状态 | 处理说明 / 验证证据 |
|---|---|---|---|---|
| **DEF-CLI-001** | **高** | P1-5 批量官方 select1/2/3 失败数≠0（共享 JDBC 致跨文件库污染） | **open** | Spec P1-5/Q8：失败数=0、退出码=0、零豁免。证据：批量 exit=1、`failed=4151`；首败 `table t1 already exists`；单文件 select1/2/3 均 exit=0、failed=0。`CliSession` 共享连接 ↔ Plan T3 每文件关闭连接。**Developer 修复**：每文件独立连接/等价空白库；修复后 Review required 须重新 **Approve**，再 QA 回归（同报告追加）。禁止默示豁免。 |

### 阻塞

| 项 | 内容 |
|---|---|
| — | 无环境阻塞；轮次 1 语料阻塞已解除 |

### 结论（轮次 2）

- **总体：Fail**
- 分项：P0-1 / P1-1 / P1-6 **Pass**；**P1-5 Fail**（DEF-CLI-001）；`mvn test`/`package` **Fail**
- 缺陷：DEF-CLI-001 open；**不合并**；**禁止**请求合并授权
- 后续：Developer 修跨文件库隔离 → Reviewer 重新 Approve → QA 追加回归（P1-5 + `mvn test`/`package` + fixtures）
