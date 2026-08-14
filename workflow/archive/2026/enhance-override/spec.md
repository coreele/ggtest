# Spec: enhance-override

> **feature-id**：`enhance-override` · 路径等级 `standard`（Spec 门禁 required + 用户确认 required）

## 背景与目标

`--override` 目前仅在「期望与实际结果不一致」时回写期望体；遇到以下两类情况直接 `FAILED`，无法自动修复：
- 类型签名不匹配（如 `row width 2 != type signature length 1`）；
- SQL 执行失败（如 `no such table: missing_table`）。

目标：强化 `--override`，新增三类自动修复——① 类型签名对齐；② 执行失败改写为 `statement error`；③ 支持 `--override-separator` 控制期望回写格式。

## 非目标

- 不修改 .slt 解析、测试执行路径与 sort mode。
- 不新增 `.sql` 后缀自动路由生成（那是 `sql-to-slt`，已 blocked）。
- 不改 `--override` 既有的「不匹配期望回写」语义（继续保留）。

## 范围与可见行为

1. **类型签名对齐**：`--override` 下，query 的类型签名与实际结果不匹配（列数或类型）时，按实际结果**自动推断**签名并改写 query 头的类型签名，再按新签名归一化回写实际结果。
2. **执行失败 → statement error**：语句/查询执行失败（业务 SQL 错误）时，`--override` 把该记录改写为 `statement error <实际消息>`（脱敏）。query 记录改写为 statement error 时删除其 `----` 期望块。
3. **separator 参数**：新增 `--separator <delim>`（`--override-separator` 的简写）。指定时 query 头写入 `separator=<delim>` 且期望按行式回写（行内列以 `delim` 连接）；**即便用例 PASS 也强制以新 separator 重写其期望块**。未指定时默认 value-per-line（现有行为）。

## 合同

- **类型推断**（与 `sql-to-slt` 一致）：每列按非 NULL 值分类——全整数 → `I`，全实数 → `R`，否则 → `T`；全 NULL 列或空结果集 → JDBC `ResultSetMetaData` 映射回退（INTEGER 族→I、FLOAT/REAL/DOUBLE/DECIMAL/NUMERIC→R、其余→T），元数据不可用 → 默认 `T`。签名长度 = 实际列数。
- **statement error 消息**：经 `CredentialRedaction` 脱敏。
- **新 CLI flag**：`--separator <delim>`（`--override-separator` 简写）；`delim` 不得含空白；仅与 `--override` 联合生效，单独使用报 UsageException。指定时**所有**成功执行的 query（含 PASS）均以行式重写期望 + 注入 separator 属性。
- **自洽**：`--override` 后重跑该 .slt 全绿。

## 验收

P0（必须可验证）：
- P0-1 签名对齐：`query T` 实际返回 2 列文本 → `--override` 后签名改写为 `TT`，重跑全绿。
- P0-2 列数对齐：`row width 2 != length 1` 的情形被自动修正（不再 FAILED）。
- P0-3 查询执行失败 → `statement error`：`SELECT * FROM missing_table` → 改写为 `statement error`（脱敏），重跑全绿。
- P0-4 `statement ok` 实际失败 → 改写为 `statement error <消息>`。
- P0-5 separator：`--separator "|"` → query 头含 `separator=|`，期望按行式（`a|b`）。
- P0-6 默认格式不变：未指定 separator 时仍 value-per-line。
- P0-7 **PASS 记录也重写**：`--separator` 下，即便 query 全绿（PASS），其期望块也以行式重写、头注入 separator。
- P0-8 回归：既有 `--override` 不匹配回写、正常测试、非 override 运行不回归。

P1（重要，非阻塞）：
- P1-1 既有 `statement error <旧消息>` 失败且消息不符 → 更新为实际消息。
- P1-2 全 NULL 列 / 空结果集 → 按回退策略产出合法签名。

## 开放问题

无（回退策略沿用 `sql-to-slt` 已定决策；separator flag 名取 `--override-separator`）。
