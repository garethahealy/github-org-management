package com.garethahealy.githubstats.factories;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.apache.directory.api.ldap.model.exception.LdapException;
import org.apache.directory.ldap.client.api.LdapConnection;
import org.apache.directory.ldap.client.api.search.FilterBuilder;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.wildfly.common.Assert.assertNotNull;
import static org.wildfly.common.Assert.assertTrue;

@QuarkusTest
class LdapConnectionFactoryIT {

    @Inject
    LdapConnectionFactory factory;

    @Test
    void canConnect() {
        assertTrue(factory.canConnect());
    }

    @Test
    void open() throws LdapException {
        try (LdapConnectionLease lease = factory.open()) {
            LdapConnection connection = lease.connection();
            assertNotNull(connection);
        }
    }

    @Test
    void searchDn() throws IOException, LdapException {
        try (LdapConnectionLease lease = factory.open()) {
            LdapConnection connection = lease.connection();
            assertNotNull(factory.searchDn(connection, FilterBuilder.equal("uid", "gahealy")));
        }
    }

    @Test
    void search() throws IOException, LdapException {
        try (LdapConnectionLease lease = factory.open()) {
            LdapConnection connection = lease.connection();
            assertNotNull(factory.search(connection, FilterBuilder.equal("uid", "gahealy"), "dn"));
        }
    }
}
