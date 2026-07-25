# Dev Notes: ggtest-core / parser

## 实现说明

- 工作项: `ggtest-core` / `parser`；路径 full；Review 门禁 required
- 已完成 Plan T1–T6:
  - T1: `pom.xml`（`com.ggtest:ggtest:0.1.0-SNAPSHOT`，Java 17，JUnit 5，Surefire）+ `src/main|test/java`、`src/test/resources`
  - T2: `com.ggtest.model` — `SourceLocation`、`sealed SqlTestRecord` 及全部 record/枚举，不可变
  - T3: `ParseException`（`sourceName`/`lineNumber`/`reason`；消息 `<sourceName>:<lineNumber>: <reason>`）
  - T4: `SqlLogicTestParser.parse(Path|String,String)`；单遍扫描；注释/空行无记录；无 `----` → `hasExpectedResults=false`
  - T5: fixtures（`.test`/`.slt`/无后缀、未知类型）+ 测试覆盖 P0-7、P1-a/b/c
  - T6: `README.md` 构建/解析用法；公共 Javadoc
- 变更路径: `pom.xml`；`src/main/java/com/ggtest/model/*`；`src/main/java/com/ggtest/parser/{ParseException,SqlLogicTestParser}.java`；`src/test/java/com/ggtest/parser/SqlLogicTestParserTest.java`；`src/test/resources/fixtures/*`；`README.md`；本文件
- 文档影响: 开发文档已更新；用户/运维 N/A
- 安全: 只读 UTF-8 文本；不执行 SQL、不连库、不改输入；无敏感信息写入
- 未解决风险: 无。验证须显式 JDK 17（例：`JAVA_HOME=/opt/homebrew/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home`）；系统默认 `java` 可能不可用

## 验证

- 最低验证层: L2
- 命令: `mvn -q clean test`（Java 17）
- 结果: `BUILD SUCCESS`；Tests run: 10, Failures: 0, Errors: 0, Skipped: 0

## QA 修复回执

| 缺陷 ID | 处理 | 摘要 | 验证 | 建议复测 |
|---|---|---|---|---|
| — | — | 尚无 QA 缺陷 | — | — |
