# Review: ggtest-core / normalize

> **工作项**：`ggtest-core` · **sub-feature-id**：`normalize`  
> **路径**：full · **Review 门禁**：required · **Design**：skipped（无 design.md）  
> **审阅版本**：`07a8e51`（分支 `ggtest-core-normalize` → `main`；Plan T1–T6）  
> **依据**：[`spec.md`](./spec.md)、[`plan.md`](./plan.md)、[`dev-notes.md`](./dev-notes.md)；`documentation.md` / `quality.md` / `security.md` / `git.md`

## 审阅范围

- 实现：`com.ggtest.normalize`（`ValueNormalizer`、`ResultSorter`、`ResultHasher`、`ResultComparer`）；`pom.xml` description
- 测试：`src/test/java/com/ggtest/normalize/*`；`src/test/resources/fixtures/normalize/*`
- 文档：`README.md`、公共 Javadoc、`dev-notes.md`
- Git：源分支 `ggtest-core-normalize`；提交 `07a8e51`

## 结论

**Approve**

无阻塞项；满足 Spec/Plan 与进入 QA 条件。建议 Manager 调度 QA。

## 实现正确性

| 要求 | 证据 | 结果 |
|---|---|---|
| P0-2 哈希兼容 | fixture `p0-2-select1-hash.txt`：30 个规范化 I 值 → MD5 `3c13dee48d9356ae19af2515e05e6b54`；threshold 8 哈希形态比对通过 | 通过 |
| P0-4 I/NULL、R/`%.3f`、T/`(empty)` | 验收三用例 `passed`；单元测试覆盖控制字符→`@`、不可解析 I→`0`、R→`0.000` | 通过 |
| P0-5 rowsort / nosort | 行序错位：`ROWSORT` pass、`NOSORT` fail；`diffSummary` 非空且含 expected/actual | 通过 |
| P1-3 valuesort | 期望 `1\n2\n3\n4` 比对通过 | 通过 |
| Plan T1–T4 | 无 JDBC；`DEFAULT_HASH_THRESHOLD=8`；threshold ≤ 0 全量文本；规范化→排序→（可选）哈希→通过/失败 | 通过 |
| 不越界 | 未改 parser；无 design.md；无 JDBC/SQL 执行 | 通过 |

独立验证：

```text
JAVA_HOME=/opt/homebrew/opt/openjdk@17/.../Contents/Home
PATH=~/tools/apache-maven-3.9.16/bin:$PATH
mvn clean test
→ BUILD SUCCESS
→ Tests run: 36, Failures: 0, Errors: 0, Skipped: 0
  （normalize 26 + parser 10）
```

## 测试有效性

- 覆盖 P0-2/P0-4/P0-5/P1-3 与 Plan T1–T5；断言可因错误 MD5、规范化、排序或差异摘要缺失而失败。
- 失败路径：`ResultComparerTest.failureDiffSummaryIsNonEmpty`；P0-5 nosort。
- 边界：threshold ≤ 0；I/R 不可解析；T 控制/非 ASCII；三 `SortMode`。
- L2 达标；`mvn clean test` 与 Plan 一致；parser 无回归。
- 非阻塞缺口：`p0-4-normalize-cases.txt` 未加载（P0-4 已内联覆盖）；`size == hashThreshold` 无专项用例（Spec「超过」；实现 `size > threshold`）。

## 文档影响核对

| Plan 声明 | 一致 | 备注 |
|---|---|---|
| 开发文档：`README.md` + Javadoc + `dev-notes.md` | 是 | 构建/测试命令、`ResultComparer` 示例、Javadoc、L2 证据 |
| 用户文档 N/A | 是 | 无 CLI |
| 运维文档 N/A | 是 | 无部署面 |

## 安全影响核对

检查范围：内存文本/值规范化与 MD5；classpath fixtures 只读。无认证授权、网络、写回、SQL/命令执行。

| 检查项 | 结果 | 备注 |
|---|---|---|
| 敏感信息 | 无发现 | 代码/测试/文档/fixtures/提交无凭据 |
| 认证与授权 | N/A | — |
| 输入与外部访问 | 可接受 | 仅调用方传入值；不连库；不执行 SQL |
| 文件操作 | 可接受 | 测试 classpath 只读；无路径遍历 |
| 依赖变更 | 可接受 | 无新生产依赖；`pom.xml` 仅 description |
| 处置状态 | 无需处置 | 允许进入 QA |

## Git 合规

| 检查项 | 结果 |
|---|---|
| 工作分支 | `ggtest-core-normalize`（非 main） |
| 提交 | `07a8e51` Conventional Commits；范围限 normalize + README/dev-notes/pom description |
| 禁止提交项 | 无密钥/`.env`/构建产物；`.gitignore` 含 `target/` |
| 越界 | 提交未改 parser |

## 必修项

| ID | 位置 | 问题 | 状态 |
|---|---|---|---|
| — | — | 无 | — |

> 阻塞项须用 `Request changes`；本报告无阻塞项。

## 非阻塞建议

| ID | 位置 | 建议 |
|---|---|---|
| N1 | `fixtures/normalize/p0-4-normalize-cases.txt` | 验收加载或删除，避免双源 |
| N2 | `ResultComparer.splitExpectedLines` | `parseHashExpectation` 未参与比对，可简化或用于校验 `N` |
| N3 | `ResultComparerTest` | 可选：`valueCount == hashThreshold` 仍走全量文本 |

## 后续动作

1. Manager：调度 **QA**（`qa-report.md`）。
2. QA：P0-2/P0-4/P0-5/P1-3；`mvn -q clean test`（JDK 17）；含 parser 回归。
3. Developer：N1–N3 可选，不阻断 QA。
4. 复审：无（Approve；QA Fail 修复后再审）。
