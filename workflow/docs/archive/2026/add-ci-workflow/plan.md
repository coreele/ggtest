# Plan: add-ci-workflow

## 元信息

- 工作项标识: add-ci-workflow
- sub-feature-id: add-ci-workflow（未拆分）
- 依据 Spec: N/A（fast 路径，Spec skipped）
- 依据 Design: N/A（fast 路径，Design skipped）
- 依据 UI: N/A（纯 CI 配置，无用户界面）
- 路径等级: fast
- Review 门禁: skipped
- Review 跳过说明: fast 路径，工作项记录标记 skipped
- 最低验证层: L2（构建 + 单元测试通过）
- 验证命令: `mvn verify`
- 预期证据: `BUILD SUCCESS`，所有测试通过（PG 集成测试由 `assumeTrue` 自动跳过）

## 适用工程规范

- `workflow/docs/standards/documentation.md`
- `workflow/docs/standards/git.md`
- `workflow/docs/standards/quality.md`
- `workflow/docs/standards/security.md`

## 目标摘要

添加 GitHub Actions CI 工作流：Java 17 环境下 `mvn verify`（编译 + 测试），在 push 到 `main` 和 PR 时触发。PostgreSQL 集成测试由 JUnit 5 `assumeTrue(GGTEST_PG_URL)` 自动跳过。

## 任务拆解

1. 创建 `.github/workflows/ci.yml`（完成条件：文件存在，包含 Java 17 setup、Maven cache、`mvn verify` 步骤，push/PR 触发）

## 依赖与顺序

- 无外部依赖。单文件创建，无顺序约束。

## 触碰路径

- `.github/workflows/ci.yml`（新建）

## 验收与验证

> fast 且无 Spec：以下为可测条目。

| ID | 要求或命令 | 预期证据 | 结果（实施后填） |
|---|---|---|---|
| V1 | `.github/workflows/ci.yml` 存在且格式合法 | GitHub Actions 编辑器无语法错误 | |
| V2 | 本地模拟：`mvn verify` | `BUILD SUCCESS` | |
| V3 | push 后 GitHub Actions 自动运行 | workflow run 状态为 success（需合入 main 后观察） | |

## 验证缺口

| 项 | 原因 | 风险 | 恢复条件 |
|---|---|---|---|
| N/A | | | |

## 文档影响

| 类别 | 更新路径或 N/A 理由 |
|---|---|
| 开发文档 | N/A（CI 配置自文档化） |
| 用户文档 | N/A（不影响用户功能） |
| 运维文档 | N/A（无需运维操作） |

## 交接顺序

1. Developer 实施与开发者验证（创建 ci.yml，运行 `mvn verify`）→
2. Reviewer（Review 门禁 skipped，跳过）→
3. QA 验收（验证 ci.yml 存在、`mvn verify` 通过）→
4. 用户合并授权 → Manager `done` 一次提交 → 合入

## 修订记录

| 日期 | 摘要 |
|---|---|
| 2026-08-09 | 初稿 |
