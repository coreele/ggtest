# Plan: fix-ca017-override-atomic-move

## 元信息

- 工作项标识: fix-ca017-override-atomic-move
- sub-feature-id: fix-ca017-override-atomic-move（未拆分）
- 依据 Spec: N/A（fast，单点正确性修复）
- 依据 Design: N/A
- 依据 UI: N/A
- 路径等级: fast
- Review 门禁: required
- 最低验证层: 单元测试 + 构建（含新增测试覆盖原子移动回退分支）
- 验证命令:
  - `mvn -q -Dtest=OverrideWriterTest test`
  - `mvn -q clean test`
- 预期证据:
  - `OverrideWriterTest` 全部 Pass（含新增 `writeAtomically_fallsBackToReplaceWhenAtomicMoveUnsupported`）
  - `mvn clean test`：BUILD SUCCESS，0 failures（回归无退化）
  - 构建产物正常打包

## 适用工程规范

> 使用仓库根路径。

- `workflow/docs/standards/documentation.md`
- `workflow/docs/standards/git.md`（Git 工作区）
- `workflow/docs/standards/quality.md`
- `workflow/docs/standards/security.md`

## 目标摘要

修复 `OverrideWriter.writeAtomically` 的原子移动回退为死代码缺陷：内层 `catch (UnsupportedOperationException)` 无法捕获 `Files.move(..., ATOMIC_MOVE)` 在不支持原子移动时实际抛出的 `AtomicMoveNotSupportedException`（`FileSystemException → IOException`，非 `UnsupportedOperationException`），导致文档承诺的 `REPLACE_EXISTING` 回退永不触发，在不支持原子移动的文件系统上 `--override` 写回直接以 `IOException` 失败。同时为该回退分支补充此前缺失的回归测试。

## Bug 定位

`src/main/java/com/ggtest/cli/OverrideWriter.java:84-88`

```java
try {
    Files.move(temp, target.toAbsolutePath(), StandardCopyOption.ATOMIC_MOVE);
} catch (UnsupportedOperationException ex) {                       // ← 永不触发
    Files.move(temp, target.toAbsolutePath(), StandardCopyOption.REPLACE_EXISTING);
}
```

`Files.move(src, target, ATOMIC_MOVE)` 在目标 FS 不支持原子移动时抛 `java.nio.file.AtomicMoveNotSupportedException`，它继承自 `FileSystemException → IOException`，**不是** `UnsupportedOperationException`。该异常落入外层 `catch (Exception)`（L89）→ 删除 temp 后以 `IOException` 重抛，L87 回退分支不可达。与方法 Javadoc（L68-70）承诺相反。

## 修复方案

1. **修正捕获类型**：将内层 `catch (UnsupportedOperationException ex)` 改为 `catch (AtomicMoveNotSupportedException ex)`，使 `REPLACE_EXISTING` 回退可达。
2. **可测性（§6）**：引入最小注入缝，使「原子移动 → 回退非原子替换」策略可在任意本地 FS 上确定性地测试（现有测试无法在常规本地 FS 上触发 `AtomicMoveNotSupportedException`）。新增包级 `FileMover` 函数式接口与包级构造器，默认实现委派 `Files.move`：

```java
@FunctionalInterface
interface FileMover {
    void move(Path source, Path target, CopyOption... options) throws IOException;
}

private final FileMover mover;

OverrideWriter() {
    this((source, target, options) -> Files.move(source, target, options));
}

// 包级，仅测试注入
OverrideWriter(FileMover mover) {
    this.mover = Objects.requireNonNull(mover, "mover");
}
```

`writeAtomically` 改为经 `mover` 执行移动：

```java
Files.writeString(temp, newText, StandardCharsets.UTF_8);
try {
    mover.move(temp, target.toAbsolutePath(), StandardCopyOption.ATOMIC_MOVE);
} catch (AtomicMoveNotSupportedException ex) {
    mover.move(temp, target.toAbsolutePath(), StandardCopyOption.REPLACE_EXISTING);
}
```

新增 import：`java.nio.file.AtomicMoveNotSupportedException`、`java.nio.file.CopyOption`。外层 `catch (Exception)` 清理（删 temp、保原文件）不变。

## 任务拆解

1. **修改 `OverrideWriter`**（完成条件：`catch` 类型改为 `AtomicMoveNotSupportedException`；新增 `FileMover` 缝 + 包级构造器；默认构造器行为不变）。
2. **新增测试** `OverrideWriterTest.writeAtomically_fallsBackToReplaceWhenAtomicMoveUnsupported`（完成条件：注入一个对 `ATOMIC_MOVE` 抛 `AtomicMoveNotSupportedException`、对其余选项执行真实 `Files.move(REPLACE_EXISTING)` 的 `FileMover`；断言写回成功且目标内容更新）。
3. **开发者验证**（完成条件：`OverrideWriterTest` 全 Pass；`mvn clean test` BUILD SUCCESS 无回归）。
4. **Review / QA**（完成条件：`review.md` Approve；`qa-report.md` Pass）。

## 依赖与顺序

- 无外部依赖；仅触碰 `OverrideWriter` 与其测试。
- 顺序：1 → 2 → 3 → Review → QA。

## 触碰路径

- `src/main/java/com/ggtest/cli/OverrideWriter.java`
- `src/test/java/com/ggtest/cli/OverrideWriterTest.java`

## 验收与验证

> fast 且无 Spec：可测条目如下。

| ID | 要求或命令 | 预期证据 | 结果（实施后填） |
|---|---|---|---|
| V1 | `writeAtomically` 在原子移动不支持时回退 `REPLACE_EXISTING` 成功 | 新增测试 Pass | |
| V2 | 现有原子移动快乐路径不回归 | `writeAtomically_overwritesFile` / `_utf8Content` Pass | |
| V3 | 写失败时原文件完整（不回归） | `writeFailureLeavesOriginalIntact` Pass | |
| V4 | `mvn -q -Dtest=OverrideWriterTest test` | BUILD SUCCESS，0 failures | |
| V5 | `mvn -q clean test` | BUILD SUCCESS，全量 0 failures（回归无退化） | |

## 验证缺口

| 项 | 原因 | 风险 | 恢复条件 |
|---|---|---|---|
| N/A | | | |

> 说明：通过 `FileMover` 注入缝，可在任意本地 FS 上确定性触发回退分支，无环境缺口。

## 文档影响

| 类别 | 更新路径或 N/A 理由 |
|---|---|
| 开发文档 | N/A（实现内部细节；Javadoc 已说明回退语义，修复后行为与现有 Javadoc 一致，无需改动） |
| 用户文档 | N/A（无 CLI/行为对外变化；`--override` 在不支持原子移动的 FS 上由「失败」变为「按文档回退」，属缺陷修复，README 无需改） |
| 运维文档 | N/A |

## 交接顺序

1. Developer 实施与开发者验证 →
2. Reviewer（Review 门禁 required）→
3. QA 验收 →
4. 用户合并授权 → Manager `done` 一次提交 → 合入

## 修订记录

| 日期 | 摘要 |
|---|---|
| 2026-08-13 | 初版 Plan（来源：2026-08-13 审计 CA-017） |
