# Review: fix-ca017-override-atomic-move

## 审阅范围

- 实现版本 / 提交: `151d0f5`（源分支 `fix-ca017-override-atomic-move`，对比基线 `main`）
- 依据: [plan.md](plan.md)；fast 路径，无 Spec / Design

## 实现正确性

- 根因判断准确：`Files.move(src, target, ATOMIC_MOVE)` 不支持时抛 `java.nio.file.AtomicMoveNotSupportedException`（继承链 `FileSystemException → IOException → Exception`），**非** `UnsupportedOperationException`。原内层 `catch (UnsupportedOperationException)` 不可达，异常落入外层 `catch (Exception)` 以 `IOException` 重抛，`REPLACE_EXISTING` 回退死代码。修复改为 `catch (AtomicMoveNotSupportedException ex)` 后，内层先于外层捕获，回退可达。✓
- `FileMover` 注入缝设计合理且最小：包级 `@FunctionalInterface`，默认构造器委派 `(s,t,o) -> Files.move(s,t,o)`，生产路径行为与改动前逐字一致；包级 `OverrideWriter(FileMover)` 仅用于测试注入，`Objects.requireNonNull` 防御空值。`OverrideCoordinator.java:38` 的 `new OverrideWriter()` 调用源兼容。✓
- 失败语义保持：回退 `REPLACE_EXISTING` 再失败仍落入外层 `catch (Exception)` → 删 temp、保原文件、抛 `IOException`，与既有契约一致。✓
- 范围守纪律：仅触碰 `OverrideWriter` 与其测试；全仓 `ATOMIC_MOVE` 生产用法仅此一处，无同模式遗漏需在本切片处理。

## 测试有效性

- 新增 `writeAtomically_fallsBackToReplaceWhenAtomicMoveUnsupported`：注入对 `ATOMIC_MOVE` 抛 `AtomicMoveNotSupportedException`、对其余选项走真实 `Files.move` 的 `FileMover`，断言目标内容更新为 `replaced\n`。该测试在修复前（死代码 catch）会失败（异常类型不匹配 → 外层 catch → 抛 IOException），故真正覆盖了先前不可达分支。✓
- 既有覆盖不回归：`writeAtomically_overwritesFile` / `_utf8Content`（快乐路径原子移动）、`writeFailureLeavesOriginalIntact`（失败清理）、`writeAtomically_nonexistentParentThrows`。✓
- 验证证据复现：`mvn -Dtest=OverrideWriterTest test` → 15/0/0；`mvn clean test` → 360/0/0（34 既有 skip）。

## 文档影响核对

| Plan 声明 | 实现是否一致 | 备注 |
|---|---|---|
| 开发文档 N/A | 一致 | 既有 Javadoc「If atomic move is unsupported, REPLACE_EXISTING is used」修复后与行为一致，无需改 |
| 用户文档 N/A | 一致 | 无 CLI/对外行为变化 |
| 运维文档 N/A | 一致 | — |

## 安全影响核对

| 检查项 | 结果 | 处置状态 | 备注 |
|---|---|---|---|
| 敏感信息 | 无 | n/a | 未触碰凭据/日志 |
| 认证与授权 | 无 | n/a | — |
| 输入与外部访问 | 无 | n/a | 注入缝为包级，不对外暴露；temp 文件仍用 `Files.createTempFile` 于目标目录 |
| 依赖变更 | 无 | n/a | 仅 JDK NIO，无新依赖 |

## 必修项

| ID | 位置 | 问题 | 状态 |
|---|---|---|---|
| — | — | 无阻塞项 | — |

> `Comment` 不得包含阻塞项；阻塞问题须使用 `Request changes`。

## 结论

Approve

## 后续动作与复审范围

- 进入 QA：按 plan V1–V5 复测；重点 `OverrideWriterTest`（含新增回退分支）与 `mvn clean test` 全量回归。
- 后续若 QA Fail 触发 Developer 修复（本切片 Review=required），修复后须重新 Approve 方可进 QA；建议复审范围限 `OverrideWriter` 及其测试。
