package com.garethahealy.githuborgmanagement.commands.users;

import io.quarkus.test.junit.main.Launch;
import io.quarkus.test.junit.main.LaunchResult;
import io.quarkus.test.junit.main.QuarkusMainIntegrationTest;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@QuarkusMainIntegrationTest
class ListenToPullRequestsCommandIT {

    @Test
    @Launch(value = {"users", "listen-to-pullrequests", "--dry-run=true", "--organization=redhat-cop", "--issue-repo=org", "--processors=MembersChangeInConfigYaml,MembersChangeInAnsibleVarsYaml", "--ldap-members-csv=ldap-members.csv", "--supplementary-csv=supplementary.csv", "--fail-if-no-vpn=true"})
    void run(LaunchResult result) {
        result.echoSystemOut();

        assertNotNull(result.getErrorOutput());
        assertEquals(0, result.exitCode());
    }
}
