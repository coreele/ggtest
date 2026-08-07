# Plan: fix-shared-defaults

> 实施与验证计划。fast；无 Spec / Design。
>
> **适用对象**：Developer、QA、Manager。  
> **前置条件**：源分支 `fix-shared-defaults`；JDK 17+、Maven 3.8+。  
> **阅读顺序**：元信息 → 目标 → 任务 → 依赖 → 触碰路径 → 验证 → 验收 → 文档影响 → 交接。  
> **预期结果**：两常量各一处字面量；行为不变；`mvn -q clean test` 退出码 0。  
> **失败处理**：见「无法执行验证时的处理」。  
> **确认**：用户已授权 Manager 覆盖确认（2026-07-26）；持久化后可设 `planned`。

## 元信息

- 工作项标识: fix-shared-defaults（未拆分；sub-feature-id = feature-id）
- 依据 Spec: N/A（Spec 门禁 skipped）
- 依据 Design: N/A（Design 门禁 skipped）；分层以 [architecture-overview/design.md](../architecture-overview/design.md) 为准（**禁止** `parser` → `normalize`）
- 路径等级: fast
- Review 门禁: **skipped**（fast；工作项已标）
- 最低验证层: **L2**（单元测试 + 全量构建）
- 验证命令: `mvn -q clean test`
- 源分支: `fix-shared-defaults` → `main`
- 关联: 审计 Finding §4 + Tech Debt；CA-004、CA-005

## 适用工程规范

- [文档工程](../../standards/documentation.md)
- [Git 协作](../../standards/git.md)
- [质量与验证](../../standards/quality.md)
- [安全](../../standards/security.md)

## 目标摘要

收敛重复默认常量，消除漂移；**不改变**默认值与运行时行为。

| 常量 | 权威字面量落点 | 引用方 |
|---|---|---|
| `DEFAULT_HASH_THRESHOLD = 8` | **`ResultComparer`**（唯一字面量） | 删除 `CliArgumentParser` 副本；`RuntimeConfigResolver` → `ResultComparer.DEFAULT_HASH_THRESHOLD` |
| `DEFAULT_COLUMN_SEPARATOR = " "` | **`com.ggtest.model.SqlLogicDefaults`**（唯一字面量） | `SqlLogicTestParser` → `SqlLogicDefaults`；`ResultComparer.DEFAULT_COLUMN_SEPARATOR` **转发**（保持公开 API） |

**分层说明：** 工作项希望 parser 引用 `ResultComparer`，但架构禁止 `parser`→`normalize`。故 column 字面量落在 `model`（二者均可依赖）；hash 仅 CLI/runner 使用，权威留在 `ResultComparer`。

依据：`agents/docs/manager/fix-shared-defaults.md`；`agents/docs/audit/2026-07-26-src.md`。

## 任务拆解

### T1 — `DEFAULT_HASH_THRESHOLD`（CA-004）

1. 保留 `ResultComparer.DEFAULT_HASH_THRESHOLD = 8` 为唯一字面量。  
2. 删除 `CliArgumentParser.DEFAULT_HASH_THRESHOLD`。  
3. `RuntimeConfigResolver` 默认回退改为 `ResultComparer.DEFAULT_HASH_THRESHOLD`（加 import）。  

**完成条件：** 除 `ResultComparer` 外无该常量定义；默认回退引用正确。

### T2 — `DEFAULT_COLUMN_SEPARATOR`（CA-005）

1. 新增 `com.ggtest.model.SqlLogicDefaults`，含唯一字面量 `DEFAULT_COLUMN_SEPARATOR = " "`。  
2. `ResultComparer.DEFAULT_COLUMN_SEPARATOR` 改为 `= SqlLogicDefaults.DEFAULT_COLUMN_SEPARATOR`（无第二字面量）。  
3. 删除 `SqlLogicTestParser` 私有常量；引用 `SqlLogicDefaults.DEFAULT_COLUMN_SEPARATOR`。  

**完成条件：** 字面量 `" "` 仅在 `SqlLogicDefaults`；parser **无** `com.ggtest.normalize` import；`ResultComparer.DEFAULT_COLUMN_SEPARATOR` 仍为 `" "`。

### T3 — 静态核对

`rg` 确认：`DEFAULT_HASH_THRESHOLD\s*=` 仅 `ResultComparer`；`DEFAULT_COLUMN_SEPARATOR\s*=\s*" "` 仅 `SqlLogicDefaults`。  

**完成条件：** 与权威落点一致。

### T4 — 验证

执行 `mvn -q clean test`；摘要写入 `dev-notes.md`。  

**完成条件：** 退出码 0；测试全绿。

### T5 — 登记册

`agents/docs/standards/code-audit-register.md`：CA-004、CA-005 → `resolved`，注明权威落点。  

**完成条件：** 登记册与实现一致。

## 依赖与顺序

```text
T1 ──┐
T2 ──┼──► T3 ──► T4 ──► T5
（T1、T2 可并行）
```

**禁止：** 改默认值；parser→normalize；新增业务行为；提交真实 `.env`、无关重构。

## 触碰路径

| 任务 | 路径 |
|---|---|
| T1 | `src/main/java/com/ggtest/cli/CliArgumentParser.java`；`RuntimeConfigResolver.java` |
| T2 | **新增** `src/main/java/com/ggtest/model/SqlLogicDefaults.java`；`ResultComparer.java`（转发）；`SqlLogicTestParser.java` |
| T3 | `src/`（只读） |
| T4 | Maven 套件；`agents/docs/features/fix-shared-defaults/dev-notes.md` |
| T5 | `agents/docs/standards/code-audit-register.md` |

**不改行为合同：** `SqlLogicTestRunner`、既有测试期望值（除非仅因删除 `CliArgumentParser` 常量而改引用）。

## 验证

| 项 | 内容 |
|---|---|
| 最低验证层 | **L2** — 无新行为；既有套件覆盖默认 threshold 与比对/解析路径 |
| 验证命令 | `mvn -q clean test` |
| 预期证据 | 退出码 0；全部 Pass；T3 grep 符合权威落点 |

### 无法执行验证时的处理

| 未验证项 | 原因 | 风险 | 恢复条件 |
|---|---|---|---|
| T4 | JDK/Maven/网络不可用 | 引用断裂或默认值漂移未检出 | 环境恢复后重跑 `mvn -q clean test` 得 0 |
| T4 失败 | 引用遗漏或意外行为变更 | 错误默认进入主干 | 修复后重跑 T3–T4；记入 `dev-notes.md` |

**禁止**静默跳过。

## 验收

| ID | 条件 | 证据 |
|---|---|---|
| A1 | hash 字面量仅在 `ResultComparer` | T3 |
| A2 | column 字面量仅在 `SqlLogicDefaults`；`ResultComparer` 为转发 | T3 |
| A3 | CLI 默认 hash threshold 仍为 8 | 既有 CLI/`RuntimeConfigResolver` 测 + T4 |
| A4 | parser 默认列分隔符仍为 U+0020 | 既有 parser 测 + T4 |
| A5 | `SqlLogicTestParser` 无 `normalize` import | 源码 / T3 |
| A6 | `mvn -q clean test` 退出码 0 | T4 |
| A7 | CA-004/CA-005 为 `resolved` | T5 |

## Review 门禁与进入 QA

- Review：**skipped**；无需 Approve。  
- 进入 QA：T1–T5 完成；`dev-notes.md` 含 T4 证据。

## 文档影响

| 类别 | 更新路径或 N/A |
|---|---|
| 开发文档 | `dev-notes.md`；`code-audit-register.md`（CA-004/CA-005）。README：**N/A**（已指向 `ResultComparer`） |
| 用户文档 | **N/A** — 无用户可见变更 |
| 运维文档 | **N/A** — 无部署/排障变更 |

## 交接顺序

1. Manager 持久化 Plan 确认 → `planned` → Developer。  
2. Developer：T1–T5；写 `dev-notes.md`。  
3. Review：跳过。  
4. QA：独立复验 A1–A7；`qa-report.md`。  
5. 合入：用户授权后；合入前停合并授权（工作项已记）。Planner 不改状态。

## 修订记录

| 日期 | 摘要 |
|---|---|
| 2026-07-26 | 初稿；修正 column 落点为 `model.SqlLogicDefaults`（避免 parser→normalize）；精简 |
