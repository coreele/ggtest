package com.ggtest.normalize;

import com.ggtest.model.ColumnType;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Infers a query type signature ({@code I}/{@code R}/{@code T} per column) from
 * raw result rows.
 *
 * <p>Value-driven and JDBC-free: for each column, non-{@code null} values are
 * inspected — all integers → {@code I}, else all reals → {@code R}, else
 * {@code T}. A column with no non-{@code null} value (all NULL) and an empty
 * result set fall back to {@code T}, since no stronger signal is available.
 */
public final class TypeSignatureInferer {

    private TypeSignatureInferer() {}

    /**
     * Infers the type signature from raw rows.
     *
     * @param rows raw result rows; each inner list is one row of column values
     *             ({@code null} means SQL NULL)
     * @return one {@link ColumnType} per column; empty when {@code rows} is empty
     */
    public static List<ColumnType> infer(List<List<String>> rows) {
        Objects.requireNonNull(rows, "rows");
        if (rows.isEmpty()) {
            return List.of();
        }
        int columns = rows.get(0).size();
        List<ColumnType> signature = new ArrayList<>(columns);
        for (int c = 0; c < columns; c++) {
            signature.add(inferColumn(rows, c));
        }
        return List.copyOf(signature);
    }

    private static ColumnType inferColumn(List<List<String>> rows, int column) {
        boolean sawNonNull = false;
        boolean allInteger = true;
        boolean allReal = true;
        for (List<String> row : rows) {
            String value = row.get(column);
            if (value == null) {
                continue;
            }
            sawNonNull = true;
            if (!isInteger(value)) {
                allInteger = false;
            }
            if (!isReal(value)) {
                allReal = false;
            }
        }
        if (!sawNonNull) {
            return ColumnType.TEXT;
        }
        if (allInteger) {
            return ColumnType.INTEGER;
        }
        if (allReal) {
            return ColumnType.REAL;
        }
        return ColumnType.TEXT;
    }

    private static boolean isInteger(String value) {
        try {
            Long.parseLong(value);
            return true;
        } catch (NumberFormatException ex) {
            return false;
        }
    }

    private static boolean isReal(String value) {
        try {
            Double.parseDouble(value);
            return true;
        } catch (NumberFormatException ex) {
            return false;
        }
    }
}
