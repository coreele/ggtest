# Review: xugu-engine

## 审阅范围

- 实现提交：`e3a8847` feat(xugu): add XuguDB engine (--engine xugu, alias xugudb)
- 登记提交：`dbc0b99`（register+spec）、`1e444b3`（design+plan）
- 依据：`spec.md` v1.0（用户确认）+ `design.md` v1.0 + `plan.md`；合同继承架构现状
- 触碰：2 个 db 新类、`FileRunner`/`RuntimeConfigResolver`/`Main`、`pom.xml`/`.gitignore`/`scripts`、4 个测试类 + fixtures、README

## 实现正确性

### Spec 合同对照

| 合同 | 实现 | 结论 |
|---|---|---|
| `--engine xugu`/`xugudb` 归一为 `xugu` | `RuntimeConfigResolver.normalizeEngine`：`xugudb`→`xugu`；允许值 `sqlite/postgres/xugu` | Pass |
| URL 须 `jdbc:xugu:` 前缀 | `validateEngineUrlPair` 加 xugu 分支 | Pass |
| 驱动 SPI 注册 + shade 合并 | `pom.xml` compile-scope + `ServicesResourceTransformer`；实测 uber-jar SPI 三行 | Pass |
| executor 继承 `AbstractJdbcExecutor`，不覆盖 execute*，不关连接 | `XuguJdbcExecutor` 仅 engineName+标记+展示名 | Pass |
| 隔离 prepare/teardown/setSearchPath | `XuguSchemaIsolation`：CREATE SCHEMA + SET SCHEMA / DROP SCHEMA CASCADE / SET SCHEMA；`isSafeIdentifier` | Pass |
| `FileRunner` 默认连接 prepare、命名连接 setSearchPath、finally teardown+关全部 | `needsIsolation=isPostgres\|\|isXugu` 统一路径，按引擎分派 | Pass |
| `skipif`/`onlyif` 以 `xugu` 匹配 | engineName()=`"xugu"`；`skipif xugu` 生效（CLI 测试验证） | Pass |
| NULL 经 getString+wasNull | 沿用 `AbstractJdbcExecutor.readRows`（executor 测试 NULL 用例验证） | Pass |
| `--parallel`/`--halt`/`conn=<name>` 正交 | CLI 集成测试覆盖并行隔离/多连接/halt；隔离使并行安全 | Pass |
| 凭据脱敏 | 沿用 `FileOutcome.detailLines()` + `CredentialRedaction`；脱敏测试（真实+不可达）通过 | Pass |
| `GGTEST_XG_*` 仅测试门、不入运行时白名单 | resolver 不读取（`doesNotReadGgtestXgGateKeysAsRuntimeConfig` 用例验证）；`DotEnvLoader` 未改 | Pass |
| 退出码优先级不变 | 未改 `CliSession`；executor/CLI 测试验证 0/1/2 | Pass |

### 关键路径检查

| 检查项 | 结果 | 说明 |
|---|---|---|
| 零下游回归 | Pass | `AbstractJdbcExecutor`/`ConnectionFactory`/runner/parser/normalize 未改 |
| sqlite/postgres/顺序/并行/halt 零回归 | Pass | `mvn test` 全量 367/0（无服务 35 skip、有服务 17 skip） |
| uber-jar 含驱动 + SPI | Pass | `jar tf` 含 `Driver.class`；SPI 三行；`java -jar --engine xugu` E2E PASS |
| 抽象守卫（runner/db 不直接依赖具体引擎/驱动） | Pass | `RunnerDependencyIsolationTest` 已更新豁免 `xugu` 子包 + 禁引 `com.xugu`/`com.ggtest.db.xugu`（runner） |
| `conn=<name>` 多连接 | Pass | `multi-conn.test` 通过；命名连接经 `setSearchPath` 指向同文件 schema |

### 观察（非阻塞）

1. **`DROP SCHEMA` 无 `IF EXISTS`（虚谷方言）**：`teardown` 用 `DROP SCHEMA <name> CASCADE`。安全性由 `prepare` 总先创建保证（生命周期内 schema 必存在）；`isSafeIdentifier` 防 teardown 注入。dev-notes 已记录。非阻塞。
2. **`checker-qual` 非运行时依赖**：实测驱动可无 checker-qual 加载；pom 仅声明 xugu-jdbc（compile）。如未来驱动版本改为运行时依赖 checker-qual，需补声明。非阻塞。
3. **`SET SCHEMA` 后系统目录可达**：无需 PG 的 `, pg_catalog` 回退（实测）。非阻塞。
4. **致命标记含中文**：源自 `ErrorCode.txt`（E50020/E50022）；`AbstractJdbcExecutor` 主探测（SQLState 08* / isClosed）兜底，标记为补充。非阻塞。

## 测试有效性

### 覆盖（Spec P0/P1）

| Spec | 测试 | 结果 |
|---|---|---|
| P0-1 引擎解析/归一化 | `RuntimeConfigResolverTest`（allowsXugu/xugudbAlias） | Pass |
| P0-2 URL 校验 | `xuguEngineUrlMismatchYieldsUsageError` | Pass |
| P0-3 单文件 E2E | `xuguEngineRunsBasicFixture` + V8 uber-jar | Pass |
| P0-4 断言失败报告 | `xuguEngineReportsAssertionFailure` | Pass |
| P0-5 NULL | `XuguJdbcExecutorTest.queryReturnsRawValuesAndSqlNullAsNull` | Pass |
| P0-6 skipif xugu | `skipifAndOnlyifRespectXuguEngineCaseInsensitively` | Pass |
| P0-7 凭据脱敏 | `nonEmptyPasswordNeverPrintedWhenXuguConnectionFails`（非门控） | Pass |
| P0-8 零回归 | `mvn test` 全量 | Pass |
| P1-1 跨文件隔离 | `crossFileSchemaIsolationKeepsSameNamedTablesIndependent` + `XuguSchemaIsolationTest` | Pass |
| P1-2 并行隔离 | `parallelXuguSchemaIsolation` | Pass |
| P1-3 多连接 | `namedConnectionsAreIsolatedToSameFileSchema` | Pass |
| P1-4 halt 正交 | `haltStopsAfterFirstFailingFile` | Pass |

### 复现验证

```
有服务（127.0.0.1:5138/SYSTEM, SYSDBA/SYSDBA）:
  mvn test → 367, 0 failures, 0 errors, 17 skipped (PG 门控)
无服务:
  mvn test → 367, 0 failures, 0 errors, 35 skipped (PG+XG 门控)
java -jar target/ggtest-*.jar --engine xugu --url jdbc:xugu://127.0.0.1:5138/SYSTEM?char_set=utf8 \
  --user SYSDBA --password SYSDBA fixtures/xg/basic.test → [PASSED] exit0
```

## 文档影响核对

| Plan 声明 | 实现 | 备注 |
|---|---|---|
| 开发文档 | 一致 | README：`driver/` bootstrap、引擎/隔离表加 XuguDB、`GGTEST_XG_*` 段、校正「加引擎」表述 |
| 用户文档 | 一致 | README：`--engine xugu`（别名 xugudb）、示例、`--help` 行 |
| 运维文档 | N/A | 无部署/排障变更 |

## 安全影响核对

| 检查项 | 结果 | 备注 |
|---|---|---|
| 凭据脱敏 | Pass | 沿用既有；真实+不可达 URL 脱敏用例通过 |
| 驱动为专有 | Pass | `driver/` gitignore，不入库；bootstrap 文档化 |
| 依赖变更 | Pass | 新增 `com.xugudb:xugu-jdbc`（本地 file repo）；`checker-qual` 非运行时 |
| 输入校验 | Pass | `--engine`/URL 校验既有路径扩展 |
| 隔离标识符注入 | Pass | `isSafeIdentifier` 守卫 teardown |

## 必修项

无阻塞项。

## 结论

**Approve**

## 后续动作与复审范围

- QA 按 plan V1-V10 验收（有服务环境复测集成项 V2/V3/V5/V8；无服务环境验证 V7 零回归）。
- Manager 收到 QA Pass 后获取用户合并授权；目标分支 `xgtest`（已在该分支实现），`done` + 未入库 `review.md`/`qa-report.md` 一次提交；`xgtest`→`main` 由用户自行决定。

---

## 复审（round 2）——代码审计后

- 触发：用户要求在合并授权前执行一次代码审计 + 复审。
- 审计报告：`workflow/docs/audit/2026-08-12-xugu-engine.md`（范围：本次变更的全部生产源文件）。
- 审计结论：Findings — Critical 0 / High 0 / Medium 0 / Low 3 / Info 1。无阻塞项。

### 审计 Findings 对照

| Finding | 级别 | 本切片是否引入 | 复审结论 |
|---|---|---|---|
| `FileRunner:67-91` prepare 失败连接泄漏 | Low §5 | **否**（PG 已有同款模式；本切片仅扩展 `needsIsolation` 使之同效） | 登记 CA-017；不建议在本切片内修复（引入改 PG 的风险，范围蔓延）。后续独立工作项一次性修 PG+Xugu |
| `XuguSchemaIsolation:66-71` `setSearchPath` 未校验 `isSafeIdentifier`（与 `teardown` 不一致） | Low §2 | **否**（`PostgresSchemaIsolation.setSearchPath` 同款） | 可接受——调用方始终传入 `prepare` 生成的 UUID 名；与 PG 一致 |
| `FileRunner:72-74` prepare 使用三元 `isPostgres ? ... : Xugu...`（隐式 fallback） | Low §4 | **是** | 可改为显式 `if/else`（与工厂一致），非阻塞。建议本切片修复：工作量 2 行 |
| `validateEngineUrlPair` xugu 分支无 `return;`（风格不一致） | Info | 镜像 PG 原有模式 | 可接受 |
| CA-017 登记建议 | — | — | 已追加入 `code-audit-register.md` |

### 复审结论

**Approve**（复审 round 2）。审计无阻塞项；CA-017 为 pre-existing 模式，不入本切片修复范围。建议开发者可顺手将 FileRunner:72-74 的三元改为显式 `if/else`（与工厂一致，2 行改动），非强制。

QA 结论不变（第 1 轮 Pass）；可进入合并授权。
