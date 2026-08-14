# Review: fix-ca018-search-path-validation

## 审阅范围

- 实现版本 / 提交: `95749aa`（源分支 `fix-ca018-search-path-validation`）
- 依据: [plan.md](plan.md)；fast 路径，无 Spec / Design

## 实现正确性

- `setSearchPath`（PG + MySQL）现于入口调 `SchemaNames.requireSafe(schema)`，与 `teardown` 对齐，CA-018 纵深防御缺口关闭：非法标识符在拼入 `SET search_path TO <schema>` / `USE <schema>` 前即抛出，不会到达 DDL。✓
- `prepare()` 用 `SchemaNames.generate()`（`ggtest_` + 小写 hex UUID），与原实现逐字等价；`teardown` 仍校验后 `DROP`。✓
- 去重（Low）：两类私有 `isSafeIdentifier` 删除，规则 `[a-z][a-z0-9_]*` 与名生成统一到 `SchemaNames`，消除单边漂移。✓
- 异常类型：`requireSafe` 抛 `IllegalArgumentException`（非 `SQLException`）。理由成立——base `com.ggtest.db` 包须 driver-agnostic（守护测试约束）；非法名为编程错误，unchecked 合理。isolation 类 `throws SQLException` 仍覆盖真实 SQL 操作。teardown 不安全名路径类型由 SQLException→IllegalArgumentException，属防御路径（prepare 生成名永不触发），无测试断言旧类型，可接受。✓
- 范围守纪律：仅触碰两个 isolation 类 + 新增 SchemaNames/测试。

## 测试有效性

- `SchemaNamesTest`（5，确定性、无 DB）：`generate` 50 次产出安全前缀名；`isSafe` 接受/拒绝矩阵覆盖 null/空/首字母数字/大写/SQL 元字符/空白/连字符；`requireSafe` 返回合法名、非法名抛 IllegalArgumentException。✓
- 架构守护 `RunnerDependencyIsolationTest.executorAbstractionStaysFreeOfJdbc` 仍 Pass（确认 SchemaNames 未引入 JDBC 引用，含注释扫描）。✓
- DB 门控测试（PG/MySQL isolation）签名未变，无 DB 时跳过；prepare/teardown 生命周期不回归。✓
- 验证：`SchemaNamesTest` 5/0；`mvn clean test` 365/0/0（34 既有 skip）。

## 文档影响核对

| Plan 声明 | 实现是否一致 | 备注 |
|---|---|---|
| 开发文档 N/A | 一致 | Javadoc 已注明 requireSafe 前置条件 |
| 用户文档 N/A | 一致 | 无对外行为变化 |
| 运维文档 N/A | 一致 | — |

## 安全影响核对

| 检查项 | 结果 | 处置状态 | 备注 |
|---|---|---|---|
| 敏感信息 | 无 | n/a | — |
| 认证与授权 | 无 | n/a | — |
| 输入与外部访问 | 改善 | 已闭环 | setSearchPath 不再无条件拼接外部 schema 名（SQL 注入面收窄；调用方现仍仅传 prepare 生成名） |
| 依赖变更 | 无 | n/a | — |

## 必修项

| ID | 位置 | 问题 | 状态 |
|---|---|---|---|
| — | — | 无阻塞项 | — |

## 结论

Approve

## 后续动作与复审范围

- 进 QA：复跑 `SchemaNamesTest` + `mvn clean test`（重点架构守护）。
- QA Fail 修复后须复审；范围限 `SchemaNames` 及两个 isolation 类。
