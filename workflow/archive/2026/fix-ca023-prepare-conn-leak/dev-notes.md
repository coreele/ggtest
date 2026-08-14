# Dev Notes: fix-ca023-prepare-conn-leak

## 实现摘要

- 分支：`fix-ca023-prepare-conn-leak` ← `main`。
- 修复 CA-023：隔离引擎（PG/MySQL）`prepare` 失败时默认连接泄漏。`FileRunner.run` 中 isolation 块是独立 try（无 finally），其内 `return hardFailure` 绕过外层 finally（负责 teardown + 关闭所有连接），`first` 永不关闭。
- 修复：在 prepare 失败的 catch 内、return 之前显式 `first.close()`（吞 SQLException）。

## 决策

- 取「显式 close」而非「把 needsIsolation 块纳入 try-finally」：前者手术式、低风险、不动既有工作代码结构；该 catch 是唯一泄漏点（open 失败路径 put 之前抛、无泄漏；prepare 成功则进入 runner 段、finally 正常关闭）。审计二选一均认可。
- 未 `connections.remove("")`：return 后局部 map 即丢弃，无意义。

## 变更路径

| 任务 | 路径 |
|---|---|
| 泄漏修复 | `src/main/java/com/ggtest/cli/FileRunner.java`（prepare 失败 catch 加 `first.close()`） |

diff 要点：
```java
} catch (SQLException ex) {
    err.println("schema isolation failed: " + sanitize(ex.getMessage()));
    try { first.close(); } catch (SQLException ignored) { }   // ← 新增：关闭泄漏连接
    return FileOutcome.hardFailure(...);
}
```

## 验证

| 命令 | 结果 |
|---|---|
| `mvn clean test` | Tests=**369** Failures=0 Errors=0 Skipped=34；BUILD SUCCESS |

## 验证缺口

- prepare 失败路径无确定性单测：触发需真实 PG/MySQL 且 CREATE SCHEMA 失败（DB 故障注入）；close 仅资源层可观测，断言困难。与审计「prepare 极少失败」判断一致。
- 保障：code inspection（catch 内 close 后再 return）+ 全量回归（SQLite 全路径 + 架构守护 + PG 门控 happy path 不回归）。

## 文档影响

| 类别 | 已更新路径或交接说明 |
|---|---|
| 开发文档 | N/A（资源管理内部细节） |
| 用户文档 | N/A（无对外行为变化） |
| 运维文档 | N/A |

## 未解决风险 / 验证缺口

| 项 | 原因 | 风险 | 恢复条件 |
|---|---|---|---|
| prepare 失败路径无确定性单测 | 见上 | 低（prepare 极少失败；JVM 退出 OS 回收连接） | 有故障注入环境时补集成测 |

## QA 修复回执

| 缺陷 ID | 处理 | 摘要 | 验证 | 建议复测 |
|---|---|---|---|---|
| — | N/A | 本轮无 QA Fail | — | — |
