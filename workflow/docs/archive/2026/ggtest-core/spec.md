# Spec: ggtest-core（总览）

> 需求与规格总览。本文件为索引，**不含**详细合同与 GWT 验收；不对巨型 Spec 做整包 Plan。
>
> **feature-id**：`ggtest-core` · **sub-feature-id**：`ggtest-core`（与 feature-id 相同表示总览行）
> **适用对象**：Manager（进度跟踪）、Planner/Developer/QA（定位子 Spec）、用户（确认产品边界与已决议）。
> **前置条件**：已阅读工作项记录 `workflow/workflow/docs/manager/ggtest-core.md`；了解 sqllogictest 格式基本概念。
> **阅读顺序**：背景与目标 → 非目标摘要 → 子 Spec 职责与依赖 → 已决议 → 开放问题。
> **预期结果**：读者能把握产品边界与切片切分，并跳转到对应子目录下的 `spec.md` 查看合同与验收。
>
> **确认要求（full 路径）**：本总览仅作索引；**各子 Spec 须分别由当前用户会话确认**后方可进入各自 Design/Plan。不对总览行调度 Planner。

## 背景与目标

- sqllogictest 是 SQLite 官方的 SQL 正确性测试格式，拥有 700 万+ 条测试语料，采用行式文本协议描述「执行语句/查询并断言结果」的测试脚本（常见扩展名 `.test` / `.slt`）。
- 现有 Java 实现 hydromatic/sql-logic-test 已停滞；Rust 实现 sqllogictest-rs 是事实标杆但不适用于 JVM 生态。
- **目标**：从零到一实现 **GGTEST**——一个 **Java** 语言的 sqllogictest 测试工具，包含 parser（解析输入）与 runner（对目标数据库执行并比对），架构上支持多数据库扩展。
- **首期交付**：仅支持 **SQLite（经 JDBC）**，实现格式核心功能，能够跑通官方测试语料并输出通过/失败/跳过统计。
- **成熟度指标**：官方语料在 SQLite（JDBC）上硬验收——失败数为 0、退出码为 0（详见 `cli-corpus`：P0-1 / P1-5）。零豁免为默认立场（已决议 Q8）。

## 非目标摘要

以下明确**不在首期范围**（架构可预留，但不实现、不验收）：

- 其他数据库支持（PostgreSQL、MySQL、DuckDB 等）；首期仅 SQLite（JDBC）。
- sqllogictest-rs 扩展：`statement error` 错误消息/正则匹配；变量替换；record/complete 模式；JUnit XML；多文件并行 / 多连接并发。
- 语料生成或维护（用户自备本地路径）；图形界面、守护进程、CI/CD 集成；发布到 Maven Central。
- SQL 方言转换或改写（脚本 SQL 原样发送）。

## 子 Spec 职责与依赖

| 顺序 | sub-feature-id | Spec 路径 | 职责摘要 | 验收对齐（原编号） |
|---|---|---|---|---|
| — | ggtest-core | [`spec.md`](./spec.md) | 本总览（根目录仅保留总览 Spec） | N/A |
| 1 | parser | [`ggtest-core-parser/spec.md`](./ggtest-core-parser/spec.md) | 解析 `.test`/`.slt`/单文件 → 记录模型；解析错误含文件+行号；**不连库** | P0-7 |
| 2 | normalize | [`ggtest-core-normalize/spec.md`](./ggtest-core-normalize/spec.md) | I/T/R 规范化、排序、MD5 兼容、hash-threshold；可用固定样例 | P0-2、P0-4、P0-5、P1-3 |
| 3 | runner-sqlite | [`ggtest-core-runner-sqlite/spec.md`](./ggtest-core-runner-sqlite/spec.md) | Runner + 执行器抽象 + SQLite JDBC；skipif/onlyif/halt/label/statement/query | P0-3、P0-6、P0-8、P1-2、P1-4 |
| 4 | cli-corpus | [`ggtest-core-cli-corpus/spec.md`](./ggtest-core-cli-corpus/spec.md) | CLI `ggtest`、统计、退出码、目录收集、官方语料硬验收 | P0-1、P1-1、P1-5、P1-6 |

**依赖顺序**：`parser` ∥ `normalize` → `runner-sqlite` → `cli-corpus`。

**详细合同与 GWT 验收**仅在各子目录 `spec.md` 中维护；本总览不重复、不作整包 Plan。

## 合同

### API / 接口

N/A（本总览无独立对外接口；CLI 见 `cli-corpus`；解析/规范化/执行见对应子 Spec）。

### 数据 / 状态

N/A（见各子 Spec）。

### 错误与约束

N/A（见各子 Spec）。全局已决议约束见下表，各子 Spec 继承、**勿重开**。

## 验收（Given-When-Then）

N/A。本总览不设独立 P0/P1；可验证验收分布在四个子 Spec。

## 开放问题

### 已决议（勿重开）

| 编号 | 议题 | 结论 |
|---|---|---|
| Q1 | Java 最低版本 | **Java 17** |
| Q2 | 构建工具 | **Maven** |
| Q3 | CLI 优先还是库优先 | **CLI 优先**；库形态作为自然产物存在但首期不承诺稳定公共 API、不发布制品 |
| Q4 | 官方语料获取方式 | **用户自备语料路径**（CLI 传入）；仓库内只随代码附带少量自造测试文件用于自测；不在仓库内提交官方大语料 |
| Q5 | hash-threshold 默认值 | **8**（跟随原始 SQLite 实现；可被 CLI 与文件内指令覆盖） |
| Q6 | halt 后记录的统计口径 | 计为 **skipped** |
| Q7 | 官方语料验收口径 | 官方语料在 SQLite（JDBC）上硬验收：**失败数 = 0**、退出码 = 0。P0-1 以 `select1.test` 为准；P1-5 以 `select1.test`、`select2.test`、`select3.test` 为准。 |
| Q8 | JDBC 与原生 SQLite 的结果差异 | 坚持 JDBC 路径下「**零豁免硬验收**」。若实测出现不可消除的 JDBC 层偏差，须再单独列为已知豁免并经用户批准——**不可默示豁免**。 |
| Q9 | P1-5 语料范围 | **select1.test、select2.test、select3.test** 为硬验收范围；其余语料不作首期硬指标。 |

**附加已决议（与上表同等效力，勿重开）：**

- 产品/项目名：**GGTEST**；工作项 feature-id：`ggtest-core`。
- 首期目标库：**SQLite（JDBC）**；其他库为首期非目标，扩展点保留。
- 输入后缀：`.slt` 与 `.test` **等价**；目录递归收集 `*.test` 与 `*.slt`；单文件路径不强制扩展名。

### 待确认

无（总览级）。各子 Spec 分别确认。
