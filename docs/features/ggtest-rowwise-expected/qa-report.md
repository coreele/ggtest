# QA Report: ggtest-rowwise-expected

> **工作项**：`ggtest-rowwise-expected`（未拆分）· **路径**：full · Review **required**（`review.md` **Approve** 2026-07-26 废止 R3 复审）  
> **分支**：`ggtest-rowwise-expected` → `main`  
> **Git**：本报告**未** commit；Pass 后仍停合并授权；Manager 与 STATUS/`done` 一次提交。合入须排除未跟踪 `examples/`（含 `demo2.slt`）与 `.env`。
>
> **轮次 1、轮次 2（含 R3 合同）因废止 R3 作废**，不得作为合入依据。本文件以**轮次 3**为准。

## 轮次

| 轮次 | 日期 | 实现版本 / 范围 | 环境 | 结论 |
|---|---|---|---|---|
| 1 | 2026-07-25 | 工作区相对 `HEAD`=`95173b0`（未 commit）；修订前合同 P0-1…P0-6 | macOS arm64；OpenJDK 17.0.20；Maven 3.9.16；SQLite `:memory:` | **Pass（已作废）** |
| 2 | 2026-07-26 | 工作区相对 `HEAD`=`95173b0`（未 commit）；R1/R2/**R3**；P0-1…P0-9 | 同上 | **Pass（已作废）** |
| **3** | **2026-07-26** | 工作区相对 `HEAD`=`95173b0`（**未 commit**）；现行 Spec：**已废止 R3**，保留 R1/R2；P0-1…P0-9 + P1-4 + Plan L3 | 同上 | **Pass** |

---

## 轮次 1（已作废）

修订前合同（文件级 separator / P0-1…P0-6）曾 Pass；后续合同变更后**作废**。

---

## 轮次 2（已作废）

含 R3（单引号/`''`/去引号）合同的重新验收曾 Pass；**废止 R3** 后**作废**。

---

## 轮次 3（废止 R3 后重新验收）

### 入口门禁

| 条件 | 证据 | 结果 |
|---|---|---|
| Plan 已用户确认并持久化 | 台账：废止 R3 用户决议；Plan 第四次修订可直接实施；状态经 `developing`→`reviewing`→`qa` | 满足 |
| Review required + Approve | `review.md` **Approve**（2026-07-26 废止 R3 复审；阻塞 none；N1–N3 非阻塞） | 满足 |
| 可验收实现 + Plan 验证 | 期望头本条 `S`/`explicit`；无 `SeparatorRecord`；expander 仅 `splitLiteral`+`strip`；fixtures 裸文本；README R1/R2 无引号壳 | 满足 |

### Spec P0 / Plan 验收

| ID | 要求 | 结果 | 证据 |
|---|---|---|---|
| P0-1 | 默认空格行式：`----` + `1 2 3`；连续空格仍空 token | **Pass** | `ResultComparerTest.p0_1_*`；`rowwise-default-space.test`；CLI `[PASSED]` exit 0 |
| P0-2 | 目标书写 IIT + `---- separator \|` + **无引号** `1 \| 1 \| hello world` | **Pass** | parser `p0_2_targetWriting_*`；comparer `p0_2_*`（expectedView=`1`,`1`,`hello world`）；`rowwise-pipe-separator.test` 第 11 行裸文本；CLI `[PASSED]` exit 0 |
| P0-3 | 单条作用域（R1）：下一条恰 `----` 不继承 `\|` | **Pass** | parser/runner `p0_3_*`；`rowwise-mixed.test` 第三条默认空格；CLI mixed `[PASSED]` |
| P0-4 | 显式 trim（R2）：`1 \| 2 \| 3` | **Pass** | `ResultComparerTest.p0_4_*` |
| P0-5 | 单元格含当前 `S`：须换分隔符或每值一行；**不**接受引号包裹 | **Pass** | `p0_5_cellContainingSeparator_*`（`S=\|` 失败；换 `,` / 每值一行通过） |
| P0-6 | 每值一行不回归 | **Pass** | `p0_6_valuePerLine*`；`NormalizeAcceptanceTest`；`query-normalize-smoke.test` CLI `[PASSED]` |
| P0-7 | 哈希 `N`/MD5 口径不变 | **Pass** | `ValueNormalizer`/`ResultHasher` 相对 `HEAD` **无 diff**；`p0_7_*`；`rowwise-mixed` 哈希行 `c0710d6b4f15dfa88f600b0e6b624077` |
| P0-8 | rowsort/nosort 可区分；先行再展开；失败 Diff | **Pass** | `ResultComparerTest.p0_8_*`（rowsort 过 / nosort 失败且 Diff 非空） |
| P0-9 | 受控 fixtures 期望头；无文件顶全局；禁 demo2/`.env`；无引号壳 fixture | **Pass** | 三件 `rowwise-*.test` 首条均为 `statement ok`；`RunnerAcceptanceTest.p0_*_rowWise*`；`src/` 无 `demo2` 依赖 |

### 负例 / P1 / 废止 R3 核对

| 项 | 要求 | 结果 | 证据 |
|---|---|---|---|
| P1-4 字面引号 | 期望 `'hello world'` 计入原文；**不得**因去引号通过 | **Pass** | `p1_4_literalQuotesAreCellContent_notUnquoted`：failed；expectedView 含 `'` |
| 无引号 API 残留 | 无 `splitLiteralRespectingQuotes` / `unquote` | **Pass** | `src/main` 无该方法名；`tokenize` 显式仅 `splitLiteral`→`strip` |
| 顶层 `---- separator` | 非法可读失败 | **Pass** | parser 负例；CLI → `must be a query expectation header`；exit 2 |
| 空 delim / 非法 `----…` / `seperator` / 三短横 / 裸 | 可读失败或不生效 | **Pass** | parser 负例；`src/main` 无 `---separator` 正例；无 `SeparatorRecord.java` |
| R1 | 本条绑定；无文件级主路径 | **Pass** | `QueryRecord`；runner 本条传参；fixtures 无文件顶 separator |
| R2 | 显式才 trim；默认连续空格仍空 token | **Pass** | `p0_4_*`；`p0_1_consecutiveSpaces*` |

缺口（不阻塞，同 review）：无专用行式 `valuesort` 单测（P0-8「及按需」）。

### 环境与命令（Plan L3）

| 命令 | 结果 | 摘要 |
|---|---|---|
| `mvn -q clean test`（及非 quiet 复核） | **Pass** | Tests run: **196**，Failures: **0**，Errors: **0**，Skipped: **17**；BUILD SUCCESS |
| `mvn -q -DskipTests package` | **Pass** | BUILD SUCCESS；`target/ggtest-0.1.0-SNAPSHOT.jar` |

CLI 冒烟（同上 jar；`--engine sqlite --url jdbc:sqlite::memory: --env-file <empty> --color never`）：三件 `rowwise-*.test`、`query-normalize-smoke.test` 均 `[PASSED]` exit 0；顶层 separator 负例 exit 2 且 `[WHY] parse error` 可读。

### 文档 / 安全

| 项 | 结果 |
|---|---|
| README「Expected results」（本条期望头、无引号目标书写、显式 trim、含 `S` 换分隔符/每值一行、顶层非法） | **Pass** |
| `dev-notes.md` L3 / P0-1…P0-9 / 删引号回改 | **Pass** |
| 运维文档 | N/A（Plan） |
| 安全 | **Pass**；认证/敏感数据 N/A；无安全发现；合入须排除 `demo2.slt`（未跟踪）与 `.env`（已 ignore、未跟踪） |

### 缺陷

无。

### 结论（轮次 3）

- **Pass** — 现行 Spec（无引号；R1/R2）P0-1…P0-9 与 Plan L3 全部通过；P1-4 与无引号 API 残留已核实；缺陷 none；关键证据无缺口
- 恢复条件：N/A
- 合并：待用户授权（**不** commit / **不** merge；本报告留工作区；轮次 1/2 **已作废**）
- 后续：Manager 可请求 **merge-auth**（仍停授权直至用户明确）；授权后 STATUS→`done` 与 `review.md`/`qa-report.md` 一次提交，再合入 `main`（排除 demo2/`.env`）
