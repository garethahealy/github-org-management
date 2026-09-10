package com.garethahealy.githuborgmanagement.factories;

import javax.security.auth.login.AppConfigurationEntry;
import javax.security.auth.login.Configuration;
import java.util.HashMap;
import java.util.Map;

/**
 * JAAS configuration that authenticates with an existing FILE Kerberos ticket cache.
 */
public final class TicketCacheLoginConfiguration extends Configuration {

    private final AppConfigurationEntry[] entries;

    public TicketCacheLoginConfiguration(String ticketCache) {
        Map<String, String> options = new HashMap<>();
        options.put("useTicketCache", "true");
        options.put("doNotPrompt", "true");
        options.put("renewTGT", "true");
        options.put("refreshKrb5Config", "true");
        options.put("ticketCache", ticketCache);

        this.entries = new AppConfigurationEntry[]{
                new AppConfigurationEntry(
                        "com.sun.security.auth.module.Krb5LoginModule",
                        AppConfigurationEntry.LoginModuleControlFlag.REQUIRED,
                        options)
        };
    }

    @Override
    public AppConfigurationEntry[] getAppConfigurationEntry(String name) {
        return entries;
    }
}
