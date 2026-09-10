package com.garethahealy.githuborgmanagement.factories;

import org.junit.jupiter.api.Test;

import javax.security.auth.login.AppConfigurationEntry;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TicketCacheLoginConfigurationTest {

    @Test
    void usesTicketCacheWithoutPrompting() {
        TicketCacheLoginConfiguration configuration = new TicketCacheLoginConfiguration("/tmp/krb5cc_1000");

        AppConfigurationEntry[] entries = configuration.getAppConfigurationEntry("ldapnetworkconnection");

        assertEquals(1, entries.length);
        assertEquals("com.sun.security.auth.module.Krb5LoginModule", entries[0].getLoginModuleName());
        Map<String, ?> options = entries[0].getOptions();
        assertEquals("true", options.get("useTicketCache"));
        assertEquals("true", options.get("doNotPrompt"));
        assertEquals("/tmp/krb5cc_1000", options.get("ticketCache"));
        assertEquals(AppConfigurationEntry.LoginModuleControlFlag.REQUIRED, entries[0].getControlFlag());
    }
}
