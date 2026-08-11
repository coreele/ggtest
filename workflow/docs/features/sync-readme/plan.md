# Plan: sync-readme

## 元信息
- 路径等级: fast
- Review 门禁: required

## 任务

### README.md
1. `separator <delim>` → `separator=<delim>`（lines 102, 107, 116, 324）
2. CLI 用法行加 `[--help]`
3. CLI 选项表加 `--help, -h` 行
4. "Expected results" 节加 `timeout=<ms>` 和 `conn=<name>` 属性说明
5. "Statement expectations" 节加 timeout= / conn= 示例
6. 库用法节 separator 语法同步

### README.zh-CN.md
同步以上全部变更

## 验收
- README 中无 `separator <` 旧语法
- 包含 timeout=、conn=、--help 文档
