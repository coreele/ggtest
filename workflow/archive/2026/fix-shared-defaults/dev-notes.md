# Dev Notes: fix-shared-defaults

> 开发实施记录。fast 路径；无 Spec / Design。

## 摘要

收敛 `DEFAULT_HASH_THRESHOLD` 与 `DEFAULT_COLUMN_SEPARATOR` 重复字面量；默认值与运行时行为不变。

## Plan 任务完成

| 任务 | 状态 | 说明 |
|---|---|---|
| T1 | 完成 | 删除 `CliArgumentParser.DEFAULT_HASH_THRESHOLD`；`RuntimeConfigResolver` 引用 `ResultComparer.DEFAULT_HASH_THRESHOLD` |
| T2 | 完成 | 新增 `SqlLogicDefaults`；`ResultComparer` 转发 column 常量；`SqlLogicTestParser` 引用 model |
| T3 | 完成 | `rg` 确认权威落点唯一 |
| T4 | 完成 | `mvn -q clean test` 退出码 0 |
| T5 | 完成 | CA-004、CA-005 → `resolved` |

## 变更路径

| 路径 | 变更 |
|---|---|
| `src/main/java/com/ggtest/model/SqlLogicDefaults.java` | **新增** — column 分隔符唯一字面量 |
| `src/main/java/com/ggtest/normalize/ResultComparer.java` | hash 唯一字面量保留；column 转发 `SqlLogicDefaults` |
| `src/main/java/com/ggtest/cli/CliArgumentParser.java` | 删除 `DEFAULT_HASH_THRESHOLD` |
| `src/main/java/com/ggtest/cli/RuntimeConfigResolver.java` | 默认回退 → `ResultComparer.DEFAULT_HASH_THRESHOLD` |
| `src/main/java/com/ggtest/parser/SqlLogicTestParser.java` | 删除私有常量；引用 `SqlLogicDefaults` |
| `src/test/java/com/ggtest/model/SqlLogicDefaultsTest.java` | **新增** — 默认常量与转发 |
| `src/test/java/com/ggtest/cli/RuntimeConfigResolverTest.java` | 新增 `defaultHashThresholdIsEight` |
| `workflow/audit/register.md` | **新增**（worktree）；CA-004/005 resolved |

## TDD

1. 新增 `SqlLogicDefaultsTest`、`RuntimeConfigResolverTest.defaultHashThresholdIsEight`。
2. 实现后全量测试通过；既有 `ResultComparerTest.defaultHashThresholdIsEight` 与 parser 测试覆盖行为不变。

## 验证

| 项 | 命令 / 方法 | 结果 |
|---|---|---|
| T3 静态核对 | `rg 'DEFAULT_HASH_THRESHOLD\s*=' src/` | 仅 `ResultComparer` |
| T3 静态核对 | `rg 'DEFAULT_COLUMN_SEPARATOR\s*=\s*" "' src/` | 仅 `SqlLogicDefaults` |
| L2 全量测试 | `mvn -q clean test` | 退出码 0；全部 Pass |

## 文档影响

- `dev-notes.md`（本文件）
- `workflow/audit/register.md` — CA-004、CA-005 resolved
- README / 用户文档 — N/A（无用户可见变更）

## 未解决风险

无。

## 建议后续

Review 门禁 skipped → 直接进入 QA 复验 A1–A7。
