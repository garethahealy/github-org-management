package com.garethahealy.githubstats.services.ldap;

import com.garethahealy.githubstats.factories.LdapConnectionLease;
import com.garethahealy.githubstats.model.users.OrgMember;
import com.garethahealy.githubstats.testutils.BaseRequiresLdapConnection;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.apache.directory.api.ldap.model.exception.LdapException;
import org.apache.directory.ldap.client.api.LdapConnection;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
class LdapSearchServiceIT extends BaseRequiresLdapConnection {

    @Inject
    LdapSearchService ldapSearchService;

    @Test
    @EnabledIf("canConnectVpn")
    void canConnect() {
        assertTrue(ldapSearchService.canConnect());
    }

    @Test
    @EnabledIf("canConnectVpn")
    void searchOnUser() throws IOException, LdapException {
        try (LdapConnectionLease lease = ldapSearchService.open()) {
            LdapConnection connection = lease.connection();
            assertTrue(ldapSearchService.searchOnUser(connection, "gahealy"));
        }
    }

    @Test
    @EnabledIf("canConnectVpn")
    void searchOnName() throws IOException, LdapException {
        try (LdapConnectionLease lease = ldapSearchService.open()) {
            LdapConnection connection = lease.connection();
            String answer = ldapSearchService.searchOnName(connection, "Gareth Healy");

            assertNotNull(answer);
            assertEquals("gahealy@redhat.com", answer);
        }
    }

    @Test
    @EnabledIf("canConnectVpn")
    void searchOnGitHubLogin() throws IOException, LdapException {
        try (LdapConnectionLease lease = ldapSearchService.open()) {
            LdapConnection connection = lease.connection();
            String answer = ldapSearchService.searchOnGitHubLogin(connection, "gahealy");

            assertNotNull(answer);
            assertEquals("gahealy@redhat.com", answer);
        }
    }

    @Test
    @EnabledIf("canConnectVpn")
    void searchOnQuaySocial() throws IOException, LdapException {
        try (LdapConnectionLease lease = ldapSearchService.open()) {
            LdapConnection connection = lease.connection();
            String answer = ldapSearchService.searchOnQuaySocial(connection, "garethahealy");

            assertNotNull(answer);
            assertEquals("gahealy@redhat.com", answer);
        }
    }

    @Test
    @EnabledIf("canConnectVpn")
    void searchOnPrimaryMail() throws IOException, LdapException {
        try (LdapConnectionLease lease = ldapSearchService.open()) {
            LdapConnection connection = lease.connection();
            String answer = ldapSearchService.searchOnPrimaryMail(connection, "gahealy@redhat.com");

            assertNotNull(answer);
            assertEquals("gahealy@redhat.com", answer);
        }
    }

    @Test
    @EnabledIf("canConnectVpn")
    void retrieve() throws IOException, LdapException {
        try (LdapConnectionLease lease = ldapSearchService.open()) {
            LdapConnection connection = lease.connection();
            OrgMember answer = ldapSearchService.retrieve(connection, "garethahealy", "gahealy@redhat.com");

            assertNotNull(answer);

            assertEquals("gahealy@redhat.com", answer.redhatEmailAddress());
            assertEquals("garethahealy", answer.gitHubUsername());
            assertEquals("garethahealy", answer.linkedGitHubUsernames().getFirst());
            assertEquals("garethahealy", answer.linkedQuayUsernames().getFirst());
        }
    }
}
