# Spec: execute-markdown-slt

## 背景与目标

用户已有 `bin/mdslt` 可把 Markdown 转为 `.slt`，但该脚本输出不是运行时行号保真的执行视图。本工作项新增第一版“可执行 Markdown 文档”：显式传入 `.md` 文件时，ggtest 只执行指定 fenced code block 内的 sqllogictest 内容，代码块外全部视为说明文本并在运行时保留原始行号。

第一版目标：

- ` ```sql`、` ```slt`、` ```sqllogictest` fenced code block 等价映射到现有 sqllogictest 执行器。
- Markdown 原文件路径与原始行号用于 parse error、failure report 与 `--override` 写回。
- 设计上预留后续按语言名扩展 Python、shell 等执行器的结构，但本轮不执行这些语言。

## 非目标

- 不新增 Python、shell 或其他外部命令执行器。
- 不引入 `ignore` / `no-test` 等跳过标记；可执行 Markdown 的内容由文档约定保证。
- 不要求完整 CommonMark 解析器；第一版只识别 backtick fenced code block。
- 不改变 `.slt` / `.test` 文件解析语义。
- 不自动把目录中的所有 `.md` 文件纳入递归收集。
- 不改变 `bin/mdslt` / `bin/sltmd` 的脚本行为；脚本可在后续工作项再统一。

## 范围与可见行为

- 显式文件输入 `ggtest path/to/doc.md` 必须作为可执行 Markdown 运行。
- 显式 `.md` 文件的报告 display/source name 仍使用原始 `.md` 路径。
- 目录输入行为保持当前保守策略：递归只收集 `.test` / `.slt`，不自动收集 `.md`。
- Markdown 文件中可有多个可执行 fenced code block；它们在同一文件执行上下文内按原文顺序组成 sqllogictest record 流。
- 支持的执行语言名为 `sql`、`slt`、`sqllogictest`，匹配 fenced code block info string 的第一个 token，大小写不敏感。
- 非支持语言的 fenced code block 与代码块外 Markdown 文本均不执行。
- 对支持语言代码块内的内容不做 SQL 直执行；仍按现有 sqllogictest 语法解析。因此纯 SQL 出现在 ` ```sql` 内时，按 sqllogictest parser 的正常 parse error 处理。

## 合同

### API / 接口

- CLI 不新增必需参数；显式传入 `.md` 文件即启用 Markdown 输入适配。
- 现有 `.slt` / `.test` 输入路径保持现有 parser 直读。
- 第一版执行器 registry 至少能把 `sql`、`slt`、`sqllogictest` 解析为同一个 sqllogictest block executor；未来新增语言 executor 不应要求重写 CLI 主流程。
- `--override` 对 `.md` 文件可用，写回目标为原始 Markdown 文件。

### 数据 / 状态

- Markdown 运行时视图必须与原文件行数一致。
- 支持语言代码块的开 fence 行、关 fence 行、代码块外文本行、非支持语言代码块行必须在运行时视图中掩码为空行。
- 支持语言代码块内容行必须原样出现在运行时视图的相同行号。
- 记录的 `SourceLocation.startLine` 必须对应原始 Markdown 文件中的行号。
- `--override` 只能改写 record 对应的原始行区间；不得改写代码块外 Markdown 文本或 fence 行。

### 错误与约束

- Parse error 和 failure detail 必须显示原始 `.md` 文件路径与原始行号。
- 支持语言代码块中的语法错误必须按现有 sqllogictest parse error / failure 处理。
- 非支持语言代码块不得执行，也不得因为内容不是 sqllogictest 而报错。
- 第一版仅识别 backtick fence：trim 后以至少三个反引号开始的开关行。Tilde fence 与缩进代码块不在本轮范围。
- 若支持语言代码块未闭合，则视为代码块持续到 EOF；其中内容按 sqllogictest 解析，行号仍保真。
- 空的可执行 Markdown 文件或没有支持语言代码块的 `.md` 文件按“无可执行 records”处理，必须返回通过且不产生执行记录。

## 验收

### P0

- P0-1: Given 显式 `.md` 文件包含 ` ```sql` fenced block，When 运行 ggtest，Then 代码块内 `statement ok` / `query` 按现有 sqllogictest 执行并通过。
- P0-2: Given `.md` 的第 N 行在支持语言代码块内有失败 query，When 运行 ggtest，Then failure detail 指向该 `.md` 文件的第 N 行。
- P0-3: Given `.md` 在代码块外含任意 Markdown 标题、正文、列表或引用，When 运行 ggtest，Then 这些行不被 parser 当成 record，也不影响行号。
- P0-4: Given `.md` 含多个 `sql` / `slt` / `sqllogictest` 代码块，When 运行 ggtest，Then 它们按原文顺序在同一文件上下文执行。
- P0-5: Given `.md` 含非支持语言代码块，When 运行 ggtest，Then 该代码块不执行、不解析、不报 sqllogictest 错。
- P0-6: Given `.md` 中 ` ```SQL`、` ```slt`、` ```sqllogictest`，When 运行 ggtest，Then 三者均映射到 sqllogictest 执行器。
- P0-7: Given `.md` 支持语言代码块内是纯 SQL 而非 sqllogictest record，When 运行 ggtest，Then 按现有 parser 报 parse error，而非被当作 SQL 直连执行。
- P0-8: Given 显式 `.md` 文件带 `--override`，When query expected result 需要覆盖，Then 只改写代码块内对应 expected block，保留 Markdown 文本和 fence 行。
- P0-9: Given 目录输入，When 目录下包含 `.md`、`.slt`、`.test`，Then 递归收集仍只自动包含 `.slt` / `.test`。

### P1

- P1-1: 空 `.md` 或无支持语言代码块的 `.md` 运行通过，records 数为 0。
- P1-2: 支持语言 code fence info string 有额外 token 时，以第一个 token 决定执行器。
- P1-3: 支持语言代码块未闭合时运行到 EOF，行号仍保真。
- P1-4: README / README.zh-CN 说明 `.md` 执行规则、支持语言名、目录收集限制与 `--override` 影响。

## 开放问题

- 无。当前合同按用户已确认方向拟定；等待用户确认后进入 Design。
