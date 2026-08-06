# QA Report: fix-normalize-integer-float

## 轮次

| 轮次 | 日期 | 范围 | 结论 |
|---|---|---|---|
| 1 | 2026-08-06 | 首测：A1–A5 + Plan L2 | Pass |

## 环境与命令

- 分支：`fix-normalize-integer-float`；实现未 commit（工作区相对 `HEAD`=`e110f5f`）
- 环境：Linux；OpenJDK 17.0.19；Maven 3.6.3
- QA 入口：Plan **approved**；Review **Approve**（门禁 required）；路径 fast；状态 `qa`
- 实现：`ValueNormalizer.java`、`ValueNormalizerTest.java`、本目录文档
- 不计入本项交付：工作区脏 `pom.xml`、`docs/manager/*`、未跟踪 `sqllogictest/`

| 命令 | 结果 |
|---|---|
| `mvn -q test -Dtest=ValueNormalizerTest` | exit 0；Tests=**11** Failures=**0** Errors=**0** Skipped=**0** |
| `mvn -q clean test` | exit 0；Tests=**228** Failures=**0** Errors=**0** Skipped=**18**（既有门控） |
| `mvn -q -DskipTests package` | exit 0（补 JAR 以跑 CLI） |
| `./bin/ggtest ./sqllogictest/test/evidence/slt_lang_aggfunc.test` | 文件 FAILED；仅剩下表 WI-2/WI-3；**无** WI-1（~43/~86/~380/~390 `0` mismatch / label 冲突） |

SLT 剩余失败位点（允许，不否定本项）:

| 行号 | WHY | 简述 | 归属 |
|---|---|---|---|
| 480 | integer overflow | `SELECT sum(x) FROM t1` | WI-2 |
| 484 | integer overflow | `SELECT sum(DISTINCT x) FROM t1` | WI-2 |
| 491 | result mismatch | `total(x)` 期望 `...50000.000`，实际 `...52000.000` | WI-3 |

## 覆盖（对照 plan 最低验证层 + spec 验收）

Spec：N/A（skipped）。依据 Plan A1–A5 / L2；对齐归档 I：`%d` 向零截断；非数值 → `"0"`。

| ID | 条目 | 结果 | 证据 |
|---|---|---|---|
| A1 | 浮点样 `"1.0"`/`"5.0"` → 对应整数文本 | Pass | `integerFloatLikeTruncatesTowardZero`；`ValueNormalizerTest` 11/0/0/0 |
| A2 | 非整数数值向零截断（含负） | Pass | `"1.25"→"1"`、`"-1.9"→"-1"`、`"1.5"→"1"`；`(long) Double.parseDouble` |
| A3 | 真正非数值 → `"0"`；纯整数 / NULL 不回归 | Pass | `"abc"→"0"`；`"42"`/`"-7"`；`null→"NULL"` |
| A4 | `slt_lang_aggfunc` WI-1 位点消失 | Pass | CLI 仅剩 480/484/491；无 ~43/~86/~380/~390 的 `0` mismatch / label 冲突 |
| A5 | 范围仅本缺陷；WI-2/WI-3 可仍失败 | Pass | WI diff 限 normalizer + 对应测试 + 本目录文档；仍失败位点见上表 |
| L2 | Plan 最低验证层 | Pass | 定点单测 + `mvn clean test` 绿；A4 CLI 已独立执行 |

文档：`dev-notes.md` Pass（red→green、L2、A4、WI-2/WI-3）；用户/运维 N/A。

安全：结果值字符串解析与整数格式化；无认证/授权/文件写回/出站网络/本项依赖升级/敏感写入。敏感信息无发现；`parseDouble` 属既有比对路径。处置：无安全阻塞；允许合并（待用户授权；QA 不执行合并）。

回归：`ValueNormalizerTest`；全量 Maven；`slt_lang_aggfunc.test` 整文件（WI-1 消失；WI-2/WI-3 允许残留）。quality.md §6：无。

## 缺陷

| ID | 严重度 | 摘要 | 状态 |
|---|---|---|---|
| — | — | 无 | — |

## 结论

- 总体: **Pass**
- 恢复条件: N/A
- 合并: 待用户授权（本轮 **未** commit / push / merge；`qa-report.md` 留工作区）
- 残余风险:（1）合入排除无关 `pom.xml`；（2）`docs/manager/*` / 并列 WI 记录由 Manager 择机入库；（3）未跟踪 `sqllogictest/` 勿入库；（4）整文件 `failed≠0` 属 WI-2/WI-3，不阻塞本项
- 建议下一步: **merge-auth**（Manager 停授权；QA 不请求合并执行）；授权后 Manager 置 `done` 并与未入库报告/实现一次提交，再合入 `main`
