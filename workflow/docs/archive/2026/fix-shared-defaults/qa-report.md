# QA Report: fix-shared-defaults

## 轮次

| 轮次 | 日期 | 实现版本 | 范围 | 结论 |
|---|---|---|---|---|
| 1 | 2026-07-26 | `ae37ca2` | 首测 A1–A7 | **Pass** |

## 环境与命令

- 路径：`/Users/zhougangjie/Space/ggtest/.worktrees/fix-shared-defaults`
- 分支：`fix-shared-defaults` @ `ae37ca23a36eb7fa7c9a3c0b743d2599c331c400`
- JDK/Maven：本机可用；`mvn -q clean test` 退出码 0（约 19s）
- 门禁：fast；Plan approved；Review skipped（工作项已标）

## 覆盖（对照 plan 验收 A1–A7）

| ID | 条目 | 结果 | 证据 |
|---|---|---|---|
| A1 | `DEFAULT_HASH_THRESHOLD` 字面量仅在 `ResultComparer` | Pass | `rg 'DEFAULT_HASH_THRESHOLD\s*=' src/` → 仅 `ResultComparer.java:24`；`CliArgumentParser` 无该符号 |
| A2 | column 字面量仅在 `SqlLogicDefaults`；`ResultComparer` 转发 | Pass | `rg 'DEFAULT_COLUMN_SEPARATOR\s*=\s*" "' src/` → 仅 `SqlLogicDefaults.java:9`；`ResultComparer:27` 为 `= SqlLogicDefaults.DEFAULT_COLUMN_SEPARATOR` |
| A3 | CLI 默认 hash threshold 仍为 8 | Pass | `RuntimeConfigResolver.java:166` → `ResultComparer.DEFAULT_HASH_THRESHOLD`；`RuntimeConfigResolverTest.defaultHashThresholdIsEight`；`ResultComparerTest` 断言 8 |
| A4 | parser 默认列分隔符仍为 U+0020 | Pass | `SqlLogicTestParser` 引用 `SqlLogicDefaults.DEFAULT_COLUMN_SEPARATOR`；`SqlLogicDefaultsTest.defaultColumnSeparatorIsSpace` |
| A5 | `SqlLogicTestParser` 无 `normalize` import | Pass | 源码无 `import com.ggtest.normalize` |
| A6 | `mvn -q clean test` 退出码 0 | Pass | QA 独立执行；`EXIT_CODE=0`；全量 Pass |
| A7 | CA-004/CA-005 为 `resolved` | Pass | `workflow/workflow/docs/standards/code-audit-register.md` L24–25 |

## 回归

- L2 全量：`mvn -q clean test` — Pass
- 新增/受影响单测：`SqlLogicDefaultsTest`、`RuntimeConfigResolverTest.defaultHashThresholdIsEight` — 随全量通过

## 文档与安全

| 项 | 结果 | 说明 |
|---|---|---|
| 用户/运维文档 | N/A | plan 声明无用户可见变更 |
| `dev-notes.md` | Pass | T1–T5 与实现一致 |
| 安全（`security.md`） | N/A | 常量收敛；无认证/凭据/注入面变更 |

## 缺陷

无。

## 结论

- **总体：Pass**
- 恢复条件：N/A
- 合并：质量条件已满足；**待用户授权**（工作项要求合入前停合并授权）
- 报告状态：未提交（按 QA 规范）
