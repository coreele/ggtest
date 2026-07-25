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
       [--env-file <path>]
       <file-or-dir> [<file-or-dir> ...]
```

| Option | Default | Notes |
|---|---|---|
| `--url` | (from env / `.env`) | JDBC URL; required from CLI, `GGTEST_URL`, or `.env` |
| `--user` / `--password` | none | Optional DB credentials; **never** written to logs or the report |
| `--engine` | `sqlite` | `sqlite` or `postgres` (case-insensitive); must match the URL scheme |
| `--hash-threshold` | `8` | Initial hash threshold per file; file-level `hash-threshold` still applies |
| `--env-file` | (CWD `.env`) | When set, **replaces** the default CWD `.env` (does not layer both) |

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
| Test gate (CI / local PG) | `GGTEST_PG_URL`, `GGTEST_PG_USER`, `GGTEST_PG_PASSWORD` |

### Engines and isolation

| Engine | URL prefix | Per-file isolation |
|---|---|---|
| `sqlite` (default) | `jdbc:sqlite:` | Independent connection (e.g. blank `:memory:` DB) |
| `postgres` | `jdbc:postgresql:` | Unique schema + `search_path`, then `DROP SCHEMA … CASCADE` |

Engine↔URL mismatch or unknown engine → exit code `2`, no connection, no execution.

Official sqllogictest corpora on PostgreSQL are **not** a hard acceptance criterion
(zero failures on PG select1/2/3 is optional exploration only).

### Exit codes

| Code | Meaning |
|---|---|
| `0` | All assertable records passed |
| `1` | At least one assertion failure |
| `2` | Usage / config / parse / connection / fatal error |

### Report

Stdout is plain text: per-file `passed` / `failed` / `skipped`, a `TOTAL` line,
and `FAILURE` lines with file, line, SQL first-line summary, and reason. Parse
and connection problems are reported as `ERROR` (exit code 2); remaining files
still run after a parse error. Passwords are never printed.

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

Per-file state (hash-threshold, conditions, labels) is scoped to a single
`run` call. Supporting another database: implement `com.ggtest.db.DatabaseExecutor`
(see `com.ggtest.db.postgres.PostgresJdbcExecutor`).
