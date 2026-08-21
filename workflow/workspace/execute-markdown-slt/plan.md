# Plan: execute-markdown-slt

## 元信息

- 依据 Spec: `workflow/workspace/execute-markdown-slt/spec.md`
- 依据 Design: `workflow/workspace/execute-markdown-slt/design.md`
- 依据 UI: N/A
- 路径等级: standard
- Review 门禁: required
- 最低验证层: unit, integration, build
- 验证命令:
  - `mvn -q -Dtest=MarkdownExecutableExtractorTest,FileRunnerTest,TestFileCollectorTest test`
  - `mvn -q -Dtest=MainOrchestrationTest test`
  - `mvn -q test`
- 预期证据:
  - Markdown extractor 单元测试覆盖 fence 识别、语言映射、行号掩码、未闭合 block。
  - CLI/FileRunner 集成测试覆盖显式 `.md` 执行、失败行号、非支持语言跳过、`--override` 写回。
  - 默认测试套件通过。

## 目标摘要

实现第一版可执行 Markdown：显式 `.md` 文件中的 `sql` / `slt` / `sqllogictest` fenced code block 映射到 sqllogictest 执行器，代码块外不执行且保持原始行号。设计上保留语言 registry 入口，但本轮只执行 sqllogictest。

## 任务拆解

1. 新增 Markdown 输入适配层：`ExecutableDocument`、`ExecutableDocumentLoader`、`MarkdownExecutableExtractor` 与语言 registry。
2. 调整 `FileRunner`：从文件加载运行视图后调用 `SqlLogicTestParser.parse(sourceName, content)`。
3. 保持 `TestFileCollector` 目录递归规则不变，补测试确认目录不自动收 `.md`。
4. 补充 extractor 单元测试：支持语言、非支持语言、info string 首 token、大小写、未闭合 block、空文档。
5. 补充 CLI/FileRunner 集成测试：显式 `.md` 运行成功、失败行号保真、纯 SQL parse error、`--override` 写回 `.md`。
6. 更新 README / README.zh-CN，说明可执行 Markdown 规则与限制。
7. 执行验证命令并记录到 `dev-notes.md`。

## 依赖与顺序

- 先 TDD 写 extractor 和 FileRunner/CLI 失败用例。
- 再实现输入适配层与 FileRunner 接线。
- 最后补文档并执行全量回归。

## 触碰路径

- `src/main/java/com/ggtest/cli/ExecutableDocument.java`
- `src/main/java/com/ggtest/cli/ExecutableDocumentLoader.java`
- `src/main/java/com/ggtest/cli/MarkdownExecutableExtractor.java`
- `src/main/java/com/ggtest/cli/FileRunner.java`
- `src/test/java/com/ggtest/cli/MarkdownExecutableExtractorTest.java`
- `src/test/java/com/ggtest/cli/FileRunnerTest.java`
- `src/test/java/com/ggtest/cli/TestFileCollectorTest.java`
- `src/test/java/com/ggtest/cli/MainOrchestrationTest.java`
- `README.md`
- `README.zh-CN.md`

## 验收与验证

| ID | 要求或命令 | 预期证据 | 结果（实施后填） |
|---|---|---|---|
| P0-1 | 显式 `.md` 中 `sql` block 可执行 | CLI/FileRunner 测试通过，记录通过 | 待填 |
| P0-2 | `.md` 失败报告指向原始行号 | 集成测试断言 failure detail 行号 | 待填 |
| P0-3 | 代码块外 Markdown 不被解析且不影响行号 | extractor/FileRunner 测试通过 | 待填 |
| P0-4 | 多个支持代码块按顺序同文件上下文执行 | 集成测试第二块依赖第一块建表并通过 | 待填 |
| P0-5 | 非支持语言代码块不执行不解析 | 测试含无效 bash/python 内容仍通过 | 待填 |
| P0-6 | `SQL` / `slt` / `sqllogictest` 等价 | extractor 或集成测试覆盖 | 待填 |
| P0-7 | 支持代码块内纯 SQL 按 parser parse error | 集成测试 exit 2 / parse error | 待填 |
| P0-8 | `.md` 支持 `--override` 且只改代码块内期望 | override 集成测试验证 prose/fence 保留 | 待填 |
| P0-9 | 目录输入不自动收 `.md` | `TestFileCollectorTest` 覆盖 | 待填 |
| P1-1 | 空或无支持代码块 `.md` 运行通过 records=0 | extractor / FileRunner 测试覆盖 | 待填 |
| P1-2 | info string 首 token 决定执行器 | extractor 测试覆盖 | 待填 |
| P1-3 | 未闭合支持代码块运行到 EOF 且行号保真 | extractor 测试覆盖 | 待填 |
| P1-4 | README / README.zh-CN 更新 | 文档 diff 覆盖规则与限制 | 待填 |
| V-1 | `mvn -q -Dtest=MarkdownExecutableExtractorTest,FileRunnerTest,TestFileCollectorTest test` | exit 0 | 待填 |
| V-2 | `mvn -q -Dtest=MainOrchestrationTest test` | exit 0 | 待填 |
| V-3 | `mvn -q test` | exit 0 | 待填 |

## 验证缺口

| 项 | 原因 | 风险 | 恢复条件 |
|---|---|---|---|
| N/A | | | |

## 文档影响

| 类别 | 更新路径或 N/A 理由 |
|---|---|
| 开发文档 | N/A |
| 用户文档 | `README.md`, `README.zh-CN.md` |
| 运维文档 | N/A |

## 交接顺序

1. Developer 实施与自验 →
2. Reviewer（Review required）→
3. QA 验收 →
4. 用户授权合并 → Manager 置 `done` 一次提交 → 合入 → 归档

## 修订记录

| 日期 | 摘要 |
|---|---|
| 2026-08-21 | 初版计划 |
