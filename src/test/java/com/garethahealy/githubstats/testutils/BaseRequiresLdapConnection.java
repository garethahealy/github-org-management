package com.garethahealy.githubstats.testutils;

import com.garethahealy.githubstats.config.LdapConfigProperties;
import com.garethahealy.githubstats.factories.GssapiLdapConnectionFactory;
import org.apache.directory.api.ldap.model.cursor.EntryCursor;
import org.apache.directory.api.ldap.model.entry.Entry;
import org.apache.directory.api.ldap.model.message.SearchScope;
import org.apache.directory.api.ldap.model.name.Dn;
import org.apache.directory.ldap.client.api.LdapConnection;
import org.apache.directory.ldap.client.api.LdapConnectionConfig;
import org.apache.directory.ldap.client.api.search.FilterBuilder;
import org.jboss.logging.Logger;

import java.util.Optional;

public abstract class BaseRequiresLdapConnection {

    private static final Logger LOGGER = Logger.getLogger(BaseRequiresLdapConnection.class);
    private static final String LDAP_HOST = "admin1.idm-001.prod.iad2.dc.redhat.com";
    private static final String LDAP_DN = "cn=users,cn=accounts,dc=ipa,dc=redhat,dc=com";
    private static final String LDAP_WARMUP_USER = "gahealy";

    protected boolean canConnectVpn() {
        try {
            LdapConnectionConfig config = new LdapConnectionConfig();
            config.setLdapHost(LDAP_HOST);
            config.setLdapPort(389);

            GssapiLdapConnectionFactory factory = new GssapiLdapConnectionFactory(config, LOGGER, new TestKerberos());
            try (LdapConnection connection = factory.newLdapConnection()) {
                String filter = FilterBuilder.equal("uid", LDAP_WARMUP_USER).toString();
                try (EntryCursor cursor = connection.search(new Dn(LDAP_DN), filter, SearchScope.SUBTREE, "dn")) {
                    for (Entry entry : cursor) {
                        return true;
                    }
                }
            }
        } catch (Exception ex) {
            return false;
        }

        return false;
    }

    private static final class TestKerberos implements LdapConfigProperties.Kerberos {

        @Override
        public String realm() {
            return "IPA.REDHAT.COM";
        }

        @Override
        public Optional<String> ticketCache() {
            return Optional.empty();
        }

        @Override
        public String kdc() {
            return LDAP_HOST;
        }
    }
}
