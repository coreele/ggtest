# GGTEST feature showcase (sqllogictest format)

> Covers record types, expectation forms, and PostgreSQL PL/pgSQL.
> Pair with examples/demo_zh.slt for the Chinese commentary twin.
>
> SQLite:
> ./bin/ggtest --engine sqlite --url jdbc:sqlite::memory: examples/demo.slt
>
> PostgreSQL (all sections including PL/pgSQL):
> ./bin/ggtest --engine postgres \
> --url 'jdbc:postgresql://localhost:5432/postgres' \
> --user postgres [--password ...] \
> examples/demo.slt

## setup
```sql
statement ok
DROP TABLE IF EXISTS items;

statement ok
CREATE TABLE items(id INTEGER, name TEXT, price REAL);

statement ok
INSERT INTO items VALUES
  (2, 'banana', 0.5),
  (1, 'apple', 1.25),
  (3, 'cherry', 3.0),
  (4, '', 9.0),
  (5, NULL, NULL);
```

## I / T / R type signatures + sort modes

> T + rowsort: result is re-sorted lexicographically
```sql
query T rowsort
SELECT name FROM items WHERE id <= 3;
----
apple
banana
cherry
```

> I + nosort: order must match ORDER BY
```sql
query I nosort
SELECT id FROM items WHERE id <= 3 ORDER BY id;
----
1
2
3
```

> R: REAL formatted as %.3f
```sql
query R nosort
SELECT price FROM items WHERE id <= 3 ORDER BY id;
----
1.250
0.500
3.000
```

> valuesort: flatten all cells then sort as strings
```sql
query II valuesort
SELECT id, CAST(price AS INTEGER) FROM items WHERE id IN (1, 2) ORDER BY id;
----
0
1
1
2
```

## NULL and (empty)

```sql
query T nosort
SELECT name FROM items WHERE id = 4;
----
(empty)

query T nosort
SELECT name FROM items WHERE id = 5;
----
NULL
```

## value-per-line vs row-wise (separator=<delim>)

> Plain ---- is always value-per-line (one physical line = one cell)
```sql
query IIT nosort
SELECT 1, 1, 'hello world';
----
1
1
hello world
```

> Row-wise: declare separator=<delim> on the query header; tokens are trimmed
```sql
query IIT nosort separator=|
SELECT 1, 1, 'hello world';
----
1 | 1 | hello world
```

## execute-only (no ---- block): run SQL, do not compare
```sql
query I nosort
SELECT id FROM items WHERE id = 1;
```

## label: same label must yield the same result view
```sql
query I nosort same_id
SELECT id FROM items WHERE id = 1;
----
1

query I nosort same_id
SELECT id FROM items WHERE name = 'apple';
----
1
```

## statement error: execution MUST fail
```sql
statement error
SELECT * FROM missing_table;
```

## skipif / onlyif (engine name case-insensitive)

> sqlite: first skipped, second runs, third skipped.
> postgres: first runs, second skipped, third runs.

```sql
skipif sqlite
statement ok
SELECT 1;

onlyif sqlite
statement ok
SELECT 1;

onlyif postgres
statement ok
SELECT 1;
```

## hash-threshold: above N values → MD5 form
```sql
hash-threshold 1

query I nosort
SELECT id FROM items WHERE id IN (1, 2) ORDER BY id;
----
2 values hashing to 6ddb4095eb719e2a9f0a3f95677d24e0
```

## teardown
```sql
statement ok
DROP TABLE IF EXISTS items;
```

# PostgreSQL PL/pgSQL — requires --engine postgres

> All records guarded with onlyif postgres; clean run on SQLite.

## Scalar function

```sql
onlyif postgres
statement ok
CREATE OR REPLACE FUNCTION get_square(num integer)
RETURNS integer AS $$
BEGIN
    RETURN num * num;
END
$$ LANGUAGE plpgsql

onlyif postgres
query I
SELECT get_square(5);
----
25
```

## RETURN QUERY / SETOF

```sql
onlyif postgres
statement ok
CREATE OR REPLACE FUNCTION fibonacci(lim integer)
RETURNS SETOF integer AS $$
DECLARE
    a integer := 0;
    b integer := 1;
    t integer;
BEGIN
    RETURN NEXT a;
    WHILE b <= lim LOOP
        RETURN NEXT b;
        t := a + b;
        a := b;
        b := t;
    END LOOP;
    RETURN;
END
$$ LANGUAGE plpgsql

hash-threshold 10

onlyif postgres
query I nosort
SELECT * FROM fibonacci(13);
----
0
1
1
2
3
5
8
13
```


## PL/pgSQL cleanup

```sql
onlyif postgres
statement ok
DROP FUNCTION IF EXISTS get_square(integer);

onlyif postgres
statement ok
DROP FUNCTION IF EXISTS fibonacci(integer);
```

> halt: everything after is skipped
```sql
halt
```

> This statement will be skipped
```sql
statement ok
INSERT INTO absent_after_halt VALUES (1);
```
