package com.garethahealy.githuborgmanagement.factories;

import com.garethahealy.githuborgmanagement.testutils.BaseRequiresLdapConnection;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.apache.directory.api.ldap.model.exception.LdapException;
import org.apache.directory.ldap.client.api.LdapConnection;
import org.apache.directory.ldap.client.api.search.FilterBuilder;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;

import java.io.IOException;

import static org.wildfly.common.Assert.assertNotNull;
import static org.wildfly.common.Assert.assertTrue;

@QuarkusTest
class LdapConnectionFactoryIT extends BaseRequiresLdapConnection {

    @Inject
    LdapConnectionFactory factory;

    @Test
    @EnabledIf("canConnectVpn")
    void canConnect() {
        assertTrue(factory.canConnect());
    }

    @Test
    @EnabledIf("canConnectVpn")
    void open() throws LdapException {
        try (LdapConnectionLease lease = factory.open()) {
            LdapConnection connection = lease.connection();
            assertNotNull(connection);
        }
    }

    @Test
    @EnabledIf("canConnectVpn")
    void searchDn() throws IOException, LdapException {
        try (LdapConnectionLease lease = factory.open()) {
            LdapConnection connection = lease.connection();
            assertNotNull(factory.searchDn(connection, FilterBuilder.equal("uid", "gahealy")));
        }
    }

    @Test
    @EnabledIf("canConnectVpn")
    void search() throws IOException, LdapException {
        try (LdapConnectionLease lease = factory.open()) {
            LdapConnection connection = lease.connection();
            assertNotNull(factory.search(connection, FilterBuilder.equal("uid", "gahealy"), "dn"));
        }
    }
}
