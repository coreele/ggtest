# Design: ggtest-core / runner-sqlite

> 架构决策（Plan 之前完成）。仅处理模块边界、分层与技术选型；需求合同与验收见 [`spec.md`](./spec.md)。
>
> **feature-id**：`ggtest-core` · **sub-feature-id**：`runner-sqlite`
> **适用对象**：Planner（Plan 输入）、Developer（实现依据）、Reviewer（结构审阅）。
> **前置条件**：已读 [`spec.md`](./spec.md)、总览 [`../spec.md`](../spec.md)、工作项记录 [`ggtest-core.md`](../../../manager/ggtest-core.md)；了解上游 `com.ggtest.model` / `com.ggtest.normalize`。
> **阅读顺序**：背景 → 模块边界与分层 → 执行器契约形状 → Runner 形状 → 方案对比 → 模块影响 → 风险 → 对 Plan/Developer 要点。
> **预期结果**：读者掌握 Runner / 执行器抽象 / SQLite JDBC 适配的包边界与依赖方向，可据此编写 Plan 与实现。
> **失败处理**：发现 Spec 缺失合同信息时停止并报告 Manager，不在本文件替代 Spec 决策。

## 背景

- 上游已合入：`com.ggtest.model` 记录模型；`ResultComparer` 比对（无 JDBC）。
- 本切片：Runner + 执行器抽象 + SQLite JDBC；产出可供 `cli-corpus` 消费的逐记录结果。
- 约束（勿重开）：Java 17、Maven、engine=`sqlite`（大小写不敏感）、单连接串行、halt→skipped（Q6）、不引入 Q8 默示豁免、不交付 CLI/官方语料硬验收。

## 模块边界与分层

依赖单向向内；Runner **不得**依赖 SQLite 具体包。

```text
com.ggtest.runner  ──▶  com.ggtest.db（接口）
        │                      ▲
        ├──▶ com.ggtest.model  │
        └──▶ com.ggtest.normalize
                               │
com.ggtest.db.sqlite  ─────────┘
        └──▶ org.xerial.sqlite-jdbc
```

| 包 | 职责 | 禁止 |
|---|---|---|
| `com.ggtest.db` | 执行器抽象（引擎名、语句、查询行列原始值）；致命连接错误类型 | JDBC 驱动、SQLite 类型、Runner 编排 |
| `com.ggtest.db.sqlite` | SQLite JDBC 实现与值抽取 | 解析、normalize、条件/halt/label 编排 |
| `com.ggtest.runner` | 单文件状态机：条件、halt、hash-threshold、statement/query、label；调用 normalize | `import com.ggtest.db.sqlite`；直接使用 `java.sql` |

- **P0-8**：`runner` 仅依赖 `com.ggtest.db`；换库只加适配器，不改 `parser`/`runner`。
- **连接所有权**：调用方创建/关闭连接；执行器持有已打开连接；工具不负责库初始化/清理。

## 执行器契约形状（签名级）

`com.ggtest.db.DatabaseExecutor`（以实现为准，行为对齐 Spec 合同）：

- `String engineName()` — 首期 `"sqlite"`；供 skipif/onlyif 匹配。
- `StatementResult executeStatement(String sql)` — 业务 SQL 失败进结果对象；连接不可用抛致命异常。
- `QueryResult executeQuery(String sql)` — 成功返回 `List<List<String>>`（`null` 元素 = SQL NULL）；业务失败进结果对象；连接类失败抛致命异常。

同包附属类型：

- `StatementResult`：`succeeded()`；可选错误摘要（**禁止**用于首期 message/正则匹配）。
- `QueryResult`：成功标志 + `rows`（失败时为空）+ 可选错误摘要。
- `FatalDatabaseException`（非受检）：连接失败/中断；Runner 中止当前文件（退出码 2 映射属 `cli-corpus`）。

**SQLite 值抽取**：`wasNull()` → `null`；否则 `getString`；执行器内不做 I/T/R 规范化。

## Runner 形状（签名级）

`com.ggtest.runner.SqlLogicTestRunner`：

- 入参：有序 `List<SqlTestRecord>`、`DatabaseExecutor`、初始 `hashThreshold`（可复用 `ResultComparer.DEFAULT_HASH_THRESHOLD` = 8）。
- 出参：逐记录结果 + 文件级汇总（pass/fail/skipped 与失败素材）。
- 单文件状态：当前 hash-threshold；待生效 skipif/onlyif（作用于下一条**非条件**记录后清空）；label → 首次结果视图。

编排结构：

- `SkipIf`/`OnlyIf` 只累积；求值相对 `engineName()`（大小写不敏感）。
- `HashThreshold`：未跳过则更新 threshold。
- `Halt`：未跳过则停止；其后记录 skipped（Q6）。
- `Statement`：按 OK/ERROR 断言；单条失败不中止文件。
- `Query`：无期望仅断言成功；有期望走 `ResultComparer`；同 label 用比对视图（含哈希形态）一致性检查。
- `FatalDatabaseException`：中止文件，保留已产出结果。

## 方案对比与决策

**决策 1：执行器接口放置**

| 方案 | 概要 | 优点 | 缺点 |
|---|---|---|---|
| A（选定） | 接口 `com.ggtest.db`；实现 `com.ggtest.db.sqlite` | P0-8 可审查 import；多库只加子包 | 包略多 |
| B | 接口放 `runner` | 包更少 | 编排与扩展点混包 |

**决策:** 选 A。

**决策 2：业务失败 vs 致命失败**

| 方案 | 概要 | 优点 | 缺点 |
|---|---|---|---|
| A（选定） | 业务失败 → 结果对象；连接类 → `FatalDatabaseException` | 对齐 Spec 继续/中止语义 | 需分类 SQLException |
| B | 一律抛异常再由 Runner 分类 | 接口更瘦 | 易误中止 |

**决策:** 选 A。致命至少含：连不上、连接已关闭、链路中断；普通 SQL 错误为业务失败。

**决策 3：JDBC 驱动**

| 方案 | 概要 | 优点 | 缺点 |
|---|---|---|---|
| A（选定） | `org.xerial:sqlite-jdbc`（建议钉 **3.53.2.0**） | 事实标准；`jdbc:sqlite::memory:` 可验收 | 与原生 CLI 可能有 JDBC 层差异（Q8 不在本切片豁免） |
| B | 其他驱动/包装 | — | 偏离既定路径 |

**决策:** 选 A。凭据不得入日志/报告。

**决策 4：测试替身**

| 方案 | 概要 | 优点 | 缺点 |
|---|---|---|---|
| A（选定） | 假执行器测编排；真 `SqliteJdbcExecutor`+内存库测 P0-3 等 | 编排与驱动解耦 | 两套测试 |
| B | 全部真 SQLite | 替身少 | 编排与驱动耦合 |

**决策:** 选 A。P0-8：Reviewer 依赖检查 + 假执行器驱动 Runner。

## 模块影响

- `pom.xml` 增加 `org.xerial:sqlite-jdbc`（compile）。
- 新增 `com.ggtest.db`、`com.ggtest.db.sqlite`、`com.ggtest.runner`。
- 复用且不改行为：`model`、`parser`、`normalize`。
- 下游：`Connection` → `SqliteJdbcExecutor` → `SqlLogicTestRunner` → CLI 统计/退出码。

## 风险

| 风险 | 影响 | 缓解 |
|---|---|---|
| JDBC 与原生结果差异 | 后续语料硬验收失败 | 本切片不豁免；原始 `getString`/`null`；差异走 cli-corpus/Q8 |
| 致命/业务分类不准 | 误中止或该中止未中止 | 分类规则 + 关连接用例；Review |
| Runner 依赖 sqlite 包 | P0-8 失败 | 禁止 import；Reviewer 检查 |
| label 视图不一致 | P1-4 误判 | 与 `ResultComparer` 同一视图管线 |
| 驱动拉取失败 | 无法构建 | 钉版本；代理 `127.0.0.1:7890`；多次失败则停 |

## 对 Plan 与 Developer 的要点

### Plan

- 顺序：接口 → Runner 状态机（假执行器）→ SQLite 适配 → 验收（P0-3/P0-6/P0-8/P1-2/P1-4）→ 文档。
- 验证含内存 SQLite JDBC；建议分支 `ggtest-core-runner-sqlite`。
- README 补 Runner/执行器用法；用户/运维文档 N/A。

### Developer

- TDD；`runner` 禁止依赖 `db.sqlite` 与直接 `java.sql`。
- `statement error` 只断言失败事实；单条失败继续（halt/致命除外）。
- 不实现 CLI、目录收集、退出码、官方语料硬验收。
