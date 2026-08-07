# Spec: fix-cli-credential-redaction

> 需求与规格（Plan 之前完成）。任务拆解见后续同目录 `plan.md`。
>
> **feature-id**：`fix-cli-credential-redaction` · **sub-feature-id**：`fix-cli-credential-redaction`（未拆分）
> **适用对象**：Planner、Developer、Reviewer、QA、Manager。
> **前置条件**：工作项 [`agents/docs/manager/fix-cli-credential-redaction.md`](../../manager/fix-cli-credential-redaction.md)；审计 [`agents/docs/audit/2026-07-26-src.md`](../../audit/2026-07-26-src.md) Finding §7 / CA-002。
> **预期结果**：`CliSession` 脱敏控制点与 `CliOptions.toString()` 不泄露 URL userinfo / password 明文；测试证明修复前后差异。
> **失败处理**：合同歧义不得进入 Plan。Spec 用户确认已 **approved**（Manager 授权）。

## 背景与目标

CA-002：`CliSession.sanitize` 注释承诺脱敏，实现仅 `strip()`；连接失败等路径可能把驱动/URL 回显写入 stdout/stderr/报告。`CliOptions.toString` 对 `password` 已为 `***`，但 `url` 含 `user:pass@` 时仍可能泄露。

**目标**：两处控制点真正脱敏；测试证明曾可泄露、修复后不再泄露。

## 非目标

- 不改 JDBC 执行器、parser、normalize、runner 语义，或非脱敏路径的报告业务格式。
- 不改凭据加载优先级（CLI / 环境变量 / `.env`）。
- 不做通用密钥扫描、日志框架接入或全仓库安全审计。
- 不修改 `architecture-overview`、真实 `.env`、`examples/` 未跟踪语料。
- 不要求改写既有「password 不出现在输出」通过路径测试；本项补齐 URL userinfo 与 sanitize 真正脱敏。

## 范围与可见行为

1. **`CliSession` 脱敏控制点**（当前 `sanitize`）：经该点写入 stdout / stderr / 报告明细的字符串必须：
   - 替换 URL **userinfo**（`scheme://user:password@host...` 中的 `user:password@`）；
   - 替换**已配置 password** 的明文字面量（本次运行提供了 password 时）；
   - 保留 null 安全与首尾空白裁剪（null → `""`）。
2. **`CliOptions.toString`**：有值时 `password` 仍为 `***`；`url` 的 userinfo 不得明文出现；无 userinfo 的 URL 可保留主机/库路径可读信息。
3. **测试**：须有用例在修复前失败（证明泄露）、修复后通过。允许包内可见性或夹具触及控制点；禁止过度破坏生产封装。

## 合同

### API / 接口

- CLI 参数、退出码、`.env` 键名：**不变**。
- 实现可保持 `sanitize` 私有或抽共享 helper；可见合同：经控制点的输出与 `toString()` 不含上述明文凭据。

### 数据 / 状态

- 内存中 `url` / `password` **可保留明文**（连接需要）；仅诊断字符串视图脱敏。
- userinfo 占位须稳定可测（如 `***@` 或等价掩码）；同一输入多次调用结果一致。

### 错误与约束

- 脱敏不得抛异常掩盖原失败：去掉凭据后的剩余文本仍须可观察。
- null → `""`。
- 无 password 配置时：仍须脱敏消息内 URL userinfo；password 字面量规则不适用。

## 验收（Given-When-Then）

### P0

- **P0-1 sanitize · URL userinfo**  
  Given 错误消息含 `user:secretPass@` 形式 JDBC/URL userinfo  
  When 经 `CliSession` 脱敏控制点处理  
  Then 结果不含 `secretPass`；原消息中非凭据片段（主机、错误前缀等）仍可识别。

- **P0-2 sanitize · password 字面量**  
  Given 配置 password `super-secret-credential`，且错误消息含该字面量  
  When 经脱敏控制点处理  
  Then 结果不含 `super-secret-credential`。

- **P0-3 CliOptions.toString · URL userinfo**  
  Given `url` 含 `alice:bob@` userinfo，且 `password` 为某明文  
  When 调用 `toString()`  
  Then 结果不含 `bob` 与该 password 明文；有值时仍含 password 字段掩码 `***`。

- **P0-4 泄露证明测试**  
  Given 覆盖 P0-1 与 P0-3 的自动化测试  
  When 在仅 `strip()` / 原样 `url` 实现上运行  
  Then 测试失败（证明曾泄露）；修复后同一测试通过。

### P1

- **P1-1** 无 userinfo、无 password 字面量的普通错误消息，脱敏后除首尾空白外语义不变。
- **P1-2** 既有「CLI `--password` / `.env` password 不出现在 stdout/stderr」类测试不回归。

## 开放问题

- 无（已 approved；userinfo 占位字形由实现选定，验收以明文不出现且可测为准）。
