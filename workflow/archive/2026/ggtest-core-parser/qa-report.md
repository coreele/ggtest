# QA Report: ggtest-core / parser

> **工作项**：`ggtest-core` · **sub-feature-id**：`parser`  
> **路径**：full · **Review 门禁**：required（入口前已 Approve）

## 轮次

| 轮次 | 日期 | 实现版本 / 范围 | 环境 | 结论 |
|---|---|---|---|---|
| 1 | 2026-07-24 | Plan T1–T6；首测 P0-7、P1-a/b/c | macOS aarch64；JDK 17.0.19（Homebrew openjdk@17）；Maven 3.9.16 | Pass |

## 入口门禁核验（轮次 1）

| 条件 | 证据 | 结果 |
|---|---|---|
| Plan 已用户确认并持久化 | `workflow/archive/2026/ggtest-core/ggtest-core.md`：用户确认 parser Plan（「ok」） | 满足 |
| Review 门禁 required 且 Reviewer Approve | 工作项门禁 `required`；`review.md` 结论 Approve | 满足 |
| 可验收实现与 Plan 验证要求 | `pom.xml`、`model`/`parser`、fixtures、`SqlLogicTestParserTest`；L2：`mvn -q clean test` | 满足 |

## 环境与命令（轮次 1）

- 工作区：`/Users/zhougangjie/Space/ggtest`
- JDK：`JAVA_HOME=/opt/homebrew/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home`（openjdk 17.0.19）
- 命令：`mvn -q clean test` → 退出码 0
- Surefire：`target/surefire-reports/com.ggtest.parser.SqlLogicTestParserTest.txt` — Tests run: 10, Failures: 0, Errors: 0, Skipped: 0
- 补充：`fixtures/equivalent-content{.test,.slt,}` 三份 `cmp` 内容一致

## 覆盖（对照 Spec 验收 + Plan 验证）

### Spec 验收

| ID | 要求 | 结果 | 证据 |
|---|---|---|---|
| P0-7 | 未知记录类型 → 错误含文件名与行号（CLI 退出码 2 属 `cli-corpus`） | 通过 | `p0_7_*`：`ParseException` 消息 `"<sourceName>:<lineNumber>: <reason>"`；`unknown-record.test` 行号 3；非法签名/残缺 statement 同 fail-fast |
| P1-a | 注释/空行 + 全部记录类型 → 有序记录；注释/空行不产生记录 | 通过 | `p1_a_*`：7 条记录字段与行号；`all-records.test` 类型序 |
| P1-b | 无 `----` 的 query → 只执行不比对 | 通过 | `p1_b_*`：`hasExpectedResults=false`，`expectedResults` 空 |
| P1-c | 同内容 `.test` / `.slt` / 无标准后缀 → 语义等价 | 通过 | `p1_c_*`：临时文件与资源三路径 `semanticKey` 等价 |

### Plan 验证

| 项 | 要求 | 结果 | 证据 |
|---|---|---|---|
| 最低验证层 | L2（单元测试 + 构建） | 通过 | 同上 `mvn -q clean test` |
| 验证命令 | `mvn -q clean test`（Java 17） | 通过 | 显式 JDK 17；退出码 0 |
| 预期证据 | BUILD SUCCESS；Surefire 全过 | 通过 | Tests run: 10；Failures/Errors/Skipped: 0 |

### 回归

| 范围 | 结果 | 说明 |
|---|---|---|
| 本切片全部解析/错误路径（10 用例） | 通过 | 首个实现切片；全量 Surefire 即回归面 |

### 文档验收

| 类别 | Plan 声明 | 结果 | 证据 |
|---|---|---|---|
| 开发文档 | `README.md` + 公共 Javadoc | 通过 | 前置条件、`mvn -q clean test`、双 `parse` 示例、`ParseException` 格式；公共类型有 Javadoc |
| 用户文档 | N/A（无 CLI） | 通过（N/A） | 与 Plan/Spec 一致 |
| 运维文档 | N/A（无部署面） | 通过（N/A） | 与 Plan 一致 |

### 安全验收（`security.md`）

| 检查项 | 结果 | 备注 |
|---|---|---|
| 范围 | 文本解析 + `Files.readString`（UTF-8） | 输入处理/文件操作；无认证、授权、网络、反序列化 |
| 敏感信息 | 无发现 | 代码/测试/fixtures/文档无凭据 |
| 输入与文件 | 可接受 | 只读；不执行 SQL；不连库；不改输入；fail-fast |
| 依赖 | 可接受 | JUnit 5.10.2、Surefire 3.2.5（test） |
| 处置状态 | 无需处置 | 允许继续（质量不阻塞；工作项声明不合并） |

## 缺陷

| ID | 严重度 | 摘要 | 状态 | 处理说明 / 验证证据 |
|---|---|---|---|---|
| — | — | 无 | — | — |

非阻塞观察（不计入缺陷）：启用正式 Git 提交前建议 `.gitignore` 排除 `target/`。

## 结论（轮次 1）

- **总体：Pass**
- 恢复条件：N/A
- 合并：不合并（源/目标分支「不适用」；非 Git 工作区；不请求合并授权）
- 建议后续：Manager 请求用户完成关闭/完成授权 → 关闭并归档 `parser`；可推进 `normalize` 等切片
