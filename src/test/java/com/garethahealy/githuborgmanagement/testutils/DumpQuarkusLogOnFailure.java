package com.garethahealy.githuborgmanagement.testutils;

import io.quarkus.test.junit.main.QuarkusMainIntegrationTest;
import org.eclipse.microprofile.config.ConfigProvider;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.platform.commons.support.AnnotationSupport;

import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Prints the Quarkus log file when a {@link QuarkusMainIntegrationTest} fails.
 * <p>
 * {@code @Launch} asserts the process exit code from
 * {@code QuarkusMainIntegrationTestExtension.beforeEach}. On a mismatch that assertion
 * fails before the test method runs, so {@code LaunchResult.echoSystemOut()} is never called.
 * The launched process still writes its logs to {@code target/quarkus.log}.
 */
public class DumpQuarkusLogOnFailure implements AfterEachCallback {

    private final Path logFile;

    public DumpQuarkusLogOnFailure() {
        this(null);
    }

    DumpQuarkusLogOnFailure(Path logFile) {
        this.logFile = logFile;
    }

    @Override
    public void afterEach(ExtensionContext context) {
        dump(context, System.out);
    }

    void dump(ExtensionContext context, PrintStream out) {
        if (context.getExecutionException().isEmpty()) {
            return;
        }

        if (!AnnotationSupport.isAnnotated(context.getRequiredTestClass(), QuarkusMainIntegrationTest.class)) {
            return;
        }

        Path path = logFile == null ? resolveLogFile() : logFile;
        if (Files.isRegularFile(path)) {
            try {
                String contents = Files.readString(path);
                if (!contents.isBlank()) {
                    out.print(contents);
                    if (!contents.endsWith("\n")) {
                        out.println();
                    }
                }
            } catch (IOException e) {
                out.printf("Unable to read %s: %s%n", path.toAbsolutePath(), e.getMessage());
            }
        }
    }

    private static Path resolveLogFile() {
        String configured = System.getProperty("quarkus.log.file.path");
        if (configured == null || configured.isBlank()) {
            try {
                configured = ConfigProvider.getConfig()
                    .getOptionalValue("quarkus.log.file.path", String.class)
                    .orElse(null);
            } catch (RuntimeException ignored) {
                configured = null;
            }
        }

        if (configured != null && !configured.isBlank()) {
            return Path.of(configured);
        }

        if (Files.isDirectory(Path.of("build"))) {
            return Path.of("build", "quarkus.log");
        }

        return Path.of("target", "quarkus.log");
    }
}
