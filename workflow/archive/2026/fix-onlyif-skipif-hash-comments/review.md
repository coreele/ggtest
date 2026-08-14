# Review: fix-onlyif-skipif-hash-comments

## 审阅范围

- 工作项: `fix-onlyif-skipif-hash-comments`（未拆分；路径 fast；Review 门禁 required）
- 依据: `workflow/archive/2026/fix-onlyif-skipif-hash-comments/fix-onlyif-skipif-hash-comments.md`；`plan.md`（T0–T4，A1–A5）；`dev-notes.md`；`workflow/agents/standards/{documentation,quality,security,git}.md`
- Spec / Design: N/A（门禁 skipped）
- 实现版本: 分支 `fix-onlyif-skipif-hash-comments`；基于 `main` HEAD `61225e247fc1f4a41eff56ce7709362bb73a631c`；实现未 commit（工作区 diff）
- 审阅对象:
  - `src/main/java/com/ggtest/parser/SqlLogicTestParser.java`（`stripTrailingHashComment` + onlyif/skipif 再 tokenize）
  - `src/test/java/com/ggtest/parser/SqlLogicTestParserTest.java`（3 个新测）
  - `workflow/archive/2026/fix-onlyif-skipif-hash-comments/{plan,dev-notes}.md`
- 工作区另有 `pom.xml`、`workflow/STATUS.md` 修改与未跟踪 `workflow/archive/2026/fix-onlyif-skipif-hash-comments/fix-onlyif-skipif-hash-comments.md`：不纳入本项实现交付（见非阻塞备注）
- 审阅人独立复跑: 是（见「独立验证」）

## 结论

Approve

实现满足 Plan T0–T4 与 A1–A5；L2 与 `in1.test` 冒烟由审阅人独立复跑通过。无阻塞项。可进入 QA。

## 实现正确性

| ID | 要求 | 证据 | 结果 |
|---|---|---|---|
| A1 | `onlyif <engine> # …` → 正确 `dbName`，不抛「requires a database name」 | `onlyif_trailingHashComment_parsesDbName`；`splitTokens(stripTrailingHashComment(header))` | Pass |
| A2 | `skipif <engine> # …` 同上 | `skipif_trailingHashComment_parsesDbName` | Pass |
| A3 | 无尾注释不回归 | `onlyifAndSkipif_withoutTrailingHash_stillParse` + 既有 `p1_a_*` | Pass |
| A4 | `in1.test` 无该 parse error | CLI：无该字符串；`passed=1 failed=0` | Pass |
| A5 | 范围仅本缺陷 | 意图变更限于 parser + 对应测试 + 本目录文档；`pom.xml`/manager 不属本项交付 | Pass（交付范围） |

- T2：仅 onlyif/skipif header 在 `tokens.length == 2` 判定前 strip 行尾 `# …`；整行 `#` 仍由 `line.startsWith("#")` 跳过；未改 record 模型/runner。
- `stripTrailingHashComment` 自首个 `#` 剥离，符合行尾注释合同（引擎名不含 `#`）。
- T1 red→green：`dev-notes` 记 red Errors=2（`requires a database name`）。审阅人未重放 red（已 green）；采信 notes + green 复跑。

## 测试有效性

- 新测在缺 strip 时会因 `tokens.length != 2` / 断言失败而 Fail；覆盖 A1–A3。
- A4 无 `parse(Path)` 单测；Plan 允许 CLI；独立 CLI 证据充分。
- L2：`SqlLogicTestParserTest` 33/0/0/0；`mvn -q clean test` → Tests=227 Failures=0 Errors=0 Skipped=18（既有门控）。

## 文档影响核对

| Plan 声明 | 实现是否一致 | 备注 |
|---|---|---|
| 开发文档 | 是 | `dev-notes.md`（red→green、验证表、A1–A5）；parser 简短 Javadoc |
| 用户文档 | N/A | 对齐官方语料既有写法；未改 README |
| 运维文档 | N/A | 无部署/排障变更 |

## 安全影响核对

触发面：语料行输入解析（行尾 `#` 剥离）。无认证/授权、无网络、无本项依赖升级、无敏感数据写入。

| 检查项 | 结果 | 备注 |
|---|---|---|
| 敏感信息 | 无发现 | 无密钥/凭据；CLI 用 `jdbc:sqlite::memory:` |
| 认证与授权 | N/A | 未触及 |
| 输入与外部访问 | 可接受 | 剥离注释后再 tokenize；不扩大执行面 |
| 依赖变更 | 本项无 | 工作区 `pom.xml` compiler plugin 脏改动须排除出本项 |

处置状态：无安全阻塞；允许进入 QA。

## Git 合规

| 检查 | 结果 |
|---|---|
| 源分支 | `fix-onlyif-skipif-hash-comments`（符合） |
| 实现提交 | 无；符合本轮「不要 commit」 |
| 禁止提交项 | 审阅对象无密钥/`.env`/构建产物纳入本项 |
| `review.md` | 写入工作区；未 `git add`/`commit`/`push` |

## 独立验证

| 命令 | 结果 |
|---|---|
| `mvn -q test -Dtest=SqlLogicTestParserTest` | exit 0；Tests=33 F=0 E=0 S=0 |
| `mvn -q clean test` | exit 0；Tests=227 F=0 E=0 S=18 |
| `./bin/ggtest --engine sqlite --url jdbc:sqlite::memory: sqllogictest/test/evidence/in1.test` | 无 `onlyif requires a database name`；无 parse error；`TOTAL: passed=1 failed=0 skipped=0` |

未重跑：`mvn -q -DskipTests package`（已有可用 jar；A4 CLI 已通过）；T1 red（已 green）。

## 必修项

| ID | 位置 | 问题 | 状态 |
|---|---|---|---|
| — | — | 无 | — |

> `Comment` 不得包含阻塞项；阻塞问题须使用 `Request changes`。本报告结论为 Approve，无阻塞项。

## 非阻塞备注

1. 工作区 `pom.xml` 新增 `maven-compiler-plugin` 不在 Plan 触碰路径；合入本项时须排除。
2. `workflow/docs/manager/*` / `STATUS.md` 属 Manager，不计入 A5 实现 diff。
3. 可选：为 `in1.test` 增加 `parser.parse(Path)` 冒烟单测（非必须）。

## 后续动作

1. Manager：Approve → 调度 QA（定点 `SqlLogicTestParserTest`、`mvn -q clean test`、`in1.test` CLI）。
2. 合入范围：parser + 对应测试 + 本目录文档（`review.md` 按 git.md §1.4）；排除无关 `pom.xml`。
3. 复审范围：N/A。
