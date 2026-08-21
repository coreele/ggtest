# Dev Notes: execute-markdown-slt

## 实现摘要

- 在 CLI 层新增 Markdown 输入适配：显式 `.md` 文件先转换成等行数 sqllogictest 运行视图，再交给现有 parser/runner。
- 新增 `sql` / `slt` / `sqllogictest` 语言 registry 映射；当前都映射为 sqllogictest，保留后续扩展入口。
- `FileRunner` 改为加载 `ExecutableDocument` 后调用 `parse(sourceName, content)`，`--override` 继续写回原始文件路径。
- 补充中英文 README 的可执行 Markdown 使用说明。

## 变更路径

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

## 测试先行记录（TDD）

| Spec ID / 行为项 | 测试 | 先失败 | 后通过 | 说明 |
|---|---|---|---|---|
| P0-1 | `FileRunnerTest.explicitMarkdownFileExecutesMultipleSupportedFencesInOneContext`, `MainOrchestrationTest.explicitMarkdownFileExecutesSupportedCodeBlocks` | `mvn -q -Dtest=MarkdownExecutableExtractorTest,FileRunnerTest,TestFileCollectorTest test` 在 testCompile 失败：`MarkdownExecutableExtractor` 不存在 | 同命令 exit 0；`mvn -q -Dtest=MainOrchestrationTest test` exit 0 | 显式 `.md` 通过 CLI/FileRunner 执行 |
| P0-2 | `FileRunnerTest.markdownAssertionFailureReportsOriginalLineNumber`, `MainOrchestrationTest.markdownParseErrorReportsOriginalLineNumber` | 同上 | 同上 | failure / parse error 均断言原始 `.md` 行号 |
| P0-3 | `MarkdownExecutableExtractorTest.supportedFenceContentsAreKeptOnOriginalLines`, `FileRunnerTest.explicitMarkdownFileExecutesMultipleSupportedFencesInOneContext` | 同上 | 同上 | 代码块外文本被掩码为空行 |
| P0-4 | `FileRunnerTest.explicitMarkdownFileExecutesMultipleSupportedFencesInOneContext` | 同上 | `mvn -q -Dtest=MarkdownExecutableExtractorTest,FileRunnerTest,TestFileCollectorTest test` exit 0 | 第二个 block 依赖第一个 block 创建的表 |
| P0-5 | `MarkdownExecutableExtractorTest.unsupportedFenceContentsAreMasked`, `FileRunnerTest.unsupportedMarkdownFenceIsSkipped` | 同上 | 同上 | 非支持语言内容不解析、不执行 |
| P0-6 | `MarkdownExecutableExtractorTest.supportedLanguagesUseFirstInfoTokenCaseInsensitively` | 同上 | 同上 | 覆盖 `SQL` / `slt` / `sqllogictest` 与首 token |
| P0-7 | `FileRunnerTest.markdownPureSqlFenceUsesSltParserErrors`, `MainOrchestrationTest.markdownParseErrorReportsOriginalLineNumber` | 同上 | 两条计划命令均 exit 0 | `sql` fence 内纯 SQL 走现有 parser error |
| P0-8 | `FileRunnerTest.overrideEnabled_markdownFileRewrittenInsideCodeBlockOnly` | 同上 | `mvn -q -Dtest=MarkdownExecutableExtractorTest,FileRunnerTest,TestFileCollectorTest test` exit 0 | 断言 prose/fence 保留且只替换 expected body |
| P0-9 | `TestFileCollectorTest.collectsNestedTestAndSltFilesInStableOrder`, `TestFileCollectorTest.directoryDoesNotCollectMarkdownFiles`, `TestFileCollectorTest.explicitMarkdownFileIsCollected` | 同上 | 同上 | 目录不自动收 `.md`，显式 `.md` 仍收集 |
| P1-1 | `MarkdownExecutableExtractorTest.emptyMarkdownProducesEmptyExecutableView`, `FileRunnerTest.unsupportedMarkdownFenceIsSkipped` | 同上 | 同上 | 空文档与无支持 block 均无 records |
| P1-2 | `MarkdownExecutableExtractorTest.supportedLanguagesUseFirstInfoTokenCaseInsensitively` | 同上 | 同上 | info string 首 token 生效 |
| P1-3 | `MarkdownExecutableExtractorTest.unclosedSupportedFenceRunsToEof` | 同上 | 同上 | 未闭合支持 block 到 EOF |
| P1-4 | README / README.zh-CN diff | N/A | 人工核对完成 | 文档项无可执行测试；由 diff 和 Review/QA 复核 |

## 验证

| 命令 | 验证层 | 结果摘要 / 证据 |
|---|---|---|
| `mvn -q -Dtest=MarkdownExecutableExtractorTest,FileRunnerTest,TestFileCollectorTest test` | unit + integration | exit 0；覆盖 extractor、FileRunner 与目录收集规则 |
| `mvn -q -Dtest=MainOrchestrationTest test` | integration | exit 0；覆盖真实 CLI 编排与报告行号 |
| `mvn -q test` | unit + integration + build | exit 0；全量回归通过 |
| `python3 workflow/agents/tools/wf-check.py` | workflow static | exit 1；报告 276 个既有 repo-wide archive/其他 workspace 问题，输出未包含 `execute-markdown-slt` 当前项错误 |

## 目标分支同步（最终 Review 前）

- 目标分支及提交: `main` `3014854f21af4b0b4d6adf38de5a251d1ae160ef`
- 同步后源分支 HEAD: `cada48b3e431a36a85131d751b46c9b15b772f07`（实施完成时）
- 同步方式: N/A，本地 `main` 是当前 HEAD 祖先，无需 rebase
- 冲突及处理: N/A
- 同步后复验: 三条验证命令均已在 `cada48b3e431a36a85131d751b46c9b15b772f07` 后执行并通过

## 合入前提交整理

- 用户授权合并后，按 `workflow/agents/standards/git.md` 7.3 整理源分支独有提交。
- 整理后提交结构:
  - `7951ed6` `feat(cli): execute markdown slt code blocks`
  - `ccddcfe` `docs(readme): document executable markdown inputs`
  - `docs(workflow): record execute-markdown-slt lifecycle`（工作流记录、Review、QA 与 done 关闭记录）
- 整理方式: `git reset --mixed main` 后按路径重新分组提交；未纳入无关未跟踪文件。
- 整理后复验:
  - `mvn -q -Dtest=MarkdownExecutableExtractorTest,FileRunnerTest,TestFileCollectorTest test` exit 0
  - `mvn -q -Dtest=MainOrchestrationTest test` exit 0
  - `mvn -q test` exit 0

## 文档影响

| 类别 | 已更新路径或交接说明 |
|---|---|
| 开发文档 | N/A |
| 用户文档 | `README.md`, `README.zh-CN.md` |
| 运维文档 | N/A |

## 未解决风险 / 验证缺口

| 项 | 原因 | 风险 | 恢复条件 |
|---|---|---|---|
| repo-wide `wf-check` 未通过 | 现有 archive/其他 workspace 记录存在历史坏链、表格列数和分支不存在等问题 | 不能把 `wf-check` 作为本工作项关闭门禁的绿色证据 | 后续单独治理工作流历史记录或调整 checker 作用域 |

## QA 修复回执

> QA `Fail` 后按缺陷 ID 追加，不另建文件。

| 缺陷 ID | 处理 | 摘要 | 验证证据 | 建议复测范围 |
|---|---|---|---|---|
| N/A | | | | |
