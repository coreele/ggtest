# Plan: fix-ca023-prepare-conn-leak

## 元信息

- 工作项标识: fix-ca023-prepare-conn-leak（未拆分）
- 路径等级: fast | Review 门禁: required
- 来源: `workflow/docs/audit/2026-08-12-xugu-engine.md` Findings（原记为 CA-017，因与 2026-08-13 源码审计 CA-017 撞号，登记册改编号为 **CA-023**）
- 验证命令: `mvn -q clean test`
- 预期证据: `mvn clean test` BUILD SUCCESS、0 failures（SQLite 全量 + 架构守护不回归）

## 目标摘要

修复 CA-023：隔离引擎（PG/MySQL；Xugu 在 xgtest 分支同款模式）`prepare` 失败时默认连接泄漏。`FileRunner.run` 中 `ConnectionFactory.open` 成功后 `connections.put("", first)`，若随后 `*SchemaIsolation.prepare(first)` 抛 `SQLException`，内层 catch 直接 `return hardFailure`；该 return 位于 isolation 块的独立 try（无 finally），绕过了只包住 `runner.run()` 段的 finally（负责 teardown + 关闭所有连接），`first` 永不关闭。`open` 失败路径（put 之前抛）无泄漏。

## Bug 定位

`cli/FileRunner.java`：`if (needsIsolation)` 块内
```java
Connection first = ConnectionFactory.open(options);
connections.put("", first);
try {
    ... prepare(first) ...
} catch (SQLException ex) {
    err.println("schema isolation failed: ...");
    return FileOutcome.hardFailure(...);   // ← 绕过外层 finally，first 泄漏
}
```
外层 `finally`（关闭 `connections.values()`）仅作用于 `runner.run()` 那段 try，不覆盖此 early return。

## 修复方案

审计建议二选一：「把 needsIsolation 块纳入 try-finally」或「早期 return 路径显式 close」。取**显式 close**：在 prepare 失败的 catch 内、return 之前 `first.close()`（吞 SQLException）。理由：
- 手术式、低风险，不动既有工作代码结构；
- 该 catch 是唯一泄漏点（open 失败路径 put 之前抛、无泄漏；prepare 成功则正常进入 runner 段、finally 正常关闭）；
- 审计明确认可此方案。

## 触碰路径

- `src/main/java/com/ggtest/cli/FileRunner.java`

## 验收与验证

| ID | 要求 | 预期证据 |
|---|---|---|
| V1 | prepare 失败路径关闭 first（不泄漏） | code inspection：catch 内 first.close() 后再 return |
| V2 | 既有 SQLite / 架构守护不回归 | `mvn clean test` 0 failures |
| V3 | open 失败、prepare 成功路径不变 | 既有用例 + PG 门控用例不回归 |

## 验证缺口

| 项 | 原因 | 风险 | 恢复条件 |
|---|---|---|---|
| prepare 失败路径无确定性单测 | 触发需真实 PG/MySQL 且 CREATE SCHEMA 失败（DB 故障注入）；且 close 仅资源层可观测，断言困难 | 低（prepare 极少失败；每次泄漏一连接，JVM 退出 OS 回收） | 由 code inspection + 全量回归保障；后续若有故障注入环境可补集成测 |

## 文档影响

开发/用户/运维均 N/A（资源管理内部细节，无对外行为变化）。

## 交接顺序

Developer → Reviewer（required）→ QA → 合并授权 → done → 合入 main。

## 修订记录

| 日期 | 摘要 |
|---|---|
| 2026-08-13 | 初版 Plan（来源：2026-08-12 xugu 审计，登记册 CA-023） |
