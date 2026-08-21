# GGTEST

Java CLI for running [sqllogictest](https://www.sqlite.org/sqllogictest)-format test
files against SQLite, PostgreSQL, MySQL, or XuguDB via JDBC (Maven, Java 17). Command: **`ggtest`**.

## Prerequisites

- JDK 17+
- Apache Maven 3.8+
- For PostgreSQL / MySQL / XuguDB runs: a reachable database with appropriate privileges (`CREATE SCHEMA` / `DROP SCHEMA`)

## Build, test, and package

```bash
mvn -q clean test       # compile + unit/integration tests
mvn -q clean package    # produces target/ggtest-*.jar (shaded uber-JAR)
```

Database-specific tests (PostgreSQL / MySQL / XuguDB) are **gated** by environment
variables (`GGTEST_PG_URL` / `GGTEST_MY_URL` / `GGTEST_XG_URL`). XuguDB also requires
the proprietary JDBC driver in `driver/`. When unset, those tests are skipped and
the default suite passes.

## Run (`ggtest`)

```bash
./bin/ggtest --url jdbc:sqlite::memory: examples/demo.slt
./bin/ggtest --url jdbc:postgresql://localhost:5432/db --engine postgres file.test
./bin/ggtest --url jdbc:mysql://localhost:3306 --engine mysql --user root file.test
./bin/ggtest --url jdbc:xugu://localhost:5138/SYSTEM --engine xugu --user SYSDBA file.test
```

Or `java -jar target/ggtest-*.jar --url jdbc:sqlite::memory: file.test`.

### Executable Markdown

Explicit `.md` file inputs are executable. GGTEST scans fenced code blocks whose
first info-string token is `sql`, `slt`, or `sqllogictest` (case-insensitive) and
runs those block contents as sqllogictest. Prose, fence lines, and unsupported
code blocks are ignored while original line numbers are preserved in reports.

```bash
./bin/ggtest --url jdbc:sqlite::memory: docs/example.md
```

Directory inputs still recurse only `*.test` and `*.slt`; pass `.md` files
explicitly. A Markdown file with no supported blocks passes with zero records.
`sql` fences are not a separate pure-SQL mode: their contents must be valid
sqllogictest, so plain SQL receives the normal parser error. `--override` works
for `.md` files and rewrites only the record ranges inside executable blocks.

### Output examples

**Pass:**

```text
examples/demo.slt                                            .. [PASSED] in 5 ms

TOTAL: passed=1 failed=0 skipped=0
```

**Failure** (inline detail with expected-vs-actual diff):

```text
examples/demo.slt                                            .. [FAILED] in 18 ms
    at examples/demo.slt:22 : query result mismatch
        (-expected|+actual)
        apple
    -   bananad
    +   banana
        cherry

Error: some test case failed:
[
    "examples/demo.slt",
]

TOTAL: passed=0 failed=1 skipped=0
```

**`--override` golden update** (rewrites expected results with actual output):

```text
examples/demo.slt                                            .. [OVERRIDDEN] in 210 ms

TOTAL: passed=0 failed=0 skipped=0 overridden=1
```

## CLI

```text
ggtest [options] <file-or-dir> [<file-or-dir> ...]
```

| Option | Default | Notes |
|---|---|---|
| `--url <jdbc-url>` | env `.env` | JDBC connection URL |
| `--user <user>` | none | Database user |
| `--password <pwd>` | none | Database password; never written to logs |
| `--engine <name>` | `sqlite` | `sqlite`, `postgres`, `mysql`, `xugu` — must match URL scheme |
| `--override` | off | Golden-update mode: rewrite `.slt` expected results with actual output (even if passing) |
| `--separator <s>` | off | Non-empty row-wise delimiter for `--override` output; requires `--override` |
| `--halt` | off | Stop at first failure (remaining records skipped) |
| `--parallel <N>` | off | Run up to `N` files concurrently; incompatible with `--override` |
| `--trace` | off | Print each SQL to stderr as executed |
| `--color <auto\|always\|never>` | `auto` | ANSI color control |
| `--hash-threshold <N>` | `8` | Initial per-file hash threshold; hash result sets larger than non-negative N values. `0` disables hashing until a file `hash-threshold` directive changes it |
| `--env-file <path>` | `./.env` | Load config from `.env` file (replaces default) |
| `--help`, `-h` | — | Print usage and exit |

Exit codes: `0` = all passed · `1` = assertion failures · `2` = usage / config / fatal error.

See [AGENTS.md](AGENTS.md) for project architecture, package layout, and engineering knowledge.
