package com.garethahealy.githuborgmanagement.commands.users;

import com.garethahealy.githuborgmanagement.commands.users.setup.CreateWhoAreYouIssueCommandSetup;
import com.garethahealy.githuborgmanagement.model.users.OrgMember;
import com.garethahealy.githuborgmanagement.predicates.OrgMemberFilters;
import com.garethahealy.githuborgmanagement.testutils.CsvParser;
import io.quarkus.test.junit.main.Launch;
import io.quarkus.test.junit.main.LaunchResult;
import io.quarkus.test.junit.main.QuarkusMainIntegrationTest;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@QuarkusMainIntegrationTest
class CreateWhoAreYouIssueCommandIT {

    @BeforeAll
    static void setup() throws IOException {
        CreateWhoAreYouIssueCommandSetup setup = new CreateWhoAreYouIssueCommandSetup();
        setup.setup();
    }

    /**
     * Check for missing user and guess who they are
     */
    @Test
    @Launch(value = {"users", "create-who-are-you-issues", "--dry-run=true", "--organization=redhat-cop", "--issue-repo=org", "--ldap-members-csv=target/CreateWhoAreYouIssue/ldap-members-runReadWithoutMe.csv", "--supplementary-csv=target/CreateWhoAreYouIssue/supplementary-runReadWithoutMe.csv", "--fail-if-no-vpn=true", "--permission=read", "--guess=true"})
    void runReadWithoutMe(LaunchResult result) {
        result.echoSystemOut();

        assertNotNull(result.getErrorOutput());
        assertEquals(0, result.exitCode());
        assertTrue(result.getOutput().contains("Guessed garethahealy / gahealy@redhat.com via LDAP"));
    }

    /**
     * Check for missing user who is admin and guess who they are
     */
    @Test
    @Launch(value = {"users", "create-who-are-you-issues", "--dry-run=true", "--organization=redhat-cop", "--issue-repo=org", "--ldap-members-csv=ldap-members.csv", "--supplementary-csv=supplementary.csv", "--fail-if-no-vpn=true", "--permission=admin", "--guess=true"})
    void runAdmin(LaunchResult result) {
        result.echoSystemOut();

        assertNotNull(result.getErrorOutput());
        assertEquals(0, result.exitCode());
    }

    /**
     * Check for a user who is marked for deletion
     */
    @Test
    @Launch(value = {"users", "create-who-are-you-issues", "--dry-run=true", "--organization=redhat-cop", "--issue-repo=org", "--ldap-members-csv=target/CreateWhoAreYouIssue/ldap-members-runUserMarkedForDeletion.csv", "--supplementary-csv=target/CreateWhoAreYouIssue/supplementary-runUserMarkedForDeletion.csv", "--fail-if-no-vpn=true", "--permission=admin", "--guess=true"})
    void runUserMarkedForDeletion(LaunchResult result) throws IOException {
        result.echoSystemOut();

        assertNotNull(result.getErrorOutput());
        assertEquals(0, result.exitCode());
        assertTrue(result.getOutput().contains("The following users can no longer be found in LDAP"));

        // Validate outputs
        CsvParser csvParser = new CsvParser();
        Map<String, OrgMember> members = csvParser.parse(CreateWhoAreYouIssueCommandSetup.runUserMarkedForDeletion.LDAP);
        List<OrgMember> markedWithDelete = members.values().stream().filter(OrgMemberFilters.deleteAfterIsNotNull()).toList();

        assertNotNull(markedWithDelete);
        assertEquals(1, markedWithDelete.size());
        assertEquals("sabre1041", markedWithDelete.getFirst().gitHubUsername());
        assertFalse(members.containsKey("garethahealy"));
    }
}
