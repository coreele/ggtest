# Design: execute-markdown-slt

## 背景与约束

现有执行链路是 `TestFileCollector` 收集路径，`FileRunner` 调用 `SqlLogicTestParser.parse(Path)`，随后交给 `SqlLogicTestRunner`。Parser 只理解 sqllogictest 文本，`SourceLocation.startLine` 由输入文本行号直接得出；`OverrideWriter` 又依赖这些行号回写原始文件。

Markdown 执行必须满足两个约束：

- 运行视图保留原始 `.md` 行号。
- 不让 parser / runner 直接承担 Markdown 语法职责。

## 方案对比与决策

| 方案 | 概要 | 优点 | 缺点 | 比较依据 |
|---|---|---|---|---|
| A | 先把 `.md` 转成临时 `.slt` 再执行 | 实现直接，可复用 parser | 删除 fence 行会破坏行号；`--override` 目标变复杂 | 不满足行号保真 |
| B | 扩展 `SqlLogicTestParser` 直接识别 Markdown | 单点 parse | parser 混入 Markdown 与未来语言执行职责；破坏 parser 纯 SLT 边界 | 分层与长期扩展性差 |
| C | CLI 输入适配层将 `.md` 掩码成同等行数的 SLT 视图 | 行号保真；parser/runner 无需知道 Markdown；`--override` 仍可按原文件行号工作 | 需要新增 source adapter 与测试 | 满足 Spec 且改动边界清晰 |

**决策：** 采用 C。第一版把 Markdown 视为 CLI 输入格式，输出同等行数的 SLT 运行视图；支持语言映射保留为 registry 形式，当前只注册 sqllogictest executor。

## 模块边界与分层

- `model` / `parser` / `normalize` / `runner` 不新增 Markdown 依赖。
- `cli` 增加输入适配：
  - `ExecutableDocument`：原始路径、display/source name、运行视图文本。
  - `ExecutableDocumentLoader`：按文件扩展选择普通 SLT 或 Markdown 适配。
  - `MarkdownExecutableExtractor`：识别 fenced code block，生成同等行数运行视图。
  - `FenceBlockLanguageRegistry` 或等价 registry：当前将 `sql` / `slt` / `sqllogictest` 判为 `sqllogictest` block。
- `FileRunner` 不再直接调用 `parser.parse(file)`；改为先加载 `ExecutableDocument`，再调用 `parser.parse(sourceName, content)`。
- `OverrideCoordinator` / `OverrideWriter` 继续使用原始 `Path` 写回。由于运行视图行号与原文件一致，现有行区间逻辑可复用。

## Markdown 视图生成规则

- 逐行扫描原始 Markdown。
- fence 行：trim 后以至少三个反引号开头；当前只支持 backtick fence。
- 开 fence 的 info string 去掉反引号后 strip，取第一个空白分隔 token 作为语言名，大小写不敏感。
- 若语言名为 `sql`、`slt`、`sqllogictest`，进入 active block；内容行原样输出。
- 若语言不支持，进入 inactive block；内容行输出空行。
- 代码块外与所有 fence 行均输出空行。
- 未闭合 active block 到 EOF，内容行按 active block 继续输出。

## 影响面

- CLI 显式文件输入新增 `.md` 运行能力。
- 目录收集逻辑保持只收 `.test` / `.slt`，无需改为自动收 `.md`。
- README / README.zh-CN 需要新增可执行 Markdown 说明。
- `--override` 需要针对 `.md` 覆盖一组回归测试，确保只改代码块内期望块。

## 风险

| 风险 | 影响 | 缓解 |
|---|---|---|
| Markdown 掩码行号偏移 | 失败定位和 `--override` 写错行 | 单测覆盖 fence 行、外部文本、多代码块、未闭合 block 的 line number |
| ` ```sql` 被用户放入纯 SQL 示例 | 会按 sqllogictest parse error 报错 | Spec 已明确：可执行 Markdown 内 `sql` block 按 sqllogictest 解释 |
| 未来多语言执行器膨胀 CLI | 后续扩展难维护 | 当前用 registry 隔离语言判定，不引入外部进程执行合同 |
| `--override` 对 `.md` 误改 Markdown 文本 | 文档损坏 | 只复用行号保真的代码块行区间，并用集成测试验证 fence 和 prose 保留 |

## 对 Plan 与 Developer 的要点

### Plan

- 先实现 Markdown extractor 单元测试，再接入 `FileRunner`。
- 明确 `.md` 无 records 的通过语义。
- 目标验证必须含 `--override` 写回 `.md`。

### Developer

- 不改 `SqlLogicTestParser` 的 Markdown 语义；只使用 `parse(sourceName, runtimeView)`。
- 不为未来 Python / sh 实现执行逻辑，只保留语言 registry 的接入点。
- 代码块外要输出空行而不是 `#` 注释，避免 expected-result 读取注释造成语义偏差。
