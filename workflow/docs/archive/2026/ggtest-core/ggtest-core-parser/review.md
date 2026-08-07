# Review: ggtest-core / parser

> **工作项**：`ggtest-core` · **sub-feature-id**：`parser`  
> **路径**：full · **Review 门禁**：required  
> **审阅版本**：2026-07-24（Plan T1–T6；独立 `mvn -q clean test`）  
> **依据**：[`spec.md`](./spec.md)、[`design.md`](./design.md)、[`plan.md`](./plan.md)、[`dev-notes.md`](./dev-notes.md)；`documentation.md` / `quality.md` / `security.md` / `git.md`

## 审阅范围

- 实现：`pom.xml`；`com.ggtest.model.*`；`SqlLogicTestParser`、`ParseException`
- 测试：`SqlLogicTestParserTest`；`src/test/resources/fixtures/*`
- 文档：`README.md`、公共 Javadoc、`dev-notes.md`
- 禁止改动：Spec/Design/Plan、`workflow/workflow/docs/manager/*`、业务代码
- Git：工作项声明跳过提交与合并 → 不检查提交内容

## 结论

**Approve**

无阻塞项；满足 Spec/Plan 与进入 QA 条件。建议 Manager 调度 QA。

## 实现正确性

| 要求 | 证据 | 结果 |
|---|---|---|
| P0-7 文件名+行号+原因 | `ParseException` 格式 `"<sourceName>:<lineNumber>: <reason>"`；未知类型/fixture/非法签名/残缺 statement fail-fast | 通过 |
| P1-a 全类型；注释/空行无记录 | 内联断言 7 类字段与行号；`all-records.test` 类型序 | 通过 |
| P1-b 无 `----` 仅执行 | `hasExpectedResults=false`，`expectedResults` 空 | 通过 |
| P1-c 扩展名无关 | `.test`/`.slt`/无后缀语义等价 | 通过 |
| Design 分层与模型 | sealed+record 纯 `model`；独立有序指令记录；`IOException`≠`ParseException` | 通过 |
| Plan T1–T6 / 不越界 | Java 17 Maven+JUnit5；双解析入口；README+Javadoc；不连库/不比对/无 CLI | 通过 |

独立验证：`mvn -q clean test`（JDK 17）→ `BUILD SUCCESS`；Tests run: 10，Failures/Errors/Skipped: 0。

## 测试有效性

- 覆盖 P0-7、P1-a/b/c；失败路径含未知类型、非法签名、残缺 statement。
- 断言可因错误实现失败（类型、字段、行号、消息、`hasExpectedResults`）。
- L2 达标；命令与 Plan 一致。
- 非阻塞缺口：fixture 版 P1-a 仅查类型；残缺 skipif/hash 无专项用例。

## 文档影响核对

| Plan 声明 | 一致 | 备注 |
|---|---|---|
| 开发文档：`README.md` + Javadoc | 是 | 构建/测试命令、`parse` 示例、`ParseException` 格式 |
| 用户文档 N/A | 是 | 无 CLI |
| 运维文档 N/A | 是 | 无部署面 |

## 安全影响核对

触发面：文本解析 + `Files.readString`（UTF-8）。无认证授权、网络、反序列化、写回输入。

| 检查项 | 结果 | 备注 |
|---|---|---|
| 敏感信息 | 无发现 | 代码/测试/文档/fixtures 无凭据 |
| 认证与授权 | N/A | — |
| 输入与外部访问 | 可接受 | 只读；不执行 SQL；不连库；fail-fast |
| 依赖变更 | 可接受 | 仅测试依赖 JUnit 5.10.2、Surefire 3.2.5 |
| 文件操作 | 可接受 | 调用方 `Path`；无目录递归 |
| 处置状态 | 无需处置 | 允许进入 QA |

## Git 合规

跳过提交检查（工作项声明）。非阻塞观察：工作区有 Git 元数据、无 `.gitignore`，`target/` 未跟踪；启用提交前须排除构建产物（`git.md` §3）。

## 必修项

| ID | 位置 | 问题 | 状态 |
|---|---|---|---|
| — | — | 无 | — |

> 阻塞项须用 `Request changes`；本报告无阻塞项。

## 非阻塞建议

| ID | 位置 | 建议 |
|---|---|---|
| N1 | 仓库根 | 启用提交前加 `.gitignore`（至少 `target/`） |
| N2 | `p1_a_fromFixtureFile` | 可选断言关键字段 |
| N3 | SQL/期望体读取 | 记录体内 `#` 行现作正文；若语料需任意位置忽略注释，再补合同与测试 |

## 后续动作

1. Manager：Review 门禁通过 → 调度 **QA**（产出 `qa-report.md`）。
2. QA 范围：P0-7、P1-a/b/c；命令 `mvn -q clean test`（JDK 17）。
3. Developer：N1–N3 可选，不阻断 QA。
4. 复审：无（本次 Approve；QA Fail 修复后再审）。
