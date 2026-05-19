package com.garethahealy.githubstats.services.users;

import com.garethahealy.githubstats.factories.LdapConnectionLease;
import com.garethahealy.githubstats.model.users.OrgMember;
import com.garethahealy.githubstats.services.ldap.LdapSearchService;
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
class OrgMemberValidationServiceIT extends BaseRequiresLdapConnection {

    @Inject
    LdapSearchService ldapSearchService;

    @Inject
    OrgMemberValidationService orgMemberValidationService;

    @Test
    @EnabledIf("canConnectVpn")
    void validateSsulliva() throws IOException, LdapException {
        try (LdapConnectionLease lease = ldapSearchService.open()) {
            LdapConnection connection = lease.connection();
            OrgMember answer = orgMemberValidationService.validate(ldapSearchService.retrieve(connection, "sean-m-sullivan", "ssulliva@redhat.com"));
            assertNotNull(answer);

            assertEquals("ssulliva@redhat.com", answer.redhatEmailAddress());
            assertEquals("sean-m-sullivan", answer.gitHubUsername());
            assertFalse(answer.linkedGitHubUsernames().isEmpty());
            assertTrue(answer.linkedQuayUsernames().isEmpty());
            assertEquals(1, answer.linkedGitHubUsernames().size());
            assertEquals("sean-m-sullivan", answer.linkedGitHubUsernames().getFirst());
        }
    }

    @Test
    @EnabledIf("canConnectVpn")
    void validateClaudiol() throws IOException, LdapException {
        try (LdapConnectionLease lease = ldapSearchService.open()) {
            LdapConnection connection = lease.connection();
            OrgMember answer = orgMemberValidationService.validate(ldapSearchService.retrieve(connection, "claudiol", "claudiol@redhat.com"));
            assertNotNull(answer);

            assertEquals("claudiol@redhat.com", answer.redhatEmailAddress());
            assertEquals("claudiol", answer.gitHubUsername());
            assertFalse(answer.linkedGitHubUsernames().isEmpty());
            assertTrue(answer.linkedQuayUsernames().isEmpty());
            assertEquals(1, answer.linkedGitHubUsernames().size());
            assertEquals("claudiol", answer.linkedGitHubUsernames().getFirst());
        }
    }

    @Test
    @EnabledIf("canConnectVpn")
    void validateAblock() throws IOException, LdapException {
        try (LdapConnectionLease lease = ldapSearchService.open()) {
            LdapConnection connection = lease.connection();
            OrgMember answer = orgMemberValidationService.validate(ldapSearchService.retrieve(connection, "sabre1041", "ablock@redhat.com"));
            assertNotNull(answer);

            assertEquals("ablock@redhat.com", answer.redhatEmailAddress());
            assertEquals("sabre1041", answer.gitHubUsername());
            assertFalse(answer.linkedGitHubUsernames().isEmpty());
            assertFalse(answer.linkedQuayUsernames().isEmpty());
            assertEquals(1, answer.linkedGitHubUsernames().size());
            assertEquals(1, answer.linkedQuayUsernames().size());
            assertEquals("sabre1041", answer.linkedGitHubUsernames().getFirst());
            assertEquals("ablock", answer.linkedQuayUsernames().getFirst());
        }
    }
}
