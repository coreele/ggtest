# 工作项: tighten-cli-boundary-validation

描述: 收紧 CLI 边界校验与连接异常归一化
目标分支: main
源分支: tighten-cli-boundary-validation
基线提交: ecc6a8b8d4472b4c765a7465e19bf97d4c123540
文档影响: README.md, README.zh-CN.md

> **本文件须保存为 `workflow/archive/2026/tighten-cli-boundary-validation/tighten-cli-boundary-validation.md`**，文件名与目录同名。
> 流程定义见 `workflow/WORKFLOW.md`；看板见 `workflow/STATUS.md`。
> 本工作项的全部产物平铺在 `workflow/archive/2026/tighten-cli-boundary-validation/`，无子目录、无版本后缀。
> 表内只填枚举、短标签或路径；理由与长说明写进「进度笔记」。

## 门禁

| 路径等级 | Spec | Spec 用户确认 | Design | Review |
|---|---|---|---|---|
| fast | skipped | not-required | skipped | skipped |

## 状态

| 状态 | 下一步 | 阻塞原因 | 恢复条件 | 恢复后目标 |
|---|---|---|---|---|
| archived | — |  |  |  |

## 进度笔记

- 2026-08-21：用户要求开启任务“CLI 边界校验可收紧”。范围限定为边界校验与错误映射：负数 `--hash-threshold`、空 `--separator`、Xugu JDBC 连接阶段 RuntimeException 归一化。用户补充本机 Xugu 环境：`127.0.0.1:5138/SYSTEM`，用户 `SYSDBA`，密码已脱敏。
- 2026-08-21：判定 fast；Spec/Design/Review skipped，理由是需求已明确且仅涉及 CLI 输入校验、连接错误归一化与文档同步，不改变核心执行模型。
- 2026-08-21：Developer 完成实现与自验，用户确认目标同步；QA 首轮 Pass。状态 `developing → merge-approval`，等待用户授权合并；`qa-report.md` 按 git 规范留工作树，授权后与 `done` 一次提交。
- 2026-08-21：合并授权前用户反馈 `--hash-threshold 0` 在 `select1.test` 中仍 hash。核对为文件首行 `hash-threshold 8` 覆盖 CLI 初始阈值；补充文档/help 与 runner 行为测试后复验 Pass。状态保持 `merge-approval`。
- 2026-08-21：用户授权合并，并要求合并前压缩源分支提交、将规则写入 Git 规范。状态 `merge-approval → done`；合入前按新规范整理为少量语义提交，随后 fast-forward 合入 `main`。
- 2026-08-21：`tighten-cli-boundary-validation` fast-forward 合入 `main`，本地源分支已删除；工作项归档至 `workflow/archive/2026/tighten-cli-boundary-validation/`。
