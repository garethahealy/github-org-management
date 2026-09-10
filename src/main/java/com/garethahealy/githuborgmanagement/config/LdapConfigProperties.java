package com.garethahealy.githuborgmanagement.config;

import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;
import io.smallrye.config.WithName;

import java.util.Optional;

@ConfigMapping(prefix = "redhat.ldap")
public interface LdapConfigProperties {

    String connection();

    int port();

    String dn();

    @WithName("warmup-user")
    String warmupUser();

    Kerberos kerberos();

    interface Kerberos {

        @WithDefault("IPA.REDHAT.COM")
        String realm();

        @WithName("ticket-cache")
        Optional<String> ticketCache();

        String kdc();
    }
}
