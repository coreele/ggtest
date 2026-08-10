# SQL Logic Test (local)

Syntax highlighting for `.slt` / `.test` (sqllogictest). **Not part of the ggtest git repo.**

## Install (Cursor / VS Code)

**Option A — from folder**

1. Command Palette → `Extensions: Install from Location…`
2. Select this folder: `vs-slt` (repo root)
3. Re-open a `.slt` or `.test` file; language mode should be **SQL Logic Test**

**Option B — VSIX (CLI)**

```bash
cd vs-slt
npx --yes @vscode/vsce package --out sqllogictest-0.0.1.vsix
cursor --install-extension ./sqllogictest-0.0.1.vsix
```

Then reload the window if needed.

## Manual check

Open `examples/demo.slt` in the ggtest workspace and confirm:

- `#` comments
- directives (`statement`, `query`, …)
- SQL keywords inside bodies
- `----` / `---- separator <delim>` expectation headers and result lines (not SQL), including `(empty)`
