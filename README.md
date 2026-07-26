# GGTEST

Java CLI for running [sqllogictest](https://www.sqlite.org/sqllogictest)-format test
files against SQLite or PostgreSQL via JDBC (Maven, Java 17). Command name: **`ggtest`**.

## Prerequisites

- JDK 17+
- Apache Maven 3.8+
- For PostgreSQL runs: a reachable database user with `CREATE SCHEMA` / `DROP SCHEMA` (including `CASCADE`)

## Build, test, and package

```bash
mvn -q clean test
mvn -q clean package
```

`mvn package` produces an executable uber-JAR under `target/ggtest-*.jar`
(`Main-Class` = `com.ggtest.cli.Main`).

PostgreSQL-specific tests are **gated** by `GGTEST_PG_URL` (optional
`GGTEST_PG_USER` / `GGTEST_PG_PASSWORD`). When those gate variables are unset,
PG tests are skipped and the default suite still passes. Gate variables are
**not** the same as runtime config keys (`GGTEST_*` without `_PG_`).

## Run (`ggtest`)

After packaging:

```bash
./bin/ggtest --url jdbc:sqlite::memory: path/to/file.test
./bin/ggtest --url jdbc:postgresql://localhost:5432/dbname --engine postgres path/to/file.test
# or
java -jar target/ggtest-0.1.0-SNAPSHOT.jar --url jdbc:sqlite::memory: path/to/file.test
```

### CLI

```text
ggtest [--url <jdbc-url>] [--user <user>] [--password <password>]
       [--engine <name>=sqlite] [--hash-threshold <N>]
       [--env-file <path>] [--color <auto|always|never>]
       <file-or-dir> [<file-or-dir> ...]
```

| Option | Default | Notes |
|---|---|---|
| `--url` | (from env / `.env`) | JDBC URL; required from CLI, `GGTEST_URL`, or `.env` |
| `--user` / `--password` | none | Optional DB credentials; **never** written to logs or the report |
| `--engine` | `sqlite` | `sqlite` or `postgres` (case-insensitive); must match the URL scheme |
| `--hash-threshold` | `8` | Initial hash threshold per file; file-level `hash-threshold` still applies |
| `--env-file` | (CWD `.env`) | When set, **replaces** the default CWD `.env` (does not layer both) |
| `--color` | `auto` | `auto` (TTY only), `always`, or `never`; see color priority below |

Positional arguments: at least one file or directory.

- **File**: any extension is accepted; content must be valid sqllogictest.
- **Directory**: recursively collects `*.test` and `*.slt` (stable absolute-path order).

### Configuration priority (`.env`)

Field-level merge: **CLI flags > process environment > `.env` file**.

Runtime keys (whitelist): `GGTEST_URL`, `GGTEST_USER`, `GGTEST_PASSWORD`,
`GGTEST_ENGINE`, `GGTEST_HASH_THRESHOLD`. Unknown keys are ignored.

- Default file: `./.env` in the current working directory (missing is OK).
- `--env-file <path>`: only that file is read; missing/unreadable → exit code `2`.
- See [`.env.example`](.env.example) for placeholders. Do **not** commit a real `.env`.

| Concern | Variables |
|---|---|
| Runtime config | `GGTEST_URL`, `GGTEST_USER`, `GGTEST_PASSWORD`, `GGTEST_ENGINE`, `GGTEST_HASH_THRESHOLD` |
| Report color | `GGTEST_COLOR` (`auto` \| `always` \| `never`); also `-Dggtest.color=…` |
| Test gate (CI / local PG) | `GGTEST_PG_URL`, `GGTEST_PG_USER`, `GGTEST_PG_PASSWORD` |

**Color priority:** explicit `--color` > system property `ggtest.color` > env `GGTEST_COLOR` > default `auto`.
CI / pipes typically use `--color never`, `-Dggtest.color=never`, or rely on non-TTY `auto`.

### Engines and isolation

| Engine | URL prefix | Per-file isolation |
|---|---|---|
| `sqlite` (default) | `jdbc:sqlite:` | Independent connection (e.g. blank `:memory:` DB) |
| `postgres` | `jdbc:postgresql:` | Unique schema + `search_path`, then `DROP SCHEMA … CASCADE` |

Engine↔URL mismatch or unknown engine → exit code `2`, no connection, no execution.

Official sqllogictest corpora on PostgreSQL are **not** a hard acceptance criterion
(zero failures on PG select1/2/3 is optional exploration only).

### Expected results (value-per-line and row-wise)

After a `query` record’s expectation header, expected results may be written in either
form (inferred automatically per query from the type signature column count `C` and
that query’s column separator `S`):

| Form | Rule | Example (`query III`) |
|---|---|---|
| **Value-per-line** (default / official C style) | One normalized cell value per line | `1` / `2` / `3` on three lines |
| **Row-wise** (sqllogictest-rs style) | One result row per line; columns separated by `S` | `1 2 3` on one line |

Default `S` is a single space (U+0020), when the expectation header is exactly `----`.
To set a different separator **for this query only**, write it on the expectation header:

```text
query IIT nosort
SELECT 1, 1, 'hello world'
---- separator |
1 | 1 | hello world
```

That header both opens the expected body and binds `S` to `|` for this record only.
The next query that uses a plain `----` gets the default space separator again (no
file-level inheritance). Empty `<delim>` and other malformed `----…` lines are parse
errors. A top-level `---- separator …` (not under a query) is also a parse error.
Three-dash or bare `separator` lines are not recognized.

With an explicit `---- separator` header, tokens are trimmed after split; cell text is
the trimmed token as-is (no quote shell). Spaces inside a cell work with a delimiter
like `|` (example above). If a cell contains the current delimiter, pick a different
`S` or use value-per-line. Plain `----` keeps the default space row-wise rules
(consecutive spaces still produce empty tokens; no trim).

Single-column queries usually look the same in both forms. Hash expectations
(`N values hashing to <md5>`) are unchanged; row-wise only affects how plaintext
expected lines expand into the value sequence before compare/hash.

### Exit codes

| Code | Meaning |
|---|---|
| `0` | All assertable records passed |
| `1` | At least one assertion failure |
| `2` | Usage / config / parse / connection / fatal error |

Exit codes are independent of file-level `TOTAL` counts: a hard-error file increments
`TOTAL.failed` but still yields exit code `2`.

### Report

Stdout is a human-readable per-file report (paths relative to the process CWD).
Counts in `TOTAL:` are **file counts** (not query counts).

**Success:**

```text
examples/demo.slt                                            .. [PASSED] in 5 ms
examples/demo2.slt                                           .. [PASSED] in 6 ms

TOTAL: passed=2 failed=0 skipped=0
```

**Failure** (inline `[WHY]` / `[SQL]` / `[Diff]` git-style + `at file:line`; then
`Error:` listing **only failed** files):

```text
examples/demo.slt                                            .. [FAILED] in 18 ms
    [WHY] query result mismatch:
    [SQL] SELECT name ...
    [Diff] (-expected|+actual)
        apple
    -   bananad
    +   banana
        cherry
    at examples/demo.slt:22

Error: some test case failed:
[
    "examples/demo.slt",
]

TOTAL: passed=0 failed=1 skipped=0
```

**Mixed** (discovery/parameter order; failure details inline; no extra blank blocks
between success/skip lines; `Error:` only failed paths):

```text
examples/demo.slt                                            .. [FAILED] in 18 ms
    [WHY] query result mismatch:
    ...
    at examples/demo.slt:22

examples/demo2.slt                                           .. [PASSED] in 6 ms
examples/select1.test                                        .. [PASSED] in 142 ms

Error: some test case failed:
[
    "examples/demo.slt",
]

TOTAL: passed=2 failed=1 skipped=0
```

Skipped files print `.. [SKIPPED]` **without** timing. Hard errors use the same
visual system, count toward `TOTAL.failed`, and keep exit code `2`. Passwords are
never printed.

### Official corpus (user-supplied)

Official sqllogictest corpora are **not** shipped in this repository. Point
`ggtest` at your local copies, for example:

```bash
ggtest --url jdbc:sqlite::memory: /path/to/select1.test
ggtest --url jdbc:sqlite::memory: /path/to/select1.test /path/to/select2.test /path/to/select3.test
```

Optional automated hard acceptance: set `GGTEST_CORPUS_DIR` to a directory
containing `select1.test` / `select2.test` / `select3.test`, then `mvn test`.

## Library usage

The CLI assembles the same APIs you can call from Java.

### Parser

```java
import com.ggtest.parser.SqlLogicTestParser;
import com.ggtest.model.SqlTestRecord;
import java.nio.file.Path;
import java.util.List;

SqlLogicTestParser parser = new SqlLogicTestParser();
List<SqlTestRecord> records = parser.parse(Path.of("sample.test"));
```

Malformed input throws `com.ggtest.parser.ParseException` with message
`<sourceName>:<lineNumber>: <reason>`.

### Normalize / compare

```java
import com.ggtest.model.ColumnType;
import com.ggtest.model.SortMode;
import com.ggtest.normalize.ResultComparer;
import java.util.List;

var result = ResultComparer.compare(
        List.of(ColumnType.INTEGER, ColumnType.TEXT),
        SortMode.ROWSORT,
        ResultComparer.DEFAULT_HASH_THRESHOLD,
        "1\n(empty)\n2\nx\n",
        List.of(
                List.of("2", "x"),
                List.of("1", "")));

if (!result.passed()) {
    System.err.println(result.diffSummary());
}
```

### Runner

```java
import com.ggtest.db.sqlite.SqliteJdbcExecutor;
import com.ggtest.parser.SqlLogicTestParser;
import com.ggtest.runner.FileRunResult;
import com.ggtest.runner.SqlLogicTestRunner;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;

var records = new SqlLogicTestParser().parse(Path.of("sample.test"));
try (Connection connection = DriverManager.getConnection("jdbc:sqlite::memory:")) {
    FileRunResult result = new SqlLogicTestRunner(new SqliteJdbcExecutor(connection)).run(records);
}
```

Per-file state (hash-threshold, conditions, labels) is scoped to a single `run` call.
Column separator for row-wise expectations comes from each query’s expectation header.
Supporting another database: implement `com.ggtest.db.DatabaseExecutor`
(see `com.ggtest.db.postgres.PostgresJdbcExecutor`).
