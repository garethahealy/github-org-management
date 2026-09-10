package com.garethahealy.githubstats.factories;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class KerberosKrb5ConfGeneratorTest {

    @Test
    void writesExplicitKdcAndDisablesDnsLookup() throws Exception {
        var file = KerberosKrb5ConfGenerator.write("IPA.REDHAT.COM", "kdc1.example.com, kdc2.example.com:88");
        String content = Files.readString(file);

        assertTrue(content.contains("default_realm = IPA.REDHAT.COM"));
        assertTrue(content.contains("dns_lookup_kdc = false"));
        assertTrue(content.contains("dns_canonicalize_hostname = false"));
        assertTrue(content.contains("kdc = kdc1.example.com"));
        assertTrue(content.contains("kdc = kdc2.example.com:88"));
    }

    @Test
    void requiresConfiguredHosts() {
        assertThrows(IllegalStateException.class, () -> KerberosKrb5ConfGenerator.write("IPA.REDHAT.COM", "  "));
    }

    @Test
    void splitsConfiguredHosts() {
        assertEquals(List.of("kdc1.example.com", "kdc2.example.com:88"),
                KerberosKrb5ConfGenerator.hosts("kdc1.example.com, kdc2.example.com:88"));
    }
}
