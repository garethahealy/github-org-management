package com.garethahealy.githuborgmanagement.factories;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Writes a krb5.conf that names KDCs explicitly so native-image JGSS does not need JNDI DNS.
 */
public final class KerberosKrb5ConfGenerator {

    private KerberosKrb5ConfGenerator() {
    }

    public static Path write(String realm, String kdc) throws IOException {
        List<String> kdcs = hosts(kdc);
        if (kdcs.isEmpty()) {
            throw new IllegalStateException("Set redhat.ldap.kerberos.kdc to a host:port (comma-separated).");
        }

        String kdcLines = kdcs.stream()
                .map(host -> "    kdc = " + host)
                .collect(Collectors.joining("\n"));

        String content = """
                [libdefaults]
                default_realm = %s
                dns_canonicalize_hostname = false
                rdns = false
                dns_lookup_kdc = false
                dns_lookup_realm = false
                forwardable = true

                [realms]
                %s = {
                %s
                }
                """.formatted(realm, realm, kdcLines);

        Path file = Files.createTempFile("github-org-management-krb5-", ".conf");
        file.toFile().deleteOnExit();
        Files.writeString(file, content);
        return file;
    }

    static List<String> hosts(String kdc) {
        List<String> hosts = new ArrayList<>();
        if (kdc == null || kdc.isBlank()) {
            return List.of();
        }

        for (String part : kdc.split(",")) {
            String host = part.trim();
            if (!host.isEmpty()) {
                hosts.add(host);
            }
        }
        return List.copyOf(hosts);
    }
}
