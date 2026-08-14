# Plan: fix-normalize-integer-float

## 元信息

- 工作项标识: fix-normalize-integer-float（未拆分，sub-feature-id = feature-id）
- 依据 Spec: N/A（Spec 门禁 skipped；对齐已归档 [ggtest-core-normalize Spec](../ggtest-core/ggtest-core-normalize/spec.md)：I 按 `%d`；无法解释为整数 → `0`）
- 依据 Design: N/A（Design 门禁 skipped）
- 路径等级: fast
- Review 门禁: required（进入 QA 前须 Reviewer `Approve`；虽 fast，但触及核心归一化）
- 最低验证层: L2（定点 `ValueNormalizerTest` + 全量 `mvn test`；`slt_lang_aggfunc.test` 定点冒烟必达）
- 验证命令: 见「验证」节
- 源分支: `fix-normalize-integer-float` → 目标 `main`
- 本轮约束: **不要** commit / push / merge（除非用户后续明确要求）

## 适用工程规范

- [文档工程](../../standards/documentation.md)
- [Git 协作](../../standards/git.md)
- [质量与验证](../../standards/quality.md)
- [安全](../../standards/security.md)

## 目标摘要

纠偏 `ValueNormalizer.normalizeInteger`：`Long.parseLong` 对 JDBC `getString` 的 `"1.0"` / `"1.25"` / `"5.0"` 抛 `NumberFormatException` → `"0"`，致 `slt_lang_aggfunc.test` 中期望 1/5/3 等失败并连带 `label-sum` / `label-sum-distinct` 冲突。

对齐官方 `%d`：可解析数值（含浮点样字符串）解释后**向零截断**再格式化为整数文本；真正非数值仍 → `"0"`。不扩写公开合同。

### 明确排除

| 项 | 指令 |
|---|---|
| WI-2 `fix-aggfunc-sum-overflow`（~480/484 integer overflow） | 禁止纳入本 Plan / 本 diff |
| WI-3 `fix-aggfunc-total-precision`（~491 大浮点漂移） | 禁止纳入本 Plan / 本 diff |
| `pom.xml` 编译器 pin、未跟踪 `sqllogictest/` 大体量入库 | 禁止 |

## 任务拆解

### T0 — 分支

- 做：从 `main` 创建或检出 `fix-normalize-integer-float`。
- 完成条件：当前为源分支；未改 `workflow/docs/manager/*` / `STATUS.md`。

### T1 — Red：浮点样 I 单测（先于改实现）

- 做：更新/增补 `src/test/java/com/ggtest/normalize/ValueNormalizerTest.java`，至少：
  1. `"1.0"` → `"1"`；`"5.0"` → `"5"`；
  2. `"1.25"` → `"1"`；`"-1.9"` → `"-1"`（向零截断）；
  3. 纯整数 `"42"` / `"-7"` 不回归；
  4. 非数值 `"abc"` → `"0"`；
  5. **修正**既有 `integerUnparseableBecomesZero`：`"1.5"` → `"0"` 与 `%d` 冲突，改为 `"1.5"` → `"1"`（或并入截断用例）。
- 不做：改 `ValueNormalizer`（结束时新期望应 Fail）。
- 完成条件：未改实现时新/改测失败，可归因于 `Long.parseLong` 或错误期望。

### T2 — Green：`normalizeInteger` 按 `%d` 截断

- 做：改 `src/main/java/com/ggtest/normalize/ValueNormalizer.java` 的 `normalizeInteger`：可解析数值 → 向零截断 → 整数文本；解析失败 → `"0"`。更新 Javadoc（非法 I = 非数值 → `"0"`；浮点样属可解释）。优先最小改动（如 `Double.parseDouble` + 向零截断为 `long`）；NaN/Infinity 等不可作有限整数 → `"0"`。
- 不做：改 R/T；改 runner/JDBC/ResultComparer；处理 WI-2/WI-3。
- 完成条件：T1 相关测与 `ValueNormalizerTest` 全绿。

### T3 — Verify：定点语料 + 回归

- 必达：`./bin/ggtest ./sqllogictest/test/evidence/slt_lang_aggfunc.test`（或等价 jar）后 **WI-1 位点消失**：
  - ~43 `total(DISTINCT x)` 期望 `1` 不再实际 `0`；
  - ~86 `avg(x)` 期望 `1` 不再实际 `0`；
  - ~380 `total(x)` / `label-sum` 期望 `5` 与冲突消失；
  - ~390 `total(DISTINCT x)` / `label-sum-distinct` 期望 `3` 与冲突消失。
- 允许仍失败（非本项完成条件）：~480/484 overflow（WI-2）；~491 精度漂移（WI-3）。整文件 `failed=0` **不是**硬验收。
- 另跑：`mvn -q test -Dtest=ValueNormalizerTest`；`mvn -q clean test`；可选 `mvn -q -DskipTests package`。
- 完成条件：证据写入 `dev-notes.md`；L2 绿或 quality.md §6 记缺口。

### T4 — 开发产物

- 做：本目录 `dev-notes.md`（实现摘要、red→green、WI-1 消失证据、WI-2/WI-3 仍失败、§6 若有）。
- 完成条件：Reviewer/QA 可凭 plan + notes 复现；未擅自 commit。

## 依赖与顺序

T0 → T1（red）→ T2（green）→ T3 → T4 → Review → QA。禁止 T2 先于 T1。

## 触碰路径

| 任务 | 预期路径 |
|---|---|
| T0 | 分支 `fix-normalize-integer-float` |
| T1 | `src/test/java/com/ggtest/normalize/ValueNormalizerTest.java` |
| T2 | `src/main/java/com/ggtest/normalize/ValueNormalizer.java` |
| T3 | 命令 + notes；语料只读 `sqllogictest/test/evidence/slt_lang_aggfunc.test` |
| T4 | `workflow/archive/2026/fix-normalize-integer-float/dev-notes.md` |
| 禁止 | Spec/Design；`workflow/docs/manager/*`；`STATUS.md`；WI-2/WI-3 实现；`pom.xml` 无关改动；`sqllogictest/` 入库；本轮 commit（除非用户授权） |

## 验收

（fast / 无 Spec）

| ID | 要求 | 证据 |
|---|---|---|
| A1 | `"1.0"`/`"5.0"` 等浮点样字符串归一为对应整数文本 | `ValueNormalizerTest` Pass |
| A2 | 非整数数值向零截断（`"1.25"`→`"1"`，`"-1.9"`→`"-1"`） | 同上 |
| A3 | 真正非数值 → `"0"`；纯整数 / NULL 不回归 | 同上 |
| A4 | `slt_lang_aggfunc.test` WI-1 位点（~43/~86/~380/~390 及 label 冲突）消失 | CLI 无对应 mismatch/label conflict |
| A5 | 范围仅本缺陷；WI-2/WI-3 位点可仍失败 | diff 限于 normalizer + 对应测试 + 本目录文档；notes 标明仍失败位点 |

## 验证

### 命令

```bash
# T1 red / T2 定点
mvn -q test -Dtest=ValueNormalizerTest

# L2 全量
mvn -q clean test

# 可选
mvn -q -DskipTests package

# A4：WI-1 位点应消失；~480/484、~491 可仍失败
./bin/ggtest ./sqllogictest/test/evidence/slt_lang_aggfunc.test
# 或 java -jar target/ggtest-*.jar ./sqllogictest/test/evidence/slt_lang_aggfunc.test
```

### 最低验证层理由

单点归一化纠偏：单测锁定 `%d`/截断 + 全量 Maven 防回归 → **L2**。定点语料冒烟证明 JDBC 浮点样路径；整文件零失败属 WI-2/WI-3，不抬升为 L4。

### 预期证据

| 验证 | 通过时 |
|---|---|
| T1 red | 新/改测 Fail，指向 `parseLong` 或错误 `"1.5"`→`"0"` 期望 |
| `ValueNormalizerTest` | Failures/Errors = 0 |
| `mvn -q clean test` | BUILD SUCCESS；Failures=0（Skipped 记 notes） |
| A4 CLI | 无 ~43/~86/~380/~390 的 `0` mismatch 与对应 label conflict；可仍见 ~480/484 overflow、~491 精度差 |
| package（若跑） | BUILD SUCCESS |

### 无法验证（quality.md §6）

缺 Java 17/Maven、或 `bin/ggtest`/JAR、或语料路径不可用时：`dev-notes.md` 记「未验证项 → 原因 → 风险 → 恢复条件 → 复测范围」。禁止静默跳过 A4；至少保留 A1–A3 单测作代理证据，并标明语料冒烟待补。

## Review 门禁与进入 QA

- Review：required。
- 进入 QA：T0–T4 完成；L2 绿（或已记不可执行项且 A1–A3 有代理证据）；`dev-notes.md` 含 red→green 与 A4（或 §6）；Reviewer **Approve**。

## 文档影响

| 类别 | 更新路径或 N/A |
|---|---|
| 开发文档 | 本目录 `dev-notes.md`（必写）；必要时 `ValueNormalizer` Javadoc；本 `plan.md` |
| 用户文档 | N/A（纠偏既有 `%d` 合同；默认不改 README） |
| 运维文档 | N/A（无部署/排障变更） |

## 交接顺序

1. Planner：本 plan → 用户确认 → Manager 持久化确认后置 `planned`（Planner 不改状态、不调度 Developer）。
2. Developer：分支 `fix-normalize-integer-float` 实施 T0–T4；写 notes；不 commit（除非用户另要求）。
3. Reviewer：对照 plan + A1–A5 → Approve / 回退。
4. QA：独立复跑验证与 A4 → `qa-report.md` Pass/Fail/Blocked（不得因 WI-2/WI-3 仍失败否定本项）。
5. 合并：仅用户授权后由 Manager 流程处理。

## 开放问题

无。

## 修订记录

| 日期 | 摘要 |
|---|---|
| 2026-08-06 | 初稿并 refine-docs：T0–T4 TDD；ValueNormalizer(+Test)；L2 + aggfunc WI-1；排除 WI-2/WI-3；Review required |
