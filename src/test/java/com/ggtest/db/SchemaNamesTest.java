package com.ggtest.db;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class SchemaNamesTest {

    @Test
    void generateProducesSafePrefixedIdentifier() {
        for (int i = 0; i < 50; i++) {
            String name = SchemaNames.generate();
            assertTrue(name.startsWith("ggtest_"), "missing prefix: " + name);
            assertTrue(SchemaNames.isSafe(name), "generated name not safe: " + name);
        }
    }

    @Test
    void isSafeAcceptsLowercaseLettersDigitsUnderscores() {
        assertTrue(SchemaNames.isSafe("ggtest_abc123"));
        assertTrue(SchemaNames.isSafe("a"));
        assertTrue(SchemaNames.isSafe("z_9"));
    }

    @Test
    void isSafeRejectsUnsafeIdentifiers() {
        assertFalse(SchemaNames.isSafe(null));
        assertFalse(SchemaNames.isSafe(""));
        assertFalse(SchemaNames.isSafe("1abc"), "leading digit");
        assertFalse(SchemaNames.isSafe("Abc"), "uppercase");
        assertFalse(SchemaNames.isSafe("ggtest; DROP TABLE t"), "sql metacharacters");
        assertFalse(SchemaNames.isSafe("a b"), "whitespace");
        assertFalse(SchemaNames.isSafe("a-b"), "hyphen");
        assertFalse(SchemaNames.isSafe("';--"));
    }

    @Test
    void requireSafeReturnsValidName() {
        assertEquals("ggtest_x", SchemaNames.requireSafe("ggtest_x"));
    }

    @Test
    void requireSafeThrowsForUnsafeName() {
        assertThrows(IllegalArgumentException.class, () -> SchemaNames.requireSafe("evil'; DROP"));
        assertThrows(IllegalArgumentException.class, () -> SchemaNames.requireSafe(null));
        assertThrows(IllegalArgumentException.class, () -> SchemaNames.requireSafe("1abc"));
    }
}
