# Dev Notes: ggtest-core / normalize

## 实现说明

- 工作项: `ggtest-core` / `normalize`；路径 full；Review required；源分支 `ggtest-core-normalize`（目标 `main`）
- Plan T1–T6 已完成:
  - T1 `ValueNormalizer`：I/T/R（NULL、`%.3f`、`(empty)`、控制字符→`@`）
  - T2 `ResultSorter`：行展开；`NOSORT` / `ROWSORT` / `VALUESORT`
  - T3 `ResultHasher`：值+`\n` 拼 MD5（小写 hex）；识别 `N values hashing to <md5>`；threshold≤0 不做哈希形态
  - T4 `ResultComparer`：比对入口；`DEFAULT_HASH_THRESHOLD=8`；失败含差异摘要素材；无 JDBC
  - T5 验收 + `fixtures/normalize/`（P0-2/P0-4/P0-5/P1-3）；parser 无回归
  - T6 `README.md` normalize 入口；公共 Javadoc；本文件
- 变更路径: `src/main/java/com/ggtest/normalize/{ValueNormalizer,ResultSorter,ResultHasher,ResultComparer}.java`；`src/test/java/com/ggtest/normalize/*`；`src/test/resources/fixtures/normalize/*`；`README.md`；`pom.xml`（description）；本文件
- 只读复用 `ColumnType`/`SortMode`；未改 parser
- 文档影响: 开发文档已更新；用户/运维 N/A
- 安全: 仅内存处理调用方文本/值；不执行 SQL、不连库；fixtures classpath 只读；无敏感信息入仓
- 未解决风险: 无。验证须 JDK 17（`JAVA_HOME=/opt/homebrew/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home`）。本机 Maven 用 `~/tools/apache-maven-3.9.16`（Homebrew `maven` bottle tab 错误，未阻塞）

## 验证

- 最低验证层: L2
- 命令: `mvn -q clean test`（Java 17）
- 结果: exit 0 / BUILD SUCCESS；Surefire Tests run: 36, Failures: 0, Errors: 0, Skipped: 0（normalize 26 + parser 10）
- P0-2: `select1.test` 首条 query 的 30 个规范化 I 值 → MD5 `3c13dee48d9356ae19af2515e05e6b54`

## QA 修复回执

| 缺陷 ID | 处理 | 摘要 | 验证 | 建议复测 |
|---|---|---|---|---|
| — | — | 尚无 QA 缺陷 | — | — |
