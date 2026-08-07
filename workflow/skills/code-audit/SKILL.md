---
name: code-audit
description: >-
  Use when the user asks for 代码审计, 代码质量审计, code audit, auditing src
  against code-audit standards, or reviewing known issues, tech debt, or
  optimization opportunities in repository source.
---

# code-audit — 代码质量审计

对照 **代码审计专用标准** 审计 `src/`，输出违规 Findings 与关注项（Known Issue / Tech Debt / 优化）。

**审计是只读活动，禁止修改任何代码。** 本 skill 执行期间不得编辑、新建、删除 `src/`（或任何）源码文件，也不得执行会改动工作区代码的命令。修复须在审计结束后由用户另行发起，不属于本 skill。

## 标准与扩展（可扩展）

| 资源 | 路径 | 加载规则 |
|---|---|---|
| 基线标准 | `workflow/docs/standards/code-audit.md` | 必读 |
| 专项标准 | `workflow/docs/standards/code-audit-<topic>.md` | 自动加载（见下方通配） |
| 关注项登记册 | `workflow/docs/standards/code-audit-register.md` | **若存在则必读**（不当作条款标准） |
| 其他标准 | 用户点名的文件 | 仅显式指定时追加 |

**通配：** 加载 `workflow/docs/standards/code-audit*.md`，但**排除** `code-audit-register.md`。  
**不要**加载 feature 周期规范（如 `quality.md`、`git.md`、`documentation.md`），除非用户明确点名。

新增专项：按基线 §10 写 `code-audit-<topic>.md`。登记债务/已知问题：写入登记册。

## 范围

| 项 | 规则 |
|---|---|
| 代码 | 默认 `src/`；可收窄到其子路径；禁止扩大到 `src/` 外 |
| 标准 | `code-audit*.md`（排除 register）+ 可选登记册 + 用户点名 |
| 产出 | 违规 Findings **与** 关注项（Known Issue / Tech Debt / 优化）；结论写入 `workflow/docs/audit/` |
| 改动 | **禁止修改源码**（`src/` 等）。只读取证；即使发现 Critical 也不顺手修。允许写入审计报告到 `workflow/docs/audit/` |

## 执行步骤

1. 枚举并完整读取 `workflow/docs/standards/code-audit*.md`（排除 `code-audit-register.md`）。
2. 若存在 `code-audit-register.md`，读取并记下待核对条目（不作条款源）。
3. 排除登记册后无一标准文件：**停止**，提示添加基线标准；禁止编造条款，禁止用 `quality.md` 顶替。
4. 确定审计路径（默认 `src/` 或用户子路径）。
5. 按标准条款取证 → 违规 Findings；按基线 §8 与标记（TODO/FIXME/HACK 等）收集关注项。
6. 对照登记册：仍存在则报告并建议更新；疑似已修复则标 `resolved` 建议。
7. 按下方模板输出报告，并写入 `workflow/docs/audit/YYYY-MM-DD-<scope>.md`（scope 如 `src` 或收窄后的路径别名）。对话中可摘要，**完整结论以该文件为准**。

## 报告模板

落盘：`workflow/docs/audit/YYYY-MM-DD-<scope>.md`（例如 `workflow/docs/audit/2026-07-26-src.md`）。正文使用下方结构：

```markdown
# 代码质量审计报告

## 范围
- 代码：<path>
- 标准：<code-audit*.md 列表>
- 登记册：<path 或「无」>

## 摘要
- Findings — Critical: N | High: N | Medium: N | Low: N | Info: N
- 关注项 — Known Issue: N | Tech Debt: N | 优化: N
- <一句话质量结论>

## Findings（违规）
| 级别 | 标准 | 位置 | 问题 | 建议 |
|---|---|---|---|---|
| ... | workflow/docs/standards/code-audit.md§N | path:line | ... | ... |

## 关注项（Known Issue / Tech Debt / 优化）
| 类型 | 级别 | 位置 | 描述 | 影响或收益 | 建议下一步 | 登记册 |
|---|---|---|---|---|---|---|
| Tech Debt | Low | path:line | ... | ... | ... | 新发现 / CA-xxx |

## 登记册核对
| ID | 原状态 | 审计结论 | 建议 |
|---|---|---|---|
| CA-001 | open | 仍存在 / 疑似已修复 | 保持 / 改为 resolved |

## 覆盖说明
- 已读：<文件列表>
- 未覆盖条款（若有）：<说明>
```

无对应条目时表内写一行「无」。「标准」列须含路径与条款定位。

## 边界

| 情况 | 行为 |
|---|---|
| 无 `code-audit*.md` | 停止并提示添加 |
| 无登记册 | 跳过核对；仍须从代码收集关注项 |
| 条款无法落到范围 | 覆盖说明中注明 |
| 疑似敏感信息 | 报告位置与类型；不回显完整密钥 |
| 与 Bugbot / Security Review | 不替代 |
| 安全类 | 只报风险与偏差；不写 exploit / PoC |
| 用户在审计中要求修复 | 先完成并落盘报告；修复作为审计外的新任务处理，不在本 skill 内改源码 |
| 报告落盘 | 写入 `workflow/docs/audit/`；这不属于「修改源码」 |
