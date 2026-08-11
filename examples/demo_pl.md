# GGTEST PL/pgSQL 功能展示 (sqllogictest format)

> 本文件演示 ggtest 对 PostgreSQL PL/pgSQL 的支持，涵盖函数创建、调用、
> 控制流、集合返回等常见模式。仅适用于 PostgreSQL engine。
>
> 使用方式（.env 已配好 PostgreSQL 时直接运行）：
> ./bin/ggtest examples/demo_pl.slt
>
> 显式指定 engine：
> ./bin/ggtest --engine postgres \
> --url 'jdbc:postgresql://localhost:5432/postgres' \
> --user postgres [--password ...] \
> examples/demo_pl.slt

## 标量函数 — 基本参数与返回值

```sql
statement ok
DROP FUNCTION IF EXISTS get_square(integer)

statement ok
CREATE OR REPLACE FUNCTION get_square(num integer)
RETURNS integer AS $$
BEGIN
    RETURN num * num;
END
$$ LANGUAGE plpgsql

query I
SELECT get_square(5)
----
25

query I
SELECT get_square(-3)
----
9
```

## DECLARE 局部变量

```sql
statement ok
DROP FUNCTION IF EXISTS calc_discount(numeric, numeric)

statement ok
CREATE OR REPLACE FUNCTION calc_discount(price numeric, rate numeric)
RETURNS numeric AS $$
DECLARE
    discounted numeric;
BEGIN
    discounted := price - (price * rate / 100);
    RETURN discounted;
END
$$ LANGUAGE plpgsql

query RR nosort
SELECT calc_discount(100, 10), calc_discount(50, 20)
----
90.000
40.000
```

## IF / ELSIF / ELSE 条件分支

```sql
statement ok
DROP FUNCTION IF EXISTS grade_score(integer)

statement ok
CREATE OR REPLACE FUNCTION grade_score(score integer)
RETURNS text AS $$
BEGIN
    IF score >= 80 THEN
        RETURN 'A';
    ELSIF score >= 60 THEN
        RETURN 'B';
    ELSE
        RETURN 'C';
    END IF;
END
$$ LANGUAGE plpgsql

query T nosort
SELECT grade_score(90)
----
A

query T nosort
SELECT grade_score(65)
----
B

query T nosort
SELECT grade_score(40)
----
C
```

## RETURN QUERY / SETOF — 返回结果集

```sql
statement ok
DROP FUNCTION IF EXISTS fibonacci(integer)

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

query I nosort
SELECT * FROM fibonacci(13)
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

## LOOP + EXIT WHEN + CONTINUE

```sql
statement ok
DROP FUNCTION IF EXISTS sum_evens(integer)

statement ok
CREATE OR REPLACE FUNCTION sum_evens(n integer)
RETURNS integer AS $$
DECLARE
    i     integer := 1;
    total integer := 0;
BEGIN
    LOOP
        EXIT WHEN i > n;
        IF i % 2 <> 0 THEN
            i := i + 1;
            CONTINUE;
        END IF;
        total := total + i;
        i := i + 1;
    END LOOP;
    RETURN total;
END
$$ LANGUAGE plpgsql

query I
SELECT sum_evens(10)
----
30

query I
SELECT sum_evens(5)
----
6
```

## 默认参数值

```sql
statement ok
DROP FUNCTION IF EXISTS greet(text, text)

statement ok
CREATE OR REPLACE FUNCTION greet(who text, prefix text DEFAULT 'Hello')
RETURNS text AS $$
BEGIN
    RETURN prefix || ', ' || who || '!';
END
$$ LANGUAGE plpgsql

query T nosort
SELECT greet('World')
----
Hello, World!

query T nosort
SELECT greet('GGTEST', 'Hi')
----
Hi, GGTEST!
```

## RETURNS TABLE — 行结构返回

```sql
statement ok
DROP FUNCTION IF EXISTS split_name(text)

statement ok
CREATE OR REPLACE FUNCTION split_name(full_name text)
RETURNS TABLE(first text, last text) AS $$
BEGIN
    first := split_part(full_name, ' ', 1);
    last  := split_part(full_name, ' ', 2);
    RETURN NEXT;
END
$$ LANGUAGE plpgsql

query TT nosort separator |
SELECT * FROM split_name('John Smith')
----
John | Smith
```

## 异常处理 — EXCEPTION WHEN

```sql
statement ok
DROP FUNCTION IF EXISTS safe_divide(numeric, numeric)

statement ok
CREATE OR REPLACE FUNCTION safe_divide(a numeric, b numeric)
RETURNS numeric AS $$
BEGIN
    RETURN a / b;
EXCEPTION
    WHEN division_by_zero THEN
        RETURN NULL;
END
$$ LANGUAGE plpgsql

query R
SELECT safe_divide(10, 2)
----
5.000

query T
SELECT CAST(safe_divide(10, 0) AS text)
----
NULL
```

## 验证 safe_divide 确实捕获了除零异常（返回 NULL 而非抛错）

```sql
query T
SELECT safe_divide(10, 0) IS NULL
----
t
```

## STATEMENT ERROR — 不捕获的异常会向客户端抛错

```sql
statement error division by zero
SELECT 1 / 0
```

## 清理

```sql
statement ok
DROP FUNCTION IF EXISTS get_square(integer)

statement ok
DROP FUNCTION IF EXISTS calc_discount(numeric, numeric)

statement ok
DROP FUNCTION IF EXISTS grade_score(integer)

statement ok
DROP FUNCTION IF EXISTS fibonacci(integer)

statement ok
DROP FUNCTION IF EXISTS sum_evens(integer)

statement ok
DROP FUNCTION IF EXISTS greet(text, text)

statement ok
DROP FUNCTION IF EXISTS split_name(text)

statement ok
DROP FUNCTION IF EXISTS safe_divide(numeric, numeric)
```
