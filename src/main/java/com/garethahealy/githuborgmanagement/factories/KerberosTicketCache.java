package com.garethahealy.githuborgmanagement.factories;

import com.sun.security.auth.module.UnixSystem;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Optional;

/**
 * Resolves a Kerberos credential cache that Java GSSAPI can actually read.
 * FILE caches work; macOS API, KCM, and KEYRING caches do not.
 */
public final class KerberosTicketCache {

    private KerberosTicketCache() {
    }

    public static Path resolve(Optional<String> configuredCache) {
        return resolve(configuredCache, Optional.ofNullable(System.getenv("KRB5CCNAME")), defaultFileCachePath());
    }

    static Path resolve(Optional<String> configuredCache, Optional<String> environmentCache, Path defaultFileCache) {
        String raw = configuredCache.filter(value -> !value.isBlank())
                .or(() -> environmentCache.filter(value -> !value.isBlank()))
                .orElseGet(defaultFileCache::toString);

        String type = cacheType(raw);
        if (!"FILE".equals(type)) {
            throw new IllegalStateException("Kerberos ticket cache is " + type + " (" + raw + "). Java GSSAPI cannot read API/KCM/KEYRING caches. Run: export KRB5CCNAME=FILE:" + defaultFileCache + " && kinit <user>@<REALM>");
        }

        Path path = filePath(raw);
        if (!Files.isRegularFile(path)) {
            throw new IllegalStateException("Kerberos ticket cache file not found: " + path + ". Run: export KRB5CCNAME=FILE:" + path + " && kinit <user>@<REALM>");
        }

        return path.toAbsolutePath();
    }

    static Path defaultFileCachePath() {
        try {
            return Path.of("/tmp/krb5cc_" + new UnixSystem().getUid());
        } catch (Exception e) {
            return Path.of(System.getProperty("user.home"), "krb5cc_" + System.getProperty("user.name"));
        }
    }

    private static String cacheType(String raw) {
        int separator = raw.indexOf(':');
        if (separator <= 0) {
            return "FILE";
        }

        String prefix = raw.substring(0, separator).toUpperCase(Locale.ROOT);
        return switch (prefix) {
            case "FILE", "API", "KCM", "KEYRING", "DIR" -> prefix;
            default -> "FILE";
        };
    }

    private static Path filePath(String raw) {
        if (raw.toUpperCase(Locale.ROOT).startsWith("FILE:")) {
            return Path.of(raw.substring("FILE:".length()));
        }

        return Path.of(raw);
    }
}
