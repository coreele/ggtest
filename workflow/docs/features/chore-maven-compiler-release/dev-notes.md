# Dev Notes: chore-maven-compiler-release

## 实现说明

- 工作项: chore-maven-compiler-release（未拆分）
- 分支: `chore-maven-compiler-release` → `main`；**未** commit / push / merge
- 禁止项: 未改业务/测试 Java、`workflow/workflow/docs/manager/*`；未入库 `sqllogictest/` / `.env`
- TDD: 无产品行为测试；以字面 Maven clean 构建验收

### 实现摘要

`pom.xml` 显式 `maven-compiler-plugin` **3.13.0**，`<release>${maven.compiler.release}</release>`；属性已为 17，未改值；其余插件未动。

改前基线：默认 `maven-compiler-plugin:3.1` → `Source option 5` / `Target option 5` / `no longer supported`，`mvn -q clean package` 失败。

### 变更路径

| 路径 | 变更 |
|---|---|
| `pom.xml` | 增加 compiler 插件 3.13.0 + release 绑定 |
| `workflow/workflow/docs/features/chore-maven-compiler-release/dev-notes.md` | 本文件 |

### 验证（L2；无 `-Dmaven.compiler.*` 覆盖）

| 步骤 | 命令 | exit | 摘要 |
|---|---|---|---|
| 基线（改前） | `mvn -q clean package` | 非 0 | plugin 3.1；Source/Target option 5 / no longer supported |
| P0-1 | `mvn -q clean test` | **0** | 无 option 5 / no longer supported |
| P0-2 | `mvn -q clean package` | **0** | 同上；`target/ggtest-0.1.0-SNAPSHOT.jar` |

### 建议复测（QA）

1. 无 `-Dmaven.compiler.source|target|release` 覆盖下重跑两验收命令：exit 0，无 Java 5 / option 5。
2. diff 仅 `pom.xml` + 本 Feature 文档；无业务/测试源码、无 `sqllogictest/`、无 `.env`。

### 开放问题 / 未解决风险

无。

## QA 修复回执

| 缺陷 ID | 处理 | 摘要 | 验证 | 建议复测 |
|---|---|---|---|---|
| — | N/A | 本轮无 QA Fail | — | — |
