# Dev Notes: fix-ca017-override-atomic-move

## 实现摘要

- 分支：`fix-ca017-override-atomic-move` ← `main`。
- 缺陷根因：`OverrideWriter.writeAtomically` 的内层 `catch (UnsupportedOperationException)` 是死代码。`Files.move(src, target, ATOMIC_MOVE)` 不支持时实际抛 `java.nio.file.AtomicMoveNotSupportedException`（`FileSystemException → IOException`，**非** `UnsupportedOperationException`），直接落入外层 `catch (Exception)` 以 `IOException` 重抛，文档/Javadoc 承诺的 `REPLACE_EXISTING` 回退分支不可达。
- 修复：①内层 `catch` 改为 `AtomicMoveNotSupportedException`，使回退可达；②引入最小注入缝 —— 包级函数式接口 `FileMover` + 包级构造器，默认仍委派 `Files.move`（默认构造器行为不变），使该回退分支可在任意本地 FS 上确定性测试（常规本地 FS 不会触发 `AtomicMoveNotSupportedException`，否则无法覆盖）。
- 默认行为对外不变：生产路径仍 `Files.move(..., ATOMIC_MOVE)`，失败时按文档回退 `REPLACE_EXISTING`。

## 变更路径

| 任务 | 路径 |
|---|---|
| 修复 + 注入缝 | `src/main/java/com/ggtest/cli/OverrideWriter.java` |
| 回归测试 | `src/test/java/com/ggtest/cli/OverrideWriterTest.java` |
| 开发记录 | `workflow/docs/features/fix-ca017-override-atomic-move/dev-notes.md` |

具体改动（`OverrideWriter.java`）：
- import：新增 `java.nio.file.AtomicMoveNotSupportedException`、`java.nio.file.CopyOption`。
- 新增包级 `@FunctionalInterface interface FileMover { void move(Path, Path, CopyOption...) }`。
- 新增 `private final FileMover mover` 字段；无参构造器委派 `(s,t,o) -> Files.move(s,t,o)`；新增包级 `OverrideWriter(FileMover mover)`（仅测试注入，`Objects.requireNonNull`）。
- `writeAtomically`：`Files.move(...)` → `mover.move(...)`；`catch (UnsupportedOperationException ex)` → `catch (AtomicMoveNotSupportedException ex)`。外层 `catch (Exception)` 清理（删 temp、保原文件）不变。

## 验证

| 命令 | 结果摘要 / 证据 | 备注 |
|---|---|---|
| `mvn -Dtest=OverrideWriterTest test` | Tests=**15** Failures=0 Errors=0 Skipped=0；BUILD SUCCESS | 14 → 15（+1 新增回退分支测试） |
| `mvn clean test` | Tests=**360** Failures=0 Errors=0 Skipped=34；BUILD SUCCESS | 全量回归无退化；Skipped=34 为既有 PG/MySQL/语料等门控 skip，与本修复无关 |

新增测试 `writeAtomically_fallsBackToReplaceWhenAtomicMoveUnsupported`：注入一个对 `ATOMIC_MOVE` 抛 `AtomicMoveNotSupportedException`、对其余选项执行真实 `Files.move` 的 `FileMover`，断言写回成功且目标内容更新为 `replaced\n`。该测试在修复前（死代码 catch）会失败：异常类型不匹配 → 落入外层 catch → 抛 `IOException` 而非回退。

既有覆盖不回归：`writeAtomically_overwritesFile`、`writeAtomically_utf8Content`（快乐路径原子移动）、`writeFailureLeavesOriginalIntact`、`writeAtomically_nonexistentParentThrows`（失败清理）。

## 文档影响

| 类别 | 已更新路径或交接说明 |
|---|---|
| 开发文档 | N/A（实现内部细节；修复后行为与既有 Javadoc `writeAtomically`「If atomic move is unsupported, REPLACE_EXISTING is used」一致，无需改 Javadoc） |
| 用户文档 | N/A（无 CLI/行为对外变化；`--override` 在不支持原子移动的 FS 上由「失败」变为「按文档回退」，属缺陷修复） |
| 运维文档 | N/A |

## 未解决风险 / 验证缺口

| 项 | 原因 | 风险 | 恢复条件 |
|---|---|---|---|
| N/A | 通过 `FileMover` 注入缝，回退分支可在任意本地 FS 确定性触发，无环境缺口 | — | — |

## QA 修复回执

| 缺陷 ID | 处理 | 摘要 | 验证 | 建议复测 |
|---|---|---|---|---|
| — | N/A | 本轮无 QA Fail | — | — |
