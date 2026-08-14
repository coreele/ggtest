# Design: refactor-filerunner-responsibilities

## 背景与约束

CA-010: `FileRunner`（210 行）同时承担连接管理、引擎路由（sqlite/postgres）、PG schema 隔离、override 写回编排、parser 编排、sanitize。虽已从 `CliSession` 拆出，仍为多职责单类。

约束：
- 不改变 CLI 外部行为（exit code、报告格式、文件处理顺序）
- 不改变 `OverrideWriter`、`SqlLogicTestRunner`、`ResultComparer` 等已有类的 API
- 拆分出的新类均为 package-private（`cli` 包内），不扩大公开 API 面
- 现有测试必须全部通过

## 方案对比与决策

| 方案 | 概要 | 优点 | 缺点 | 比较依据 |
|---|---|---|---|---|
| A: 最小拆分 | 仅抽 `ConnectionFactory` + `OverrideCoordinator`，引擎分支保留在 FileRunner | 改动最小，风险最低 | FileRunner 仍含引擎条件分支，未根本解决上帝对象问题 | 安全但不够彻底 |
| B: 引擎策略 | 抽 `ConnectionFactory` + `EngineAdapter` 接口 + `OverrideCoordinator`，FileRunner 仅编排 | 彻底解耦引擎差异，新增引擎仅需增加 Adapter | 多出接口+2 实现，代码总量略增 | **选中** — 职责清晰、扩展友好 |
| C: 全多态 | FileRunner 自身多态化（SqliteFileRunner / PostgresFileRunner） | 引擎路径完全独立 | 大量重复代码，override 逻辑重复 | 过度设计 |

**决策:** 方案 B。`EngineAdapter` 接口封装引擎的 prepare/executor/teardown 生命周期；`ConnectionFactory` 封装 JDBC 连接创建；`OverrideCoordinator` 封装 override 收集与写回。`FileRunner` 退化为纯编排器。

## 模块边界与分层

```
cli/
  FileRunner.java         → 纯编排：parse → connection → adapter → override
  ConnectionFactory.java  → [新] JDBC 连接创建, package-private
  EngineAdapter.java      → [新] 接口: prepare / createExecutor / teardown, package-private
  SqliteAdapter.java      → [新] EngineAdapter 实现, package-private
  PostgresAdapter.java    → [新] EngineAdapter 实现, package-private
  OverrideCoordinator.java→ [新] override 收集 + 写回, package-private
  (现有类不变)             CliOptions, OverrideWriter, ReportWriter, ...
```

依赖方向（无循环）：
```
CliSession → FileRunner → ConnectionFactory, EngineAdapter, OverrideCoordinator
                         → SqlLogicTestParser
                         → SqlLogicTestRunner
PostgresAdapter → PostgresSchemaIsolation, PostgresJdbcExecutor
SqliteAdapter   → SqliteJdbcExecutor
```

## 模块影响

| 模块 | 变更 | 说明 |
|---|---|---|
| `cli/FileRunner.java` | 重构 | 移除连接管理、引擎分支、override 逻辑；保留编排骨架 |
| `cli/ConnectionFactory.java` | 新增 | 从 `FileRunner.openConnection` 迁出 |
| `cli/EngineAdapter.java` | 新增 | 接口，定义引擎生命周期 |
| `cli/SqliteAdapter.java` | 新增 | SQLite 零操作 prepare/teardown |
| `cli/PostgresAdapter.java` | 新增 | PG schema 隔离 + teardown 错误处理 |
| `cli/OverrideCoordinator.java` | 新增 | `collectOverrides` + `applyOverrideWriteBack` |
| `cli/CliSession.java` | 小改 | `new FileRunner(...)` 构造参数变化 |
| 测试 `cli/FileRunnerTest.java` | 更新 | 适配新结构；必要时拆测试到新类 |

不碰：`OverrideWriter`、`SqlLogicTestRunner`、`ReportWriter`、`SqlLogicTestParser`、db 包下所有类。

## 风险

| 风险 | 影响 | 缓解 |
|---|---|---|
| PG teardown 异常传播路径变化 | teardown 失败可能导致错误报告格式不一致 | 保持现有 finally 语义，在 PostgresAdapter 中复制精确的 schema+teardown 逻辑；完善 FileRunnerTest 的 PG mock |
| 构造参数链变更 | CliSession、FileRunner 构造签名变化可能遗漏传参 | 编译期检查；FileRunner 保持 builder 或 record 风格传参 |
| 测试重构范围失控 | 改动类多可能导致测试改动量 > 预期 | FileRunnerTest 已有良好单元测试覆盖；新类初期可依赖 FileRunnerTest 间接覆盖，待稳定后补直接单测 |

## 对 Plan 与 Developer 的要点

### Plan

- 任务拆分顺序：ConnectionFactory → EngineAdapter + SqliteAdapter → PostgresAdapter → OverrideCoordinator → 重构 FileRunner → 更新 CliSession → 跑全量测试
- 每一步均需编译通过；ConnectionFactory/EngineAdapter/OverrideCoordinator 独立可测
- Review 门禁：standard 必须 Approve
- QA：全量 `mvn test` + PostgresCliIntegrationTest 通过

### Developer

- PostgresAdapter 的 teardown 异常处理须精确复制当前 `FileRunner.runPostgresFile` 的 finally 块逻辑（schema null 检查、teardown 异常覆盖 outcome）
- OverrideCoordinator 的 `writeBack` 返回 `FileOutcome` 或 null（null 表示无 write-back 错误，继续正常流程）
- `sanitize()` 方法保留在 FileRunner（或移入工具类但保持 package-private）；若 ConnectionFactory 创建连接时需输出错误消息，也调用 sanitize
