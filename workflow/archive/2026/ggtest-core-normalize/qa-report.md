# QA Report: ggtest-core / normalize

> **工作项**：`ggtest-core` · **sub-feature-id**：`normalize`  
> **路径**：full · **Review 门禁**：required（入口前已 Approve）  
> **Design**：skipped（无 design.md）  
> **源分支** → **目标分支**：`ggtest-core-normalize` → `main`

## 轮次

| 轮次 | 日期 | 实现版本 / 范围 | 环境 | 结论 |
|---|---|---|---|---|
| 1 | 2026-07-25 | `07a8e51`；Plan T1–T6；P0-2/P0-4/P0-5/P1-3；parser 回归 | macOS aarch64；JDK 17.0.20；Maven 3.9.16 | Pass |

## 入口门禁核验（轮次 1）

| 条件 | 证据 | 结果 |
|---|---|---|
| Plan 已用户确认并持久化 | `workflow/archive/2026/ggtest-core/ggtest-core.md`：`/manager plan ok` → approved | 满足 |
| Spec 已用户确认 | 同记录：Spec「ok」→ approved | 满足 |
| Review required 且 Approve | `review.md` 结论 Approve；无阻塞必修项 | 满足 |
| 可验收实现与 Plan 验证 | `com.ggtest.normalize` + 测试/fixtures；L2：`mvn -q clean test` | 满足 |

## 环境与命令（轮次 1）

- 工作区：`/Users/zhougangjie/Space/ggtest`；分支 `ggtest-core-normalize` @ `07a8e51`
- JDK：`JAVA_HOME=/opt/homebrew/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home`（17.0.20）
- Maven：`~/tools/apache-maven-3.9.16/bin/mvn`
- 命令：`mvn clean test`（Plan 声明 `mvn -q clean test`；本轮去 `-q` 取 Surefire 明细）→ 退出码 0 / BUILD SUCCESS
- Surefire：Tests run: **36**, Failures: **0**, Errors: **0**, Skipped: **0**（parser 10 + normalize 26）
- 独立 MD5（Python，值+`\n`）：30 个 fixture 值 → `3c13dee48d9356ae19af2515e05e6b54`

## 覆盖（对照 Spec 验收 + Plan 验证）

### Spec 验收

| ID | 要求 | 结果 | 证据 |
|---|---|---|---|
| P0-2 | 语料摘录规范化值 + hash-threshold → MD5 与 `N values hashing to <md5>` 一致 | 通过 | `p0_2_*` + `p0-2-select1-hash.txt`：30 I 值 → MD5 `3c13dee48d9356ae19af2515e05e6b54`；threshold 8 哈希比对 pass；Python 复核同值 |
| P0-4 | I/NULL、R/`%.3f`、T/`(empty)` 三条比对通过 | 通过 | `p0_4_*` 三用例 pass；`ValueNormalizerTest` 覆盖 `@` / 不可解析 I→`0` / R→`0.000` |
| P0-5 | 行序错位 → `rowsort` 通过、`nosort` 失败且含差异摘要 | 通过 | `p0_5_*`：ROWSORT pass；NOSORT fail；`diffSummary` 含 expected/actual |
| P1-3 | `valuesort` + 全值排序期望 → 通过 | 通过 | `p1_3_*`：期望 `1\n2\n3\n4` → pass |

### Plan 验证

| 项 | 要求 | 结果 | 证据 |
|---|---|---|---|
| 最低验证层 | L2 | 通过 | `mvn clean test` |
| 验证命令 | `mvn -q clean test`（Java 17） | 通过 | JDK 17；退出码 0 |
| 预期证据 | BUILD SUCCESS；Surefire 全过（含 parser） | 通过 | 36/36；Failures/Errors/Skipped: 0 |
| T1–T4 | I/T/R；三 SortMode；MD5；比对入口；默认 threshold 8；≤0 全量文本；失败差异摘要；无 JDBC | 通过 | `DEFAULT_HASH_THRESHOLD=8`；`thresholdZeroForcesFullTextEvenWhenManyValues`；`failureDiffSummaryIsNonEmpty` |
| T5 fixtures | `fixtures/normalize/` | 通过 | P0-2 已加载；P0-4 内联（见观察） |
| T6 开发文档 | README + Javadoc + `dev-notes.md` | 通过 | 见文档验收 |

### 回归

| 范围 | 结果 | 说明 |
|---|---|---|
| parser 10 | 通过 | 同次 Surefire；0 失败 |
| normalize 26 | 通过 | 验收 6 + 单元 20 |

### 文档验收

| 类别 | Plan 声明 | 结果 | 证据 |
|---|---|---|---|
| 开发文档 | README + Javadoc + `dev-notes.md` | 通过 | 前置条件、`mvn -q clean test`、`ResultComparer` 示例（threshold 8）、Javadoc、L2 证据 |
| 用户文档 | N/A | 通过（N/A） | 无 CLI |
| 运维文档 | N/A | 通过（N/A） | 无部署面 |

### 安全验收（`security.md`）

| 检查项 | 结果 | 备注 |
|---|---|---|
| 范围 | 内存规范化/排序/MD5；fixtures 只读 | 无认证/授权/网络/SQL/命令执行 |
| 敏感信息 | 无发现 | 代码/测试/fixtures/文档无凭据 |
| 输入与文件 | 可接受 | 不连库；classpath 只读；无写回 |
| 依赖 | 可接受 | 无新生产依赖 |
| 处置状态 | 无需处置 | 质量门允许请求合并授权；本轮不执行合并 |

## 缺陷

| ID | 严重度 | 摘要 | 状态 | 处理说明 / 验证证据 |
|---|---|---|---|---|
| — | — | 无 | — | — |

非阻塞观察（不阻断 Pass）：

- N1：`p0-4-normalize-cases.txt` 未加载（P0-4 已内联）；可选清理或改为加载
- N3：无 `valueCount == hashThreshold` 专项用例（Spec「超过」；实现 `size > threshold`）

## 结论（轮次 1）

- **总体：Pass**
- 恢复条件：N/A
- 合并：**不合并**（本调度禁止执行合并）
- 质量条件：Plan 确认 + Review Approve + QA Pass + 源/目标分支已声明 → 已满足请求用户完成/合并授权的前提
- 建议后续：Manager 请求用户授权 → 源分支置 `done` → Merge Executor 将 `ggtest-core-normalize` 合入 `main`；下游 `runner-sqlite` 待依赖就绪后推进
