# QA Report: xugu-engine

## 验收范围

- 验收对象：实现 `e3a8847`（分支 `xgtest`）
- 依据：`plan.md` V1-V10；`spec.md` P0/P1；`review.md` Approve
- 环境：本地 Linux；XuguDB 服务 `127.0.0.1:5138/SYSTEM`（SYSDBA/SYSDBA）可用；无 PG（PG 用例自动 skip）

## 第 1 轮：验收

| ID | 要求 | 命令 / 证据 | 结果 |
|---|---|---|---|
| V1 | 驱动可解析且入 uber-jar + SPI 合并 | `mvn package`；`jar tf target/ggtest-*.jar` 含 `com/xugu/cloudjdbc/Driver.class`；`META-INF/services/java.sql.Driver` 三行（sqlite+postgres+xugu） | Pass |
| V2 | executor 单元（门控） | `XuguJdbcExecutorTest` → 8/8 | Pass |
| V3 | 隔离单元（门控） | `XuguSchemaIsolationTest` → 1/1 | Pass |
| V4 | resolver（非门控） | `RuntimeConfigResolverTest` → 39/39（含 5 新增 xugu） | Pass |
| V5 | CLI 集成（门控） | `XuguCliIntegrationTest` → 10/10（basic/fail/skipif/xugudb 别名/跨文件/并行/多连接/halt/脱敏） | Pass |
| V6 | 脱敏（非门控） | `nonEmptyPasswordNeverPrintedWhenXuguConnectionFails`（不可达 `jdbc:xugu://127.0.0.1:1/SYSTEM` + secret）→ exit2、stdout/stderr 无 secret | Pass |
| V7 | 全量零回归 | 无服务 `mvn test` → 367/0/35skip（PG+XG 门控）；有服务 → 367/0/17skip（仅 PG 门控） | Pass |
| V8 | uber-jar 端到端 | `java -jar target/ggtest-*.jar --engine xugu ...`：basic.test → `[PASSED]`；fail+basic → passed=1 failed=1 | Pass |
| V9 | Review Approve | `review.md` Approve | Pass |
| V10 | QA 验收 | 本报告 | Pass |

### Spec 验收项映射

| Spec | 验证 ID |
|---|---|
| P0-1/2 引擎解析+URL | V4 |
| P0-3/4 单文件 E2E+失败 | V5/V8 |
| P0-5 NULL | V2 |
| P0-6 skipif xugu | V5 |
| P0-7 凭据脱敏 | V6 |
| P0-8 零回归 | V7 |
| P1-1/2 跨文件+并行隔离 | V3/V5 |
| P1-3 多连接 | V5 |
| P1-4 halt 正交 | V5 |
| P2-1 加速 | Q-Note（未量化） |
| P2-2 驱动文档 | README + scripts |

### 独立复测（QA 自跑）

- 无服务 `mvn test`：**BUILD SUCCESS**（0 失败，xugu 门控用例 skip）。
- 有服务 xugu 三套件：**Tests run: 19, Failures: 0, Errors: 0, Skipped: 0**。
- `java -jar ... --engine xugu ... fail.test basic.test`：`TOTAL: passed=1 failed=1`，fail.test `[FAILED]`、basic.test `[PASSED]`（与预期一致）。

## 缺陷

无。

## 验证缺口

| 项 | 原因 | 风险 | 恢复条件 |
|---|---|---|---|
| 大语料 wall-clock 加速（P2-1） | 无量化阈值 fixture | 低——逻辑正确性由 V5 并行隔离覆盖 | 可选准备大语料后 Q-Note |
| 非 SYSDBA 用户/非 SYSTEM 库的权限路径 | 仅以 SYSDBA/SYSTEM 验证 | 低——连接/隔离 SQL 不依赖特权假设（CREATE SCHEMA 在用户 schema 内） | 可选用普通用户复测 |

## 结论

**Pass**

建议合并授权后，Manager 在分支 `xgtest` 将 STATUS→`done` 与 `review.md`/`qa-report.md` 一次提交；`xgtest`→`main` 由用户自行决定。
