# Plan: fix-cli-credential-redaction

> 实施与验证计划。依据 [spec.md](./spec.md)（用户 **approved**）；Design 门禁 skipped。
>
> **适用对象**：Developer、Reviewer、QA、Manager。
> **前置条件**：工作项路径 `standard`；源分支 `fix-cli-credential-redaction`；目标 `main`；Java 17 + Maven。
> **阅读顺序**：元信息 → 目标摘要 → 任务拆解（TDD）→ 依赖 → 触碰路径 → 验证 → 冲突协调 → 验收 → 文档影响 → 交接。
> **预期结果**：`CliSession` 脱敏控制点与 `CliOptions.toString()` 不泄露 URL userinfo / password 明文；红/绿泄露证明测试；`mvn -q clean test` 绿。
> **失败处理**：按「验证」节定位；无法执行时记录原因/风险/恢复条件。
>
> **Plan 确认**：用户已授权 Manager 覆盖确认本 Plan；Manager 持久化确认后可将状态设为 `planned` 并调度 Developer。Planner 不自行改状态。

## 元信息

- 工作项标识: fix-cli-credential-redaction（未拆分；sub-feature-id = feature-id）
- 依据 Spec: [workflow/workflow/docs/features/fix-cli-credential-redaction/spec.md](./spec.md)（Spec 门禁 required；用户确认 **approved**）
- 依据 Design: N/A（Design 门禁 skipped）
- 路径等级: standard
- Review 门禁: **required**（进入 QA 前须 Reviewer **Approve**）
- 最低验证层: **L2**（新增单元测试覆盖脱敏控制点 + 全量 `mvn test` 回归；无新对外 CLI 合同）
- 验证命令: `mvn -q clean test`
- 源分支 / 目标: `fix-cli-credential-redaction` → `main`
- 审计依据: `workflow/workflow/docs/audit/2026-07-26-src.md` Finding §7；登记册 **CA-002**

## 适用工程规范

- [文档工程](../../standards/documentation.md)
- [Git 协作](../../standards/git.md)
- [质量与验证](../../standards/quality.md)
- [安全](../../standards/security.md)

## 目标摘要

- **问题（CA-002）**：`CliSession.sanitize`（约 L392–395）注释承诺脱敏，实现仅 `strip()`；`CliOptions.toString`（约 L37–54）对 `password` 已掩码 `***`，但 `url` 含 `user:pass@` 时仍明文输出。
- **方案**：在脱敏控制点替换 URL **userinfo** 与（当次运行已配置时）**password 字面量**；`toString` 对 `url` 做同等 userinfo 掩码；内存中 `url`/`password` 保持明文供连接使用。
- **TDD**：先写 P0 泄露证明测试（当前实现须 **红**），再实现脱敏至 **绿**；P1 回归不重写既有 password 不出 stdout/stderr 路径测试。
- **非目标**：见 spec.md「非目标」；禁止改 JDBC 执行器、parser、runner 语义或非脱敏报告格式。

## 任务拆解（TDD）

### T1 — 红：泄露证明测试（P0-1 / P0-2 / P0-3 / P0-4）

在 **未改生产脱敏逻辑** 前提下新增/扩展测试，覆盖 spec P0；运行 `mvn -q clean test` 时 **新用例须失败**（证明曾泄露）。

1. **新建** `src/test/java/com/ggtest/cli/CredentialRedactionTest.java`（同包 `com.ggtest.cli`）：
   - **P0-1**：经 `CliSession` 脱敏控制点处理含 `jdbc:postgresql://alice:secretPass@host/db` 或等价 userinfo 的消息 → 断言结果 **不含** `secretPass`；仍含可识别非凭据片段（如 `host`、错误前缀）。
   - **P0-2**：构造 `CliOptions` 含 `password=super-secret-credential`，消息含该字面量 → 经同一控制点 → **不含** `super-secret-credential`。
   - **P0-4 注释/断言**：用例须在 strip-only 实现上失败（Developer 在 T1 完成后自证红态：至少 P0-1 与 P0-3 相关用例 fail）。
2. **扩展** `src/test/java/com/ggtest/cli/RuntimeConfigResolverTest.java`（或同文件新 `@Test`）：
   - **P0-3**：`CliOptions` 的 `url` 含 `alice:bob@`，`password` 为某明文 → `toString()` **不含** `bob` 与 password 明文；仍含 `password=Optional[***]`（或等价 `***` 掩码）。
3. **触达控制点（T1 允许的最小生产改动）**：`sanitize` 现为 `private static` 且无 password 上下文；P0-2 需会话级 password。可改为 **package-private 实例方法** 或 **package-private** 共享 helper——**禁止** public 测试 API。
   - **完成条件**：strip-only / 原样 `url` 的 `toString` 上，新用例失败。

### T2 — 绿：`CliSession` 脱敏控制点（P0-1 / P0-2 / P1-1）

1. 实现 URL userinfo 替换（稳定占位，如 `://***@` 或 `***@`；同一输入多次调用结果一致）。
2. 当 `options.password()` 有值时，对消息做该明文字面量替换（无 password 配置时不做字面量规则，仍须 URL userinfo 脱敏）。
3. 保留 `null → ""` 与首尾 `strip()`；脱敏 **不得** 抛异常掩盖原失败（spec 错误约束）。
4. **P1-1**：无 userinfo、无 password 字面量的普通消息，脱敏后除 strip 外语义不变。
5. **完成条件**：T1 中 P0-1、P0-2 用例通过；实现可保持 `sanitize` 私有并委托 package-private helper（与 spec API 合同一致）。

**建议**：`CliSession` 与 `CliOptions` 共用 package-private userinfo/password helper，避免规则漂移。

### T3 — 绿：`CliOptions.toString` URL userinfo（P0-3）

1. `toString()` 输出中的 `url` 字段经与 T2 相同的 userinfo 掩码；`password` 字段行为不变（有值 → `***`）。
2. 无 userinfo 的 URL（如 `jdbc:sqlite::memory:`）保持主机/库路径可读。
3. **完成条件**：T1 P0-3 用例通过；T2+T3 后 P0-4 绿态成立。

### T4 — 回归与全量验证（P1-2）

1. 运行 `mvn -q clean test`；确认 **Failures/Errors = 0**。
2. **P1-2**：不重写既有用例，但须确认仍绿——至少 `MainOrchestrationTest.passwordIsNeverPrintedInOutput`、`EnvConfigIntegrationTest.passwordFromEnvFileNeverPrinted`、`RuntimeConfigResolverTest.cliOptionsToStringRedactsPassword`。
3. **完成条件**：全量套件绿；无 PG 门控环境时 PG skip 非 fail（与现有一致）。

### T5 — 验证回执与登记册

1. 新建 `workflow/workflow/docs/features/fix-cli-credential-redaction/dev-notes.md`：记录 TDD 红/绿证据、`mvn -q clean test` 摘要。
2. 将 `workflow/workflow/docs/standards/code-audit-register.md` **CA-002** 标为 `resolved`。
3. **完成条件**：dev-notes 与 CA-002 已更新；Ready for Review。

## 依赖与顺序

```text
T1（红）→ T2 ∥ T3（可同 commit，T3 依赖 T2 的 userinfo 规则一致）→ T4 → T5
```

- T2 与 T3 可同一 Developer 连续提交；共享 helper 应先于或同步于 `toString` 改动。
- 与 `fix-pg-teardown-once` **无代码依赖**，仅有 `CliSession.java` 合并冲突风险（见下）。

## 触碰路径

| 路径 | 变更 |
|---|---|
| `src/main/java/com/ggtest/cli/CliSession.java` | 脱敏控制点（`sanitize` / 实例化 / 委托 helper）；**不**改 `runPostgresFile` 控制流 |
| `src/main/java/com/ggtest/cli/CliOptions.java` | `toString()` 对 `url` userinfo 掩码 |
| `src/main/java/com/ggtest/cli/`（可选） | package-private 共享脱敏 helper（新建或内嵌） |
| `src/test/java/com/ggtest/cli/CredentialRedactionTest.java` | **新建** P0-1/2/4 泄露证明 |
| `src/test/java/com/ggtest/cli/RuntimeConfigResolverTest.java` | **扩展** P0-3（或并入上表测试类） |
| `workflow/workflow/docs/features/fix-cli-credential-redaction/dev-notes.md` | Developer 验证回执（T5） |
| `workflow/workflow/docs/standards/code-audit-register.md` | CA-002 → `resolved`（T5） |

**禁止触碰**：`runPostgresFile` teardown 路径（留给 `fix-pg-teardown-once`）、JDBC 执行器、parser、normalize、runner、`examples/` 未跟踪语料、真实 `.env`。

## 与 fix-pg-teardown-once 的冲突 / 协调

| 项 | 策略 |
|---|---|
| 重叠文件 | 二者均改 `CliSession.java` |
| **本项优先面** | `sanitize` / 脱敏 helper、`CliOptions.toString`、脱敏测试 |
| **对方限定面** | **仅** `runPostgresFile`（约 L187–226）；其 Plan 禁止动 `sanitize` |
| 并行开发 | 可并行；**本项（脱敏）优先合入 `main`**，`fix-pg-teardown-once` rebase；或同批合入时人工合并 |
| 冲突解决 | 保留双方意图：单次 finally teardown（对方）+ 真正脱敏（本项），互不覆盖 |
| 后置 | `refactor-cli-session-boundaries` 等本项与 teardown 项落地后再拆 |

## 验证

| 项 | 内容 |
|---|---|
| 最低验证层 | L2：脱敏为纯字符串变换 + 既有集成测回归；全量单元/集成 `test` 足够 |
| 命令 | `mvn -q clean test` |
| TDD 预期证据 | **T1 后**：新用例 Fail（含 userinfo/password 断言）；**T2–T4 后**：BUILD SUCCESS，Failures/Errors = 0 |
| Review 额外关注 | 测试非恒真；占位符稳定；security.md §1 无真实凭据入仓 |

### 无法执行验证时

| 未验证项 | 原因 | 风险 | 恢复条件 | 复测范围 |
|---|---|---|---|---|
| `mvn -q clean test` | 缺 JDK/Maven 或依赖拉取失败 | 泄露回归未证实 | 恢复工具链后重跑 | 全量 `mvn -q clean test` |
| PG 门控集成 | 无 `GGTEST_PG_*` | 实库路径未实跑（与本项无直接 P0 依赖） | 提供 PG 后可选重跑 | 既有 PG 门控测 |

**禁止**静默跳过未记录的验证缺口。

## 验收

> 完整合同见 [spec.md](./spec.md) P0/P1。

| ID | 验收要点 | Plan 证据 |
|---|---|---|
| P0-1 | sanitize 脱 URL userinfo | T1/T2 + `CredentialRedactionTest` |
| P0-2 | sanitize 脱已配置 password 字面量 | T1/T2 |
| P0-3 | `CliOptions.toString` 脱 url userinfo | T1/T3 |
| P0-4 | 修复前测试失败、修复后通过 | T1 红 + T2–T3 绿 |
| P1-1 | 普通消息除 strip 外不变 | T2 |
| P1-2 | 既有 password 不出输出测不回归 | T4 |

## 文档影响

| 类别 | 更新路径或 N/A 理由 |
|---|---|
| 开发文档 | `workflow/workflow/docs/features/fix-cli-credential-redaction/dev-notes.md`（TDD 与验证回执）；`workflow/workflow/docs/standards/code-audit-register.md` CA-002 → `resolved` |
| 用户文档 | N/A — CLI 参数、退出码、`.env` 键名与用法不变 |
| 运维文档 | N/A — 无部署/排障步骤变更 |

## Review 门禁与进入 QA

- Review 门禁：**required**（standard）。
- Reviewer 须检查：测试有效性（含红/绿证明）、文档影响、安全影响（凭据不出现在测试/fixture/文档）。
- **进入 QA 条件**：T1–T5 完成；`mvn -q clean test` 绿；dev-notes 与 CA-002 已更新；Review 结论 **Approve**。
- 合入：须用户合并授权（工作项已注明合入前停合并授权）。

## 交接顺序

1. **Developer**：T1→T2→T3→T4→T5；`dev-notes.md`；CA-002 → `resolved`。
2. **Reviewer**：测试/安全/文档影响 → `review.md`（**Approve** 后方可 QA）。
3. **QA**：依据 spec P0/P1 与 Plan 验证 → `qa-report.md`（Pass/Fail/Blocked）。
4. **Manager**：用户合并授权后按 git/quality 流转 `done`（不等待合入完成写 `done`）。

## 修订记录

| 日期 | 摘要 |
|---|---|
| 2026-07-26 | TDD 脱敏 Plan；L2 + `mvn -q clean test`；与 fix-pg-teardown-once 协调 |
