# QA Report: execute-markdown-slt

> 首测与所有回归写在本文件，按轮次追加；禁止 `qa-report-v2.md`。

## 轮次

| 轮次 | 日期 | 实现版本 | 环境 | 范围 | 结论 |
|---|---|---|---|---|---|
| 1 | 2026-08-21 | `a0829fe0811842f80db46c3fcafb36dce1e9cc91` | Java 17 / Maven / SQLite in-memory | 首测 | Pass |
| 2 | 2026-08-21 | `7951ed6` + `ccddcfe`（提交整理后实现与用户文档；关闭提交仅含工作流记录） | Java 17 / Maven / SQLite in-memory | 合并授权与提交整理复验 | Pass |

## 执行命令

| 命令 | 输出摘要 / 证据位置 |
|---|---|
| `mvn -q -Dtest=MarkdownExecutableExtractorTest,FileRunnerTest,TestFileCollectorTest test` | exit 0 |
| `mvn -q -Dtest=MainOrchestrationTest test` | exit 0 |
| `mvn -q test` | exit 0 |
| 提交整理后 `mvn -q -Dtest=MarkdownExecutableExtractorTest,FileRunnerTest,TestFileCollectorTest test` | exit 0 |
| 提交整理后 `mvn -q -Dtest=MainOrchestrationTest test` | exit 0 |
| 提交整理后 `mvn -q test` | exit 0 |

## 覆盖（对照 Spec 验收与 Plan 验证）

| ID | 条目 | 结果 | 证据 |
|---|---|---|---|
| P0-1 | 显式 `.md` 中 `sql` block 可执行 | Pass | FileRunner / MainOrchestration 测试通过 |
| P0-2 | `.md` 失败报告指向原始行号 | Pass | `markdownAssertionFailureReportsOriginalLineNumber`, `markdownParseErrorReportsOriginalLineNumber` |
| P0-3 | 代码块外 Markdown 不被解析且不影响行号 | Pass | Extractor 行号掩码测试与多 block FileRunner 测试 |
| P0-4 | 多个支持代码块按顺序同文件上下文执行 | Pass | `explicitMarkdownFileExecutesMultipleSupportedFencesInOneContext` |
| P0-5 | 非支持语言代码块不执行不解析 | Pass | Extractor unsupported block 与 FileRunner skip 测试 |
| P0-6 | `SQL` / `slt` / `sqllogictest` 等价 | Pass | `supportedLanguagesUseFirstInfoTokenCaseInsensitively` |
| P0-7 | 支持代码块内纯 SQL 按 parser parse error | Pass | FileRunner / MainOrchestration parse error 测试 |
| P0-8 | `.md` 支持 `--override` 且只改代码块内期望 | Pass | `overrideEnabled_markdownFileRewrittenInsideCodeBlockOnly` |
| P0-9 | 目录输入不自动收 `.md` | Pass | `directoryDoesNotCollectMarkdownFiles` 与目录稳定排序测试 |
| P1-1 | 空或无支持代码块 `.md` 运行通过 records=0 | Pass | `emptyMarkdownProducesEmptyExecutableView`, `unsupportedMarkdownFenceIsSkipped` |
| P1-2 | info string 首 token 决定执行器 | Pass | Extractor 首 token 测试 |
| P1-3 | 未闭合支持代码块运行到 EOF 且行号保真 | Pass | `unclosedSupportedFenceRunsToEof` |
| P1-4 | README / README.zh-CN 更新 | Pass | QA 核对文档说明覆盖规则、限制与 `--override` |
| V-1 | 定向 extractor/FileRunner/collector 测试 | Pass | 命令 exit 0 |
| V-2 | MainOrchestrationTest | Pass | 命令 exit 0 |
| V-3 | 全量 `mvn -q test` | Pass | 命令 exit 0 |

## 回归

| 范围 | 结果 | 证据 |
|---|---|---|
| CLI parser/runner/override 回归 | Pass | `mvn -q test` exit 0 |
| 目录收集兼容性 | Pass | `TestFileCollectorTest` exit 0 |
| 原 `.test` / `.slt` 输入兼容性 | Pass | 全量回归与既有 CLI 测试通过 |
| 合入前提交整理 | Pass | 整理为实现、README、工作流三类提交；整理后三条验证命令 exit 0 |

## 文档与安全验收

| 项 | 结果 | 备注 |
|---|---|---|
| 用户可见文档 | Pass | `README.md`, `README.zh-CN.md` 已更新 |
| 运维可执行文档 | N/A | 本项无运维文档影响 |
| 安全验证范围 | Pass | 仅本地文本输入适配；未新增外部命令执行、网络访问、依赖或凭据 |

## 缺陷

| ID | 严重度 | 摘要 | 状态 | 处理说明 | 验证证据 |
|---|---|---|---|---|---|
| N/A | | | | | |

## 阻塞（Blocked 时必填）

- 原因: N/A
- 风险: N/A
- 恢复条件: N/A
- 复测范围: N/A

## 结论

- 本轮结论: Pass
- 合并: 已授权（Manager 置 `done` 后合入）

## 备注

- `dev-notes.md` 记录的 `wf-check` 失败为既有 repo-wide archive/其他 workspace 问题；QA 未发现该输出中包含 `execute-markdown-slt` 当前项错误。
