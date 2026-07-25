package com.ggtest.cli;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;

/**
 * Verifies the packaged executable artifact declares {@code Main-Class}
 * {@code com.ggtest.cli.Main}. Runs only when {@code target/*.jar} already exists
 * (e.g. after {@code mvn package}); otherwise skipped so {@code mvn test} stays green.
 */
class ExecutableJarManifestTest {

    @Test
    @EnabledIf("jarExists")
    void packagedJarDeclaresCliMainClass() throws IOException {
        Path jar = findPackagedJar();
        try (JarFile jarFile = new JarFile(jar.toFile())) {
            Manifest manifest = jarFile.getManifest();
            Attributes attrs = manifest.getMainAttributes();
            assertEquals("com.ggtest.cli.Main", attrs.getValue(Attributes.Name.MAIN_CLASS));
        }
    }

    @Test
    @EnabledIf("jarExists")
    void packagedJarMergesJdbcDriverSpiForSqliteAndPostgres() throws IOException {
        Path jar = findPackagedJar();
        try (JarFile jarFile = new JarFile(jar.toFile())) {
            ZipEntry entry = jarFile.getEntry("META-INF/services/java.sql.Driver");
            assertTrue(entry != null, "shaded JAR must contain META-INF/services/java.sql.Driver");
            try (InputStream in = jarFile.getInputStream(entry);
                    BufferedReader reader =
                            new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
                Set<String> providers = reader
                        .lines()
                        .map(String::trim)
                        .filter(line -> !line.isEmpty() && !line.startsWith("#"))
                        .collect(Collectors.toCollection(LinkedHashSet::new));
                assertTrue(providers.contains("org.sqlite.JDBC"), providers.toString());
                assertTrue(providers.contains("org.postgresql.Driver"), providers.toString());
            }
        }
    }

    @Test
    void launchScriptExistsAndInvokesJava() throws IOException {
        Path script = Path.of("bin/ggtest");
        assertTrue(Files.isRegularFile(script), "bin/ggtest must exist");
        String body = Files.readString(script);
        assertTrue(body.contains("java"), body);
        assertTrue(body.contains("com.ggtest.cli.Main") || body.contains("-jar"), body);
    }

    static boolean jarExists() {
        try {
            findPackagedJar();
            return true;
        } catch (IOException | IllegalStateException ex) {
            return false;
        }
    }

    private static Path findPackagedJar() throws IOException {
        Path target = Path.of("target");
        if (!Files.isDirectory(target)) {
            throw new IllegalStateException("target/ missing");
        }
        try (Stream<Path> stream = Files.list(target)) {
            return stream
                    .filter(path -> {
                        String name = path.getFileName().toString();
                        return name.startsWith("ggtest-") && name.endsWith(".jar") && !name.endsWith("-sources.jar");
                    })
                    .filter(path -> !path.getFileName().toString().contains("original"))
                    .findFirst()
                    .orElseThrow(() -> new IllegalStateException("no ggtest jar in target/"));
        }
    }
}
