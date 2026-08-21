# 工作项: add-report-format

描述: `--format junit-xml|json` 测试报告输出（CI 集成用机器可读格式）
目标分支: main
源分支: add-report-format
基线提交: a6c8719bc48099cf772a6bd1807876dd4577259c
文档影响: README（en + zh）新增 `--format` 选项与输出格式说明；用户文档

> **本文件须保存为 `workflow/workspace/add-report-format/main.md`**。
> 流程定义见 `workflow/WORKFLOW.md`；看板见 `workflow/STATUS.md`。
> 本工作项的全部产物平铺在 `workflow/workspace/add-report-format/`，无子目录、无版本后缀。
> 表内只填枚举、短标签或路径；理由与长说明写进「进度笔记」。

## 门禁

| 路径等级 | Spec | Spec 用户确认 | Design | Review |
|---|---|---|---|---|
| standard | required | required | required | required |

## 状态

| 状态 | 下一步 | 阻塞原因 | 恢复条件 | 恢复后目标 |
|---|---|---|---|---|
| blocked | 恢复后 → Planner（Design + Plan） | 用户优先级调整，暂停以处理 P3 并行文件执行 | 用户要求恢复 P2 | designing |

## 进度笔记

- 2026-08-11 登记。来源 `TODO:12-15`（P2 测试报告输出）。当前仅 human-readable stdout（`ReportWriter` + `CliSession.execute`），缺机器可读格式，不利 CI（GitHub Actions / Jenkins）集成。
- 调研要点（见 Task ses_010268cbaffegMu6bFs6DfP31i）：
  - CLI 解析手写 switch（`CliArgumentParser.java:50-70`），无 picocli；新增 `--format` 仿 `--color` 全链路（`CliArgumentParser` → `ParsedArguments` → `CliOptions` → `RuntimeConfigResolver` → `Main.printHelp`）。
  - 无 reporter 抽象；`ReportWriter` 为 final 具体类。引入 `ResultReporter` 接口为 Design 决策点。
  - 无 per-record 计时（仅文件级 `CliSession.java:68,70` 局部变量）；junit-xml `<testcase time="">` 需下沉。
  - 期望/实际 diff 在 `SqlLogicTestRunner.java:260` 被拍平进 `failureReason` 字符串；结构化输出需重新捕获或保留 `ResultComparer.CompareResult`。
  - 无 Jackson/Gson/JAXB 依赖；XML 可用 JDK StAX，JSON 可手写或加 Jackson。
- 路径等级 standard：新增公开 CLI 表面 + 新增行为 + 跨模块（CLI↔runner 数据模型）。
- Spec 用户确认 required：存在业务歧义（XML schema 选型、testcase 粒度、输出目标 stdout vs file、JSON 形状）。
- 2026-08-11：Analyst 完成 spec.md；用户会话确认全部 10 项推荐方案（surefire-style XSD / record 级 testcase / 自定义 JSON / 仅 stdout / 字节一致默认 / 多文件聚合 / 凭据脱敏 / halt+override 交互 / 秒3位 / 结构化错误）。Spec 决策记录节闭合。状态 → designing，调度 Planner。
- 2026-08-11：Planner 调度前用户调整优先级，暂停 P2 以处理 P3。状态 → blocked；Spec 已确认成果保留，恢复时直接续 Planner（Design + Plan）。
- 2026-08-14：按 ggnote `WORKFLOW.md` 标准迁移工作流目录（记录与产物合并为同一目录；权威文件改为 `workflow/WORKFLOW.md`）。
