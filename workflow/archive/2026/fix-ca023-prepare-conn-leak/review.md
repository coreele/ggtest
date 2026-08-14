# Review: fix-ca023-prepare-conn-leak

## 审阅范围

- 实现版本 / 提交: `c579fd8`（源分支 `fix-ca023-prepare-conn-leak`）
- 依据: [plan.md](plan.md)；fast 路径。来源 2026-08-12 xugu 审计（登记册 CA-023）。

## 实现正确性

- 泄漏分析准确：`connections.put("", first)` 后 prepare 抛 → catch 内 `return hardFailure`；isolation 块是独立 try（无 finally），return 绕过只包住 `runner.run()` 段的外层 finally（负责 teardown + 关闭所有连接），`first` 泄漏。✓
- 修复正确：prepare 失败 catch 内、return 前 `first.close()`（吞 SQLException）。`first` 在外层 open-try 作用域内可见。close 后 map 中残留已关闭连接，但立即 return、局部 map 丢弃，无影响。✓
- 路径完备性：open 失败路径（put 之前抛）无泄漏；prepare 成功路径进入 runner 段、finally 正常关闭。唯一泄漏点（prepare 失败）已覆盖。✓
- 未采用「纳入 try-finally」重构：选显式 close，手术式、低风险；审计二选一均认可。范围守纪律，仅 1 处 +4 行。
- 注：main 上的实现为 PG/MySQL；xgtest 分支 Xugu 同款模式在下次 rebase 后共享同一 FileRunner，一并覆盖。

## 测试有效性

- 无确定性单测（见 plan 验证缺口）：触发需真实 PG/MySQL 且 CREATE SCHEMA 失败；close 仅资源层可观测。与审计「prepare 极少失败」一致。
- 保障：`mvn clean test` 369/0/0（34 既有 skip），SQLite 全路径 + 架构守护 + PG 门控 happy path 不回归。
- code inspection：catch 内 `first.close()` 后再 return，逻辑直白。

## 文档影响核对

| Plan 声明 | 实现是否一致 | 备注 |
|---|---|---|
| 开发文档 N/A | 一致 | 资源管理内部细节 |
| 用户文档 N/A | 一致 | 无对外行为变化 |
| 运维文档 N/A | 一致 | — |

## 安全影响核对

| 检查项 | 结果 | 处置状态 | 备注 |
|---|---|---|---|
| 敏感信息 | 无 | n/a | — |
| 认证与授权 | 无 | n/a | — |
| 输入与外部访问 | 无 | n/a | — |
| 依赖变更 | 无 | n/a | — |
| 资源管理 | 改善 | 已闭环 | 修复连接泄漏（资源可用性） |

## 必修项

| ID | 位置 | 问题 | 状态 |
|---|---|---|---|
| — | — | 无阻塞项 | — |

## 结论

Approve

## 后续动作与复审范围

- 进 QA：`mvn clean test` 复跑（无回归）。
- QA Fail 修复后复审；范围限 FileRunner prepare 失败 catch。
