package com.garethahealy.githubstats.config;

import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithName;

@ConfigMapping(prefix = "redhat.ldap")
public interface LdapConfigProperties {

    String connection();

    int port();

    String dn();

    @WithName("warmup-user")
    String warmupUser();
}
