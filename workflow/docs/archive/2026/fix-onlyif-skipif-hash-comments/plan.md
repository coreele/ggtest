# Plan: fix-onlyif-skipif-hash-comments

## 元信息

- 工作项标识: fix-onlyif-skipif-hash-comments（未拆分，sub-feature-id = feature-id）
- 依据 Spec: N/A（Spec 门禁 skipped；范围以工作项记录为准）
- 依据 Design: N/A（Design 门禁 skipped）
- 路径等级: fast
- Review 门禁: required（进入 QA 前须 Reviewer `Approve`；触及语料条件指令解析）
- 最低验证层: L2（定点 parser 单测 + 全量 `mvn test`；`in1.test` 解析冒烟必达；全文件执行零失败 nice-to-have）
- 验证命令: 见「验证」节
- 源分支: `fix-onlyif-skipif-hash-comments` → 目标 `main`
- 本轮约束: 不要 commit / push / merge（除非用户后续明确要求）

## 适用工程规范

- [文档工程](../../standards/documentation.md)
- [Git 协作](../../standards/git.md)
- [质量与验证](../../standards/quality.md)
- [安全](../../standards/security.md)

## 目标摘要

修复 `SqlLogicTestParser`，使 `onlyif`/`skipif` 行尾 `#` 注释被剥离/忽略，官方写法（如 `onlyif sqlite # empty RHS`、`skipif mysql # not compatible`）能识别引擎名，消除 `parse error: onlyif requires a database name`（`sqllogictest/test/evidence/in1.test` ≈22）。

工作假设：`splitTokens` 不剥离行尾 `# …` → `onlyif sqlite # empty RHS` tokens 长度 > 2 → `parseOnlyIf`/`parseSkipIf` 因 `tokens.length != 2` 抛错。整行 `#` 注释已跳过；缺口仅指令行尾注释。范围止于该修复；不改 runner 求值；不新开公开合同；不扩到无关语料/特性。其他记录类型不做新注释合同（共享 strip 若无可见行为变化可接受）。

## 任务拆解

### T0 — 分支

- 做：从 `main` 创建或检出 `fix-onlyif-skipif-hash-comments`。
- 完成条件：当前为源分支；未改 `workflow/docs/manager/*` / `STATUS.md`。

### T1 — Red：行尾 `#` 单测（先于改 parser）

- 做：在 `src/test/java/com/ggtest/parser/SqlLogicTestParserTest.java` 增补用例（风格对齐现有 `p1_a_*` / 描述性命名），至少：
  1. `onlyif sqlite # empty RHS` → `OnlyIfRecord`，`dbName() == "sqlite"`，不抛 `ParseException`；
  2. `skipif mysql # not compatible` → `SkipIfRecord`，`dbName() == "mysql"`；
  3. 无尾注释 `onlyif`/`skipif` 不回归（可依赖既有 `p1_a_allRecordTypes_…` 或显式断言）。
- 完成条件：**未改** parser 时新测失败，可归因于 `onlyif`/`skipif` requires a database name 或等价断言失败。

### T2 — Green：parser 忽略行尾 `#`

- 做：改 `src/main/java/com/ggtest/parser/SqlLogicTestParser.java`，使 `parseOnlyIf`/`parseSkipIf`（或共用 header tokenize）在 `tokens.length == 2` 判定前忽略行尾 `#` 及之后文本。最小改动优先：仅对这两类指令 header strip 后再 `splitTokens`；须保持整行 `#` 注释行为不变。
- 不做：改 `SkipIfRecord`/`OnlyIfRecord`；改 runner；为其他记录类型新增注释合同。
- 完成条件：T1 新测与 `SqlLogicTestParserTest` 全绿。

### T3 — Verify：`in1.test` + 回归

- 必达：`sqllogictest/test/evidence/in1.test` 不再报 `onlyif requires a database name`（单测 `parser.parse(Path)`/片段，或 `./bin/ggtest`/`java -jar` 输出无该 parse error）。
- Nice-to-have：`--engine sqlite --url jdbc:sqlite::memory:` 整文件执行，记 exit/`failed`；执行层失败不得伪造成「解析未修」。
- 另跑：`mvn -q clean test`；可选 `mvn -q -DskipTests package`。
- 完成条件：必达证据写入 `dev-notes.md`；L2 绿或 quality.md §6 记缺口。

### T4 — 开发产物

- 做：本目录 `dev-notes.md`（实现摘要、red→green、验证表、§6 若有）。
- 完成条件：Reviewer/QA 可凭 plan + notes 复现；未擅自 commit。

## 依赖与顺序

T0 → T1（red）→ T2（green）→ T3 → T4 → Review → QA。禁止 T2 先于 T1（先失败测再改 parser）。

## 触碰路径

| 任务 | 预期路径 |
|---|---|
| T0 | 分支 `fix-onlyif-skipif-hash-comments` |
| T1 | `src/test/java/com/ggtest/parser/SqlLogicTestParserTest.java`（优先内联字符串） |
| T2 | `src/main/java/com/ggtest/parser/SqlLogicTestParser.java`（`parseOnlyIf`/`parseSkipIf`/`splitTokens` 或局部 strip） |
| T3 | 命令 + notes；可选 `Path` → `sqllogictest/test/evidence/in1.test` |
| T4 | `workflow/docs/features/fix-onlyif-skipif-hash-comments/dev-notes.md` |
| 禁止 | Spec/Design；`workflow/docs/manager/*`；`STATUS.md`；无关模块；本轮 commit（除非用户授权） |

## 验收

（fast / 无 Spec）

| ID | 要求 | 证据 |
|---|---|---|
| A1 | `onlyif <engine> # …` → 正确 `dbName`，不抛「requires a database name」 | T1/T2 单测 Pass |
| A2 | `skipif <engine> # …` 同上 | T1/T2 单测 Pass |
| A3 | 无尾注释 `onlyif`/`skipif` 不回归 | 既有 + 新测 Pass |
| A4 | `in1.test` 无该 parse error | 单测 parse 成功，或 CLI 无该错误 |
| A5 | 范围仅本缺陷 | diff 限于 parser + 对应测试 + 本目录文档 |

## 验证

### 命令

```bash
# T1 red（方法名以实装为准；应 Fail）
mvn -q test -Dtest=SqlLogicTestParserTest#<newMethodName>

# T2 定点
mvn -q test -Dtest=SqlLogicTestParserTest

# L2 全量
mvn -q clean test

# 可选
mvn -q -DskipTests package

# A4
./bin/ggtest --engine sqlite --url jdbc:sqlite::memory: \
  sqllogictest/test/evidence/in1.test
# 或 java -jar target/ggtest-*.jar --engine sqlite --url jdbc:sqlite::memory: \
#   sqllogictest/test/evidence/in1.test
# 必达：无 "onlyif requires a database name"；failed=0 为 nice-to-have
```

### 最低验证层理由

单点 parser 修复，单测锁定行为 + 全量 Maven 防回归 → **L2**。`in1.test` 解析冒烟为必达；整文件执行零失败不抬升为 L4。

### 预期证据

| 验证 | 通过时 |
|---|---|
| T1 red | 新测 Fail，指向缺 dbName 或等价断言失败 |
| `SqlLogicTestParserTest` | Failures/Errors = 0 |
| `mvn -q clean test` | BUILD SUCCESS；Failures=0（Skipped 记 notes） |
| A4 `in1.test` | 无 `parse error: onlyif requires a database name` |
| package（若跑） | BUILD SUCCESS |

### 无法验证（quality.md §6）

缺 Java 17/Maven 或 CLI 不可跑时：`dev-notes.md` 记「未验证项 → 原因 → 风险 → 恢复条件 → 复测范围」。禁止静默跳过 A4；至少保留对等价输入/`Path` 的 parser 单测作代理证据。

## Review 门禁与进入 QA

- Review：required。
- 进入 QA：T0–T4 完成；L2 绿（或已记不可执行项且 A1–A4 有代理证据）；`dev-notes.md` 含 red→green 与 A4；Reviewer **Approve**。

## 文档影响

| 类别 | 更新路径或 N/A |
|---|---|
| 开发文档 | 本目录 `dev-notes.md`（必写）；必要时 `SqlLogicTestParser` 局部注释；本 `plan.md` |
| 用户文档 | N/A（对齐官方语料既有写法；默认不改 README） |
| 运维文档 | N/A（无部署/排障变更） |

## 交接顺序

1. Planner：本 plan → 用户确认 → Manager 持久化确认后置 `planned`（Planner 不改状态、不调度 Developer）。
2. Developer：分支 `fix-onlyif-skipif-hash-comments` 实施 T0–T4；写 notes；不 commit（除非用户另要求）。
3. Reviewer：对照 plan + A1–A5 → Approve / 回退。
4. QA：独立复跑验证与 A4 → `qa-report.md` Pass/Fail/Blocked。
5. 合并：仅用户授权后由 Manager 流程处理。

## 开放问题

无。

## 修订记录

| 日期 | 摘要 |
|---|---|
| 2026-08-06 | 初稿并 documentation.md §B 自检：T0–T4 TDD；SqlLogicTestParser(+Test)；L2 + in1 解析必达；Review required |
