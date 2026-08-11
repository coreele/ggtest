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
# Feature showcase (English / Chinese comments; same assertions)
./bin/ggtest --engine sqlite --url jdbc:sqlite::memory: examples/demo.slt examples/demo_zh.slt

./bin/ggtest --url jdbc:sqlite::memory: path/to/file.test
./bin/ggtest --url jdbc:postgresql://localhost:5432/dbname --engine postgres path/to/file.test
# or
java -jar target/ggtest-0.1.0-SNAPSHOT.jar --url jdbc:sqlite::memory: path/to/file.test
```

### CLI

```text
ggtest [--url <jdbc-url>] [--user <user>] [--password <password>]
       [--engine <name>=sqlite] [--hash-threshold <N>]
       [--env-file <path>] [--color <auto|always|never>] [--halt] [--parallel <N>] [--override] [--help]
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
| `--halt` | off | Stop when the first error is seen (assertion failure or hard error). Later records in the file are skipped (not executed, not reported as failures) and not-yet-started files are not opened or counted. Exit-code priority is unchanged. A corpus `halt` record is unaffected (it skips the rest of one file but is not an error). |
| `--parallel` | off | Run at most `N` files concurrently (N >= 1). `--parallel 1` is equivalent to sequential execution. Report structure is identical regardless of parallel mode: status lines keep input-file order, single-file blocks are never interleaved. When combined with `--halt`, already-running files complete naturally while queued files are cancelled. Cannot be used with `--override`. |
| `--override` | off | Golden-update mode: rewrite the expected interval of in-scope mismatches (query result mismatch; `statement error` message mismatch) in the source `.slt` file using the actual output. Overridden records show `[OVERRIDDEN]` and do not count as failures; scope-out mismatches (label conflict, execution failure, type-signature error, polarity flip) still `FAILED`. The file is written at most once (atomic temp+rename); files without in-scope mismatch are not touched. Exit-code priority is unchanged. Does not change parser / comparison / normalization semantics. Cannot be used with `--parallel`. |
| `--help`, `-h` | — | Print usage information and exit with code 0. |

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

### Expected results

After a `query` record’s expectation header (`----`), expected results are either
**value-per-line** (default) or **row-wise** when the query header declares
`separator=<delim>`:

| Form | Trigger | Example (`query III`) |
|---|---|---|
| **Value-per-line** (default) | Exact `----`, no query-head `separator=` | `1` / `2` / `3` on three lines |
| **Row-wise** | Query head `separator=<delim>` + exact `----` | `1 \| 1 \| hello world` on one line |

Value-per-line treats each physical line as one cell (spaces in TEXT are kept).
There is no space-based row-wise inference: `1 2 3` on one line under plain `----`
is a single cell value `"1 2 3"`.

Declare a row-wise delimiter on the **query** header (not on `----`):

```text
query IIT nosort separator=|
SELECT 1, 1, 'hello world'
----
1 | 1 | hello world
```

`delim` must be a single whitespace-delimited token (multi-character allowed; no
embedded whitespace). Each expected line must split into exactly `C` tokens
(signature length) or the compare fails with a readable message (line number,
actual token count, `C`). Tokens are trimmed; empty tokens become `(empty)`.
`separator` without `=` as the last header token is still a **label**. A removed
`---- separator …` expectation header is a parse error — use the query-head form
above. If a cell contains the current delimiter, pick a different `delim` or use
value-per-line (no quote shell).

Hash expectations (`N values hashing to <md5>`) are recognized first and unchanged.


### Statement expectations

A `statement ok` record asserts its SQL executes successfully; `statement error`
asserts it fails. `statement error` may optionally be followed by an expected
error message — the remaining text after the `error` keyword (whitespace
preserved verbatim; `#` is treated as a literal character, not stripped as a
comment):

```text
statement error no such table
SELECT * FROM missing_table
```

When a message is present, the statement must fail **and** the returned error
summary (`errorSummary`) must **contain** the expected message as a
**case-insensitive sub-string** (plain substring containment — not a regex, not
exact equality). A message that is empty or whitespace-only is treated as no
message.

| `statement error` form | Outcome |
|---|---|
| No message | Only verifies execution failure (backward compatible; behavior unchanged) |
| `<message>`, but execution succeeds | Fail: `statement expected to fail but succeeded` |
| `<message>`, execution fails but error summary does not contain `<message>` | Fail: `statement error message mismatch` (reported diff-style, expected vs actual) |
| `<message>`, execution fails and error summary contains `<message>` | Pass |


### Header attributes (`timeout=`, `conn=`)

Both `statement` and `query` headers support key=value attributes after the
mandatory tokens:

| Attribute | Scope | Effect |
|---|---|---|
| `timeout=<ms>` | statement, query | Maximum execution time in milliseconds; 0 or absent = no limit. Implemented via JDBC `setQueryTimeout` (seconds, rounded up). SQLite JDBC may not enforce it. Timeout → record FAILED (not fatal). |
| `conn=<name>` | statement, query | Uses a named connection independent from the default. Each distinct name opens a separate JDBC connection to the same URL. Records without `conn=` use the default connection. Enables multi-connection / concurrent transaction tests. |

```text
statement ok conn=c1
BEGIN;

statement error conn=c2 timeout=2000
UPDATE accounts SET balance = 0 WHERE id = 1;

query II nosort
SELECT id, balance FROM accounts ORDER BY id
----
1
100
2
200
```


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
examples/demo_zh.slt                                         .. [PASSED] in 6 ms

TOTAL: passed=2 failed=0 skipped=0
```

**Failure** (each failure block starts with `    at file:line : reason`; diff
shown only when mismatch; `Error:` listing **only failed** files):

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

**Multiple failures in one file** (blocks stack without blank lines;
`TOTAL.failed` stays a **file** count):

```text
examples/multi.slt                                           .. [FAILED] in 40 ms
    at examples/multi.slt:480 : query execution failed: ... integer overflow ...
    at examples/multi.slt:484 : query execution failed: ... integer overflow ...
    at examples/multi.slt:491 : query result mismatch
        (-expected|+actual)
            ...
    -   ...
    +   ...

Error: some test case failed:
[
    "examples/multi.slt",
]

TOTAL: passed=0 failed=1 skipped=0
```

**Mixed** (discovery/parameter order; failure details inline; no extra blank blocks
between success/skip lines; `Error:` only failed paths):

```text
examples/demo.slt                                            .. [FAILED] in 18 ms
    at examples/demo.slt:22 : query result mismatch
        (-expected|+actual)
        ...

examples/demo_zh.slt                                         .. [PASSED] in 6 ms

examples/demo_zh.slt                                         .. [PASSED] in 6 ms
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
Row-wise column separators come from each query header’s optional `separator=<delim>`.
Supporting another database: implement `com.ggtest.db.DatabaseExecutor`
(see `com.ggtest.db.postgres.PostgresJdbcExecutor`).
