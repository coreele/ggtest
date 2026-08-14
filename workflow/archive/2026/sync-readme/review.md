# Review: sync-readme

## 审阅

### README.md
- `separator <delim>` → `separator=<delim>`：4 处全部更新 ✓
- CLI 用法行加 `[--help]` ✓
- CLI 选项表加 `--help, -h` 行 ✓
- 新增 "Header attributes" 节：timeout=、conn= 文档 + 示例 ✓
- 库用法节 separator 同步 ✓

### README.zh-CN.md
- separator 语法同步 ✓
- CLI 选项表加 `--help`、`-h` ✓
- 新增"头属性"节 ✓

### 验证
- `grep "separator <"` 两文件均 CLEAN
- 无代码变更，无需 mvn test

## 结论: Approve
