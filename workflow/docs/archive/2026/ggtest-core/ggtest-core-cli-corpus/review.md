# Review: ggtest-core / cli-corpus

> **工作项**：`ggtest-core` · **sub-feature-id**：`cli-corpus`  
> **路径**：full · **Review 门禁**：required · **Design**：N/A（skipped）  
> **当前审阅版本**：`4b9604f`（DEF-CLI-001 修复；相对 `466c6f1`）；分支 `ggtest-core-cli-corpus` → `main`  
> **依据**：[`spec.md`](./spec.md)、[`plan.md`](./plan.md)、[`dev-notes.md`](./dev-notes.md)、[`qa-report.md`](./qa-report.md) 轮次 2；`documentation.md` / `quality.md` / `security.md` / `git.md`

## 审阅轮次

| 轮次 | 日期 | 实现版本 | 范围 | 结论 |
|---|---|---|---|---|
| 1 | 2026-07-25 | `466c6f1`（docs `0ed8a95`） | Plan T1–T6 初审；硬验收语料不可得 | **Approve**（硬验收未执行，非豁免） |
| 2 | 2026-07-25 | `4b9604f` | QA 轮次 2 Fail / **DEF-CLI-001** 修复复审 | **Approve** |

---

## 轮次 2 — DEF-CLI-001 复审（当前）

### 结论

**Approve**

无阻塞项；DEF-CLI-001 已按 Plan T3 修复并通过独立硬验收。可进 QA 追加回归。无默示豁免。

### 审阅范围

- 修复：`CliSession` 每文件 JDBC open → run → close；fixtures `cross-file/schema-a|b.test`；`laterFileIsNotPollutedByEarlierDatabaseSchema`；`dev-notes.md` 回执
- 对照：`qa-report.md` 轮次 2（批量 `failed=4151`、`table t1 already exists`）；Plan T3；Spec P1-5 / Q8
- 未改：Spec/Plan；上游行为；Manager STATUS

### DEF-CLI-001 对齐核对

| 要求 | 证据 | 结果 |
|---|---|---|
| Plan T3：每文件 JDBC → run → **关闭连接** | `runOneFile` 内 `try (Connection …)` | 通过 |
| Spec 跨文件重置（hash-threshold / 条件 / label） | 每文件新建 `SqlLogicTestRunner(…, options.hashThreshold())`；本轮未回退 | 通过 |
| Spec P1-5：批量失败数=0、退出码=0、零豁免 | 独立 CLI：分文件+`TOTAL failed=0`、`passed=5413`、exit 0；无 `already exists`；未 skip/豁免 | 通过 |
| 回归测试可失败性 | `schema-a`/`schema-b` 均 `CREATE TABLE t1`；共享连接必败；现 exit 0 | 通过 |

### 独立验证

```text
OpenJDK 17.0.20；Maven 3.9.16
GGTEST_CORPUS_DIR=/Users/zhougangjie/Space/sqllogictest/test
HEAD=4b9604f

GGTEST_CORPUS_DIR=… mvn -q clean test
→ BUILD SUCCESS；Tests run: 111, Failures: 0, Errors: 0, Skipped: 1
  （Skipped：Manifest @ clean test 无 JAR；CorpusHardAcceptance×2 执行通过）

mvn -q clean package → BUILD SUCCESS

P0-1: ggtest --url jdbc:sqlite::memory: select1.test
→ exit 0；TOTAL: passed=1031 failed=0 skipped=0

P1-5: 同 URL 批量 select1+select2+select3
→ exit 0；FILE failed=0（1031/1031/3351）；TOTAL: passed=5413 failed=0
→ 无 already exists
```

### 测试有效性

- `laterFileIsNotPollutedByEarlierDatabaseSchema` 覆盖 QA 首败形态（跨文件同名表）。
- L4：`CorpusHardAcceptanceTest` 在语料环境下执行并通过，与手工 P0-1/P1-5 一致。
- `MainOrchestrationTest` 10 例（含跨文件阈值+schema）；上游无新增失败。
- 已独立复跑，非仅复述 Developer 回执。

### 文档影响核对

| Plan 声明 | 一致 | 备注 |
|---|---|---|
| 开发：README、Javadoc、`dev-notes.md` | 是 | 含 DEF-CLI-001 回执与 P0-1/P1-5 证据 |
| 用户：README CLI/退出码/语料自备 | 是 | 本修复未改用户面合同 |
| 运维 N/A | 是 | |

### 安全影响核对

范围：`--user`/`--password`/JDBC URL；用户 SQL；目录只读；本轮仅连接生命周期。

| 检查项 | 结果 | 备注 |
|---|---|---|
| 敏感信息 | 无发现 | 无新凭据；报告不含口令 |
| 官方大语料入库 | 无 | 仅自造 schema fixtures |
| 输入/文件/依赖 | 可接受 | 无新依赖；每文件关闭连接 |
| 处置状态 | 无需处置 | 允许进入 QA |

### Git 合规

| 检查项 | 结果 |
|---|---|
| 工作分支 | `ggtest-core-cli-corpus`（非 main） |
| 修复提交 | `4b9604f` `fix(cli): isolate JDBC connection per test file`（DEF-CLI-001） |
| 禁止提交项 | 无密钥/官方大语料 |
| 越界 | 未改 Spec/Plan/上游行为/Manager STATUS |

### 必修项

无。

### 非阻塞建议

| ID | 位置 | 建议 |
|---|---|---|
| N1–N4 | 见轮次 1 | 仍可选；不阻断 QA |
| N5 | 文件型 SQLite URL | 重连同一路径不清空磁盘库；硬验收与 `:memory:` fixtures 已满足 Spec/Plan。若文档化「批量+文件库」，可明示共享持久库 vs 每文件空白库 |

### 后续动作

1. Manager：调度 QA 追加回归（必测 P1-5、`mvn test`/`package`、schema fixtures；回归 P0-1/P1-1/P1-6/退出码）。
2. QA：独立确认 DEF-CLI-001 关闭；禁止默示豁免。
3. Developer：无必修；N1–N5 可选。
4. 复审：无（Approve）；QA 再 Fail 再审。

---

## 轮次 1 — 初审（历史）

> **版本**：`466c6f1`（docs `0ed8a95`）· Plan T1–T6  
> **结论**：**Approve**（P0-1/P1-5 **未执行**，非通过、非豁免）

范围：`com.ggtest.cli`、`bin/ggtest`、`pom.xml`、cli 测试/fixtures、README/Javadoc/`dev-notes`；提交 `0ed8a95`/`466c6f1`；未改上游。

| 要求 | 结果 |
|---|---|
| CLI/参数/收集/报告/退出码 0/1/2 | 通过 |
| 跨文件 hash-threshold；P1-1/P1-6 | 通过 |
| P0-1/P1-5 | **未执行**（无语料） |

独立验证：`mvn clean test` → 110/0（Skipped 3）；`package` SUCCESS；fixtures 退出码 0/1/2。

硬验收缺口：语料不可得 → 官方失败数=0 未证 → 恢复后补跑。QA 轮次 2 语料到位后暴露 **DEF-CLI-001**（共享连接跨文件污染）→ 见轮次 2。

摘要：fixtures/文档/安全/Git 当时合规；非阻塞 N1–N4（`CliOptions.toString` 脱敏、`sanitize` 加固、engine 接线、编排断言收紧）。
