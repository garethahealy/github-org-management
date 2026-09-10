package com.garethahealy.githuborgmanagement.commands.users;

import com.garethahealy.githuborgmanagement.commands.users.setup.CollectMembersFromRedHatLdapCommandSetup;
import com.garethahealy.githuborgmanagement.model.users.OrgMember;
import com.garethahealy.githuborgmanagement.predicates.OrgMemberFilters;
import com.garethahealy.githuborgmanagement.testutils.CsvParser;
import io.quarkus.test.junit.main.Launch;
import io.quarkus.test.junit.main.LaunchResult;
import io.quarkus.test.junit.main.QuarkusMainIntegrationTest;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@QuarkusMainIntegrationTest
class CollectMembersFromRedHatLdapCommandIT {

    @BeforeAll
    static void setup() throws IOException {
        CollectMembersFromRedHatLdapCommandSetup setup = new CollectMembersFromRedHatLdapCommandSetup();
        setup.setup();
    }

    /**
     * Collect with the current CSVs - tests the daily run
     */
    @Test
    @Launch(value = {"users", "collect-members-from-ldap", "--organization=redhat-cop", "--ldap-members-csv=target/CollectMembersFromRedHatLdap/ldap-members-run.csv", "--supplementary-csv=target/CollectMembersFromRedHatLdap/supplementary-run.csv", "--fail-if-no-vpn=true"})
    void run(LaunchResult result) throws IOException {
        result.echoSystemOut();

        assertNotNull(result.getErrorOutput());
        assertEquals(0, result.exitCode());

        // Validate outputs
        CsvParser csvParser = new CsvParser();
        Map<String, OrgMember> ldapOutput = csvParser.parse(CollectMembersFromRedHatLdapCommandSetup.run.LDAP);
        Map<String, OrgMember> supplementaryOutput = csvParser.parse(CollectMembersFromRedHatLdapCommandSetup.run.SUPPLEMENTARY);

        assertNotNull(ldapOutput);
        assertTrue(ldapOutput.size() > 260);
        assertTrue(ldapOutput.values().stream().filter(OrgMemberFilters.deleteAfterIsNotNull()).toList().isEmpty());

        assertNotNull(supplementaryOutput);
        assertEquals(18, supplementaryOutput.size());
        assertTrue(supplementaryOutput.values().stream().filter(OrgMemberFilters.deleteAfterIsNotNull()).toList().isEmpty());
    }

    /**
     * Collect only 5 members but using an empty LDAP input to start from and validate CSVs
     */
    @Test
    @Launch(value = {"users", "collect-members-from-ldap", "--organization=redhat-cop", "--ldap-members-csv=target/CollectMembersFromRedHatLdap/ldap-members-runWithLimit.csv", "--supplementary-csv=target/CollectMembersFromRedHatLdap/supplementary-runWithLimit.csv", "--user-limit=5", "--fail-if-no-vpn=true"})
    void runWithLimit(LaunchResult result) throws IOException {
        result.echoSystemOut();

        assertNotNull(result.getErrorOutput());
        assertEquals(0, result.exitCode());
        assertTrue(result.getOutput().contains("Adding 9strands to ldap-members-runWithLimit.csv CSV"));

        // Validate outputs
        CsvParser csvParser = new CsvParser();
        Map<String, OrgMember> ldapOutput = csvParser.parse(CollectMembersFromRedHatLdapCommandSetup.runWithLimit.LDAP);
        Map<String, OrgMember> supplementaryOutput = csvParser.parse(CollectMembersFromRedHatLdapCommandSetup.runWithLimit.SUPPLEMENTARY);

        assertNotNull(ldapOutput);
        assertEquals(5, ldapOutput.size());
        assertTrue(ldapOutput.values().stream().filter(OrgMemberFilters.deleteAfterIsNotNull()).toList().isEmpty());

        assertNotNull(supplementaryOutput);
        assertEquals(18, supplementaryOutput.size());
        assertTrue(supplementaryOutput.values().stream().filter(OrgMemberFilters.deleteAfterIsNotNull()).toList().isEmpty());
    }

    /**
     * Validate a member in the supplementary can be moved to the LDAP CSV
     */
    @Test
    @Launch(value = {"users", "collect-members-from-ldap", "--organization=redhat-cop", "--ldap-members-csv=target/CollectMembersFromRedHatLdap/ldap-members-runValidateMovingMembers.csv", "--supplementary-csv=target/CollectMembersFromRedHatLdap/supplementary-runValidateMovingMembers.csv", "--validate-csv=true", "--fail-if-no-vpn=true"})
    void runValidateMovingMembers(LaunchResult result) throws IOException {
        result.echoSystemOut();

        assertNotNull(result.getErrorOutput());
        assertEquals(0, result.exitCode());
        assertTrue(result.getOutput().contains("gahealy@redhat.com is in LDAP and Supplementary CSV, removing from Supplementary"));

        // Validate outputs
        CsvParser csvParser = new CsvParser();
        Map<String, OrgMember> ldapInput = csvParser.parse(new File("ldap-members.csv"));
        Map<String, OrgMember> ldapOutput = csvParser.parse(CollectMembersFromRedHatLdapCommandSetup.runValidateMovingMembers.LDAP);
        Map<String, OrgMember> supplementaryOutput = csvParser.parse(CollectMembersFromRedHatLdapCommandSetup.runValidateMovingMembers.SUPPLEMENTARY);

        assertNotNull(ldapOutput);
        assertEquals(ldapInput.size(), ldapOutput.size());
        assertTrue(ldapOutput.values().stream().filter(OrgMemberFilters.deleteAfterIsNotNull()).toList().isEmpty());

        assertNotNull(supplementaryOutput);
        assertEquals(18, supplementaryOutput.size());
        assertTrue(supplementaryOutput.values().stream().filter(OrgMemberFilters.deleteAfterIsNotNull()).toList().isEmpty());
    }
}
