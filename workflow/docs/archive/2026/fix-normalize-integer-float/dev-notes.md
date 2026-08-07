# Dev Notes: fix-normalize-integer-float

## 实现说明

- 工作项: `fix-normalize-integer-float`（未拆分）；路径 fast；Review required；源分支 `fix-normalize-integer-float`（自 `main` 创建并已检出）；本轮 **未** commit / push / merge
- Plan T0–T4 已完成
- 行为: `normalizeInteger` 用 `Double.parseDouble` → 向零截断为 `long` → 整数文本；非数值 / NaN / Infinity → `"0"`；未改 R/T、runner、JDBC、ResultComparer；未触碰 WI-2/WI-3
- 变更路径:
  - `src/test/java/com/ggtest/normalize/ValueNormalizerTest.java`（T1）
  - `src/main/java/com/ggtest/normalize/ValueNormalizer.java`（T2 + Javadoc）
  - `workflow/workflow/docs/features/fix-normalize-integer-float/dev-notes.md`（本文件）
- 禁止未改: `workflow/workflow/docs/manager/*`、`STATUS.md`、本项无关 `pom.xml`、WI-2/WI-3 实现、`sqllogictest/` 入库
- 文档影响: 开发文档（本文件 + Javadoc）；用户/运维 N/A
- 开放问题 / §6: 无
- 建议下一步: Reviewer → Approve 后 QA（不得因 WI-2/WI-3 位点否定本项）

### TDD（red → green）

| 阶段 | 结果 |
|---|---|
| T1 Red | 仅改测：`"1.0"`/`"5.0"`→对应整数；`"1.25"`→`"1"`；`"-1.9"`→`"-1"`；`"1.5"`→`"1"`（修正原→`"0"`）；`"abc"`→`"0"`；纯整数不回归。未改实现时 Fail：`expected: <1> but was: <0>`（`Long.parseLong("1.0")`） |
| T2 Green | 截断实现后 `ValueNormalizerTest` Failures/Errors = 0 |

### 验证

| 命令 | 结果 |
|---|---|
| `mvn -q test -Dtest=ValueNormalizerTest` | Pass |
| `mvn -q clean test` | BUILD SUCCESS；tests=228 failures=0 errors=0 skipped=18 |
| `mvn -q -DskipTests package` | BUILD SUCCESS |
| `./bin/ggtest ./sqllogictest/test/evidence/slt_lang_aggfunc.test` | 文件 FAILED（仅 WI-2/WI-3）；**WI-1 消失**（无 ~43/~86/~380/~390 的 `0` mismatch / label 冲突） |

仍失败（允许）:

| 行号 | WHY | 简述 | 归属 |
|---|---|---|---|
| ~480 | integer overflow | `SELECT sum(x) FROM t1` | WI-2 |
| ~484 | integer overflow | `SELECT sum(DISTINCT x) FROM t1` | WI-2 |
| ~491 | result mismatch | `total(x)` 期望 `...50000.000`，实际 `...52000.000` | WI-3 |

## QA 修复回执

| 缺陷 ID | 处理 | 摘要 | 验证 | 建议复测 |
|---|---|---|---|---|
| — | — | 尚无 QA 缺陷 | — | — |
