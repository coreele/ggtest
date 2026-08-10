# dev-notes: add-ci-workflow

## 实现摘要

创建 `.github/workflows/ci.yml`：
- 触发：push/PR 到 `main`
- JDK：Temurin 17，Maven cache
- 步骤：`mvn verify --batch-mode`

## 变更路径

- `.github/workflows/ci.yml`（新建）

## TDD 与开发者验证

| 项 | 命令 | 结果 | 证据 |
|---|---|---|---|
| 构建 + 测试 | `mvn verify --batch-mode` | BUILD SUCCESS | 250 tests run, 0 failures, 0 errors, 18 skipped |

PostgreSQL 集成测试由 JUnit 5 `assumeTrue(GGTEST_PG_URL)` 自动跳过，无需额外配置。

## 文档影响

| 类别 | 状态 |
|---|---|
| 开发文档 | N/A |
| 用户文档 | N/A |
| 运维文档 | N/A |

## 未解决风险

无。

## 建议后续角色

Review 门禁 skipped（fast 路径），建议直接进入 QA。
