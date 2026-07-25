package com.ggtest.cli;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
import java.util.stream.Stream;
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
