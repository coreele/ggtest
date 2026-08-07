# QA Report: fix-onlyif-skipif-hash-comments

## 轮次

| 轮次 | 日期 | 范围 | 结论 |
|---|---|---|---|
| 1 | 2026-08-06 | 首测：A1–A5 + Plan L2 | Pass |

## 环境与命令

- 分支：`fix-onlyif-skipif-hash-comments`；实现未 commit（基于 `main` `61225e247fc1f4a41eff56ce7709362bb73a631c`）
- 环境：OpenJDK 17.0.19；Maven 3.6.3；Linux
- QA 入口：Plan **approved**；Review **Approve**（门禁 required）；路径 fast；状态 `qa`
- 实现：工作区 `SqlLogicTestParser.java`、`SqlLogicTestParserTest.java` + 本目录文档

| 命令 | 结果 |
|---|---|
| `mvn -q test -Dtest=SqlLogicTestParserTest` | exit 0；Tests=**33** F=**0** E=**0** S=**0** |
| `mvn -q clean test` | exit 0；Tests=**227** F=**0** E=**0** S=**18**（既有门控） |
| `mvn -q -DskipTests package` | exit 0（`clean test` 后补 JAR 以跑 CLI） |
| `./bin/ggtest --engine sqlite --url jdbc:sqlite::memory: sqllogictest/test/evidence/in1.test` | exit 0；**无** `onlyif requires a database name`；**无** `parse error`；`TOTAL: passed=1 failed=0 skipped=0` |

## 覆盖（对照 plan 最低验证层 + spec 验收）

Spec：N/A（skipped）。依据 Plan A1–A5 / L2。

| ID | 条目 | 结果 | 证据 |
|---|---|---|---|
| A1 | `onlyif <engine> # …` → 正确 `dbName`，不抛「requires a database name」 | Pass | `onlyif_trailingHashComment_parsesDbName`；`splitTokens(stripTrailingHashComment(header))`；定点 33/0/0/0 |
| A2 | `skipif <engine> # …` 同上 | Pass | `skipif_trailingHashComment_parsesDbName` |
| A3 | 无尾注释 `onlyif`/`skipif` 不回归 | Pass | `onlyifAndSkipif_withoutTrailingHash_stillParse` + 既有 `p1_a_*`；全量 227/0/0/18 |
| A4 | `in1.test` 无该 parse error | Pass | CLI 无该字符串、无 `parse error`；`passed=1 failed=0` |
| A5 | 范围仅本缺陷 | Pass | 意图变更限 parser + 对应测试 + 本目录文档；无关 `pom.xml` / `agents/docs/manager/*` 不计入交付 |
| L2 | Plan 最低验证层 | Pass | 定点单测 + `mvn -q clean test` 绿；A4 CLI 必达满足 |

文档：`dev-notes.md` Pass（red→green、验证表、A1–A5）；用户/运维文档 N/A。

安全：语料行解析（行尾 `#` 剥离）；无认证/授权/网络/本项依赖升级/敏感写入；CLI 用 `jdbc:sqlite::memory:`。敏感信息无发现；输入面可接受。处置：无安全阻塞；**允许合并**（待用户授权）。

回归：`SqlLogicTestParserTest` 全类；全量 Maven；`in1.test` 整文件。quality.md §6：无（全部适用命令已独立执行）。

## 缺陷

| ID | 严重度 | 摘要 | 状态 |
|---|---|---|---|
| — | — | 无 | — |

## 结论

- 总体: **Pass**
- 恢复条件: N/A
- 合并: 待用户授权（本轮 **未** commit / push / merge；`qa-report.md` 留工作区）
- 残余风险:（1）合入须排除无关 `pom.xml`；（2）`agents/docs/manager/*` 由 Manager 择机入库；（3）`stripTrailingHashComment` 自首个 `#` 剥离——引擎名不含 `#` 与合同一致；含 `#` 引擎名未单测（范围外）。
- 建议下一步: 向用户请求 **merge-auth**；授权后 Manager 置 `done` 并与报告一次提交；合入限 parser + 测试 + 本目录文档。
