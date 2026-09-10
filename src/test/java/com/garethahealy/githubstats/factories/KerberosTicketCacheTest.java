package com.garethahealy.githubstats.factories;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class KerberosTicketCacheTest {

    @TempDir
    Path tempDir;

    @Test
    void resolveConfiguredFilePath() throws Exception {
        Path cache = Files.createFile(tempDir.resolve("krb5cc"));

        Path resolved = KerberosTicketCache.resolve(Optional.of(cache.toString()), Optional.empty(), Path.of("/tmp/unused"));

        assertEquals(cache.toAbsolutePath(), resolved);
    }

    @Test
    void resolveFilePrefix() throws Exception {
        Path cache = Files.createFile(tempDir.resolve("krb5cc-file"));

        Path resolved = KerberosTicketCache.resolve(Optional.empty(), Optional.of("FILE:" + cache), Path.of("/tmp/unused"));

        assertEquals(cache.toAbsolutePath(), resolved);
    }

    @Test
    void rejectApiCache() {
        IllegalStateException exception = assertThrows(IllegalStateException.class,
                () -> KerberosTicketCache.resolve(Optional.empty(), Optional.of("API:abc-123"), Path.of("/tmp/krb5cc_1000")));

        assertTrue(exception.getMessage().contains("API"));
        assertTrue(exception.getMessage().contains("FILE:"));
    }

    @Test
    void rejectMissingFile() {
        Path missing = tempDir.resolve("missing-ccache");

        IllegalStateException exception = assertThrows(IllegalStateException.class,
                () -> KerberosTicketCache.resolve(Optional.of(missing.toString()), Optional.empty(), missing));

        assertTrue(exception.getMessage().contains(missing.toString()));
    }
}
