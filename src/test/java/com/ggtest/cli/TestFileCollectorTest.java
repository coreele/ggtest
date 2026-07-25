package com.ggtest.cli;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class TestFileCollectorTest {

    @TempDir
    Path tempDir;

    @Test
    void collectsNestedTestAndSltFilesInStableOrder() throws IOException {
        Path nested = tempDir.resolve("nested");
        Files.createDirectories(nested);
        Path mid = Files.writeString(tempDir.resolve("mid.test"), "statement ok\nSELECT 1;\n");
        Path deep = Files.writeString(nested.resolve("deep.slt"), "statement ok\nSELECT 1;\n");
        Path alpha = Files.writeString(tempDir.resolve("alpha.test"), "statement ok\nSELECT 1;\n");
        Files.writeString(tempDir.resolve("ignore.txt"), "not a test");
        Files.writeString(nested.resolve("also.skip"), "not a test");

        List<Path> collected = TestFileCollector.collect(List.of(tempDir.toString()));

        assertEquals(
                List.of(
                        alpha.toAbsolutePath().normalize(),
                        mid.toAbsolutePath().normalize(),
                        deep.toAbsolutePath().normalize()),
                collected);
    }

    @Test
    void singleFileIsAcceptedWithoutRequiredExtension() throws IOException {
        Path custom = Files.writeString(tempDir.resolve("custom.logic"), "statement ok\nSELECT 1;\n");

        List<Path> collected = TestFileCollector.collect(List.of(custom.toString()));

        assertEquals(List.of(custom.toAbsolutePath().normalize()), collected);
    }

    @Test
    void explicitSltFileIsCollected() throws IOException {
        Path slt = Files.writeString(tempDir.resolve("sample.slt"), "statement ok\nSELECT 1;\n");

        List<Path> collected = TestFileCollector.collect(List.of(slt.toString()));

        assertEquals(List.of(slt.toAbsolutePath().normalize()), collected);
    }

    @Test
    void missingPathYieldsUsageError() {
        Path missing = tempDir.resolve("does-not-exist.test");

        UsageException ex = assertThrows(
                UsageException.class,
                () -> TestFileCollector.collect(List.of(missing.toString())));
        assertTrue(ex.getMessage().toLowerCase().contains("not found")
                || ex.getMessage().toLowerCase().contains("missing")
                || ex.getMessage().toLowerCase().contains("does not exist"));
    }

    @Test
    void emptyDirectoryYieldsUsageError() throws IOException {
        Path empty = Files.createDirectories(tempDir.resolve("empty"));

        UsageException ex = assertThrows(
                UsageException.class,
                () -> TestFileCollector.collect(List.of(empty.toString())));
        assertTrue(ex.getMessage().toLowerCase().contains("no")
                || ex.getMessage().toLowerCase().contains("empty"));
    }
}
