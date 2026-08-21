# Plan: tighten-cli-boundary-validation

## 元信息

- 依据 Spec: N/A
- 依据 Design: N/A
- 依据 UI: N/A
- 路径等级: fast
- Review 门禁: skipped（范围为明确的 CLI 边界校验与错误映射，改动面小且测试可直接覆盖）
- 最低验证层: unit, integration
- 验证命令:
  - `mvn -q test`
  - 可选 Xugu 实库：使用用户提供的本机 XuguDB 环境运行 Xugu gated 测试
- 预期证据:
  - CLI parser / runtime config / integration tests 覆盖新增边界
  - 默认测试套件通过

## 目标摘要

- 拒绝负数 `--hash-threshold`，避免配置错误被解释为隐式禁用 hash。
- 拒绝空 `--separator`，避免 `--override --separator ""` 写出无效/歧义 header。
- 将 JDBC 驱动连接阶段逃逸的 RuntimeException 归一化为 `connection failed` hard error，并保持凭据脱敏；覆盖 Xugu 不可达连接场景。
- 同步用户文档中的 `--hash-threshold` / `--separator` 边界说明与 Xugu 支持矩阵。

## 任务拆解

1. 更新 CLI 参数解析：`--separator` 必须非空且不含空白，`--hash-threshold` 必须非负。
2. 更新运行时配置解析：env / `.env` 来源的 `GGTEST_HASH_THRESHOLD` 必须非负。
3. 更新连接工厂 / 单文件执行错误映射：连接阶段非 `SQLException` 的驱动 RuntimeException 统一映射为连接失败。
4. 补充/调整单元与集成测试：parser、resolver、Xugu 不可达连接测试。
5. 同步 README / README.zh-CN 对选项边界和 Xugu 支持的描述。
6. 运行验证命令并记录结果到 `dev-notes.md`、`qa-report.md`。

## 依赖与顺序

- 先测试暴露边界，再实现校验与错误映射，最后同步文档。
- Xugu 实库验证依赖用户提供的本机服务与专有 JDBC driver；无法满足时记录验证缺口。

## 触碰路径

- `src/main/java/com/ggtest/cli/CliArgumentParser.java`
- `src/main/java/com/ggtest/cli/RuntimeConfigResolver.java`
- `src/main/java/com/ggtest/cli/ConnectionFactory.java`
- `src/main/java/com/ggtest/cli/FileRunner.java`
- `src/test/java/com/ggtest/cli/CliArgumentParserTest.java`
- `src/test/java/com/ggtest/cli/RuntimeConfigResolverTest.java`
- `src/test/java/com/ggtest/cli/XuguCliIntegrationTest.java`
- `README.md`
- `README.zh-CN.md`

## 验收与验证

| ID | 要求或命令 | 预期证据 | 结果（实施后填） |
|---|---|---|---|
| V-1 | `--hash-threshold -1` | usage error，消息指向 hash-threshold 非负约束 | 待填 |
| V-2 | `.env` / env 中 `GGTEST_HASH_THRESHOLD=-1` | usage error，消息指向 hash-threshold 非负约束 | 待填 |
| V-3 | `--override --separator ""` | usage error，消息指向 separator 非空约束 | 待填 |
| V-4 | Xugu 不可达连接带非空密码 | exit 2，输出含 `connection failed`，不含密码 | 待填 |
| V-5 | `mvn -q test` | 默认测试套件通过 | 待填 |
| V-6 | 用户提供 Xugu 环境 gated 测试 | Xugu gated 测试通过，或记录环境/驱动缺口 | 待填 |

## 验证缺口

| 项 | 原因 | 风险 | 恢复条件 |
|---|---|---|---|
| Xugu 实库 gated 测试 | 待实施后确认本机 driver / 服务可用性 | Xugu 真实连接路径可能未覆盖 | 本机 Xugu JDBC driver 与服务可用 |

## 文档影响

| 类别 | 更新路径或 N/A 理由 |
|---|---|
| 开发文档 | N/A |
| 用户文档 | `README.md`, `README.zh-CN.md` |
| 运维文档 | N/A |

## 交接顺序

1. Developer 实施与自验 →
2. QA 验收 →
3. 用户授权合并 → Manager 置 `done` 一次提交 → 合入 → 归档

## 修订记录

| 日期 | 摘要 |
|---|---|
| 2026-08-21 | 初版计划 |
