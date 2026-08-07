# Plan: chore-maven-compiler-release

## 元信息

- 工作项标识: chore-maven-compiler-release（未拆分，sub-feature-id = feature-id）
- 依据 Spec: N/A（Spec 门禁 skipped；toolchain 基线，无产品合同）
- 依据 Design: N/A（Design 门禁 skipped）
- 路径等级: fast
- Review 门禁: skipped（仅声明既有 `release=17` 对应的 compiler 插件；无业务代码）
- 最低验证层: L2（字面 `clean test` / `clean package`；变更仅绑定编译插件，须证明默认 toolchain 下不再回退 Source/Target 5）
- 验证命令:
  - `mvn -q clean test`
  - `mvn -q clean package`
- 源分支: `chore-maven-compiler-release` → 目标: `main`

## 适用工程规范

- [文档工程](../../standards/documentation.md)
- [Git 协作](../../standards/git.md)
- [质量与验证](../../standards/quality.md)
- [安全](../../standards/security.md)

## 目标摘要

在 `pom.xml` 显式声明支持 `<release>` 的 `maven-compiler-plugin`（建议 3.13.0），绑定 `${maven.compiler.release}`（已为 17），避免 Maven 3.6.3 默认 plugin 3.1 忽略 `release` 并回退到无效 Source/Target 5。

## 任务拆解

1. **声明 compiler 插件** — 在 `<build><plugins>` 增加显式 `maven-compiler-plugin`（建议 `3.13.0`），配置 `<release>${maven.compiler.release}</release>`。完成条件：`pom.xml` 含该声明；无业务/测试源码改动。
2. **L2 验证** — 不传 `-Dmaven.compiler.*` 覆盖，执行验收命令；结果写入 `dev-notes.md`。完成条件：两命令 exit 0；日志无 `Source option 5` / `Target option 5` / `no longer supported`。

## 依赖与顺序

1 → 2。Review skipped → 验证通过后直接进 QA；QA Pass 且合并授权持久化后合入 `main`（本地合入；不 push，以 Manager/用户授权为准）。

## 触碰路径

| 任务 | 路径 |
|---|---|
| 1 | `pom.xml` |
| 2 / 文档 | `agents/features/chore-maven-compiler-release/dev-notes.md`、`qa-report.md`（本 `plan.md`） |

禁止：业务/测试源码、`sqllogictest/`、`.env`、`agents/manager/*`。

## 验收

- P0-1：无 `-Dmaven.compiler.source|target|release` 覆盖时，`mvn -q clean test` 成功，且不再报 Java 5 / Source option 5。
- P0-2：同上条件下 `mvn -q clean package` 成功，且不再报上述错误。
- P0-3：diff 仅含 `pom.xml` 与本 Feature 文档；无业务/测试代码、无 `sqllogictest/`、无 `.env`。

预期证据：两命令 exit 0；编译阶段无 Source/Target 5 失败信息；`dev-notes.md` 记录命令与结果摘要。

## 文档影响

| 类别 | 更新路径或 N/A 理由 |
|---|---|
| 开发文档 | `dev-notes.md`（验证命令与结果）；本 `plan.md` |
| 用户文档 | N/A（无用户可见行为变更） |
| 运维文档 | N/A（无部署/排障变更）；QA 产物：`qa-report.md` |

## 无法执行验证时

- 原因：本机无 JDK 17+ / Maven，或依赖拉取失败。
- 风险：无法确认默认 toolchain 下 `release` 是否生效。
- 恢复条件：具备 JDK 17+ 与可运行 `mvn` 后复测 P0-1、P0-2。

## 交接顺序

Developer 改 `pom.xml` + `dev-notes.md` → Review skipped → QA 复跑验收写 `qa-report.md` → Manager 在 Plan 确认与合并授权持久化后推进合入/`done`。

## 修订记录

| 日期 | 摘要 |
|---|---|
| 2026-08-06 | 初稿：fast 极简 Plan；仅 pom compiler 插件 + L2 验证 |
