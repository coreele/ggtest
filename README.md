# GGTEST

Java CLI for running [sqllogictest](https://www.sqlite.org/sqllogictest)-format test
files against SQLite, PostgreSQL, or MySQL via JDBC (Maven, Java 17). Command: **`ggtest`**.

## Prerequisites

- JDK 17+
- Apache Maven 3.8+
- For PostgreSQL / MySQL runs: a reachable database with appropriate privileges (`CREATE SCHEMA` / `DROP SCHEMA`)

## Build, test, and package

```bash
mvn -q clean test       # compile + unit/integration tests
mvn -q clean package    # produces target/ggtest-*.jar (shaded uber-JAR)
```

Database-specific tests (PostgreSQL / MySQL) are **gated** by environment
variables (`GGTEST_PG_URL` / `GGTEST_MY_URL`). When unset, those tests are
skipped and the default suite passes.

## Run (`ggtest`)

```bash
./bin/ggtest --url jdbc:sqlite::memory: examples/demo.slt
./bin/ggtest --url jdbc:postgresql://localhost:5432/db --engine postgres file.test
./bin/ggtest --url jdbc:mysql://localhost:3306 --engine mysql --user root file.test
```

Or `java -jar target/ggtest-*.jar --url jdbc:sqlite::memory: file.test`.

## CLI

```text
ggtest [options] <file-or-dir> [<file-or-dir> ...]
```

| Option | Default | Notes |
|---|---|---|
| `--url <jdbc-url>` | env `.env` | JDBC connection URL |
| `--user <user>` | none | Database user |
| `--password <pwd>` | none | Database password; never written to logs |
| `--engine <name>` | `sqlite` | `sqlite`, `postgres`, `mysql` — must match URL scheme |
| `--override` | off | Golden-update mode: rewrite `.slt` expected results with actual output (even if passing) |
| `--separator <s>` | off | Row-wise delimiter for `--override` output; requires `--override` |
| `--halt` | off | Stop at first failure (remaining records skipped) |
| `--parallel <N>` | off | Run up to `N` files concurrently; incompatible with `--override` |
| `--trace` | off | Print each SQL to stderr as executed |
| `--color <auto\|always\|never>` | `auto` | ANSI color control |
| `--hash-threshold <N>` | `8` | Hash result sets larger than N rows |
| `--env-file <path>` | `./.env` | Load config from `.env` file (replaces default) |
| `--help`, `-h` | — | Print usage and exit |

Exit codes: `0` = all passed · `1` = assertion failures · `2` = usage / config / fatal error.

See [AGENTS.md](AGENTS.md) for project architecture, package layout, and engineering knowledge.
