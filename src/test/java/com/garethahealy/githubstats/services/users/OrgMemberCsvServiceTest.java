package com.garethahealy.githubstats.services.users;

import com.garethahealy.githubstats.model.users.OrgMemberRepository;
import com.garethahealy.githubstats.testutils.OrgMemberMockData;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;

import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
class OrgMemberCsvServiceTest {

    @Inject
    OrgMemberCsvService orgMemberCsvService;

    @Test
    void parseSample() throws IOException {
        File input = new File("src/test/resources/sample-ldap-members.csv");

        OrgMemberRepository answer = orgMemberCsvService.parse(input);

        assertNotNull(answer);
        assertEquals(3, answer.size());
    }

    @Test
    void writeSample() throws IOException {
        File output = new File("target/OrgMemberCsvServiceTest/sample-ldap-members.csv");

        orgMemberCsvService.write(new OrgMemberRepository(output, OrgMemberMockData.getOrgMembersMap()));
    }

    @Test
    void writeHandlesEmptyMembers() throws IOException {
        File output = new File("target/OrgMemberCsvServiceTest/empty-ldap-members.csv");

        orgMemberCsvService.write(new OrgMemberRepository(output, new HashMap<>()));
    }

    @Test
    void parseLocalLdapMembers() throws IOException {
        File input = new File("ldap-members.csv");

        OrgMemberRepository answer = orgMemberCsvService.parse(input);

        assertNotNull(answer);
        assertTrue(answer.size() > 260);
    }

    @Test
    void parseLocalSupplementary() throws IOException {
        File input = new File("supplementary.csv");

        OrgMemberRepository answer = orgMemberCsvService.parse(input);

        assertNotNull(answer);
        assertEquals(18, answer.size());
    }
}
