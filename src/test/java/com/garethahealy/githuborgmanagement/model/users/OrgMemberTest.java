package com.garethahealy.githuborgmanagement.model.users;

import com.garethahealy.githuborgmanagement.services.ldap.LdapSearchService;
import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
class OrgMemberTest {

    @Test
    void handlesGithubDoubleHttps() {
        Map<String, List<String>> entries = new HashMap<>();
        entries.put(LdapSearchService.AttributeKeys.PrimaryMail, List.of("gahealy@redhat.com"));
        entries.put(LdapSearchService.AttributeKeys.SocialURLGitHub, List.of("https://https://github.com/garethahealy"));

        OrgMember member = OrgMember.from("garethahealy", entries);

        assertNotNull(member.gitHubUsername());
        assertEquals("garethahealy", member.gitHubUsername());
    }

    @Test
    void handlesGithubOrg() {
        Map<String, List<String>> entries = new HashMap<>();
        entries.put(LdapSearchService.AttributeKeys.PrimaryMail, List.of("gahealy@redhat.com"));
        entries.put(LdapSearchService.AttributeKeys.SocialURLGitHub, List.of("https://github.com/redhat-gpe/agnosticv"));

        OrgMember member = OrgMember.from("garethahealy", entries);

        assertNotNull(member.linkedGitHubUsernames());
        assertFalse(member.linkedGitHubUsernames().isEmpty());
        assertEquals("redhat-gpe/agnosticv", member.linkedGitHubUsernames().getFirst());
    }

    @Test
    void handlesGithubOrgEndingSlash() {
        Map<String, List<String>> entries = new HashMap<>();
        entries.put(LdapSearchService.AttributeKeys.PrimaryMail, List.of("gahealy@redhat.com"));
        entries.put(LdapSearchService.AttributeKeys.SocialURLGitHub, List.of("https://github.com/redhat-gpe/agnosticv/"));

        OrgMember member = OrgMember.from("garethahealy", entries);

        assertNotNull(member.linkedGitHubUsernames());
        assertFalse(member.linkedGitHubUsernames().isEmpty());
        assertEquals("redhat-gpe/agnosticv", member.linkedGitHubUsernames().getFirst());
    }

    @Test
    void handlesGithubOrgContainsQuestion() {
        Map<String, List<String>> entries = new HashMap<>();
        entries.put(LdapSearchService.AttributeKeys.PrimaryMail, List.of("gahealy@redhat.com"));
        entries.put(LdapSearchService.AttributeKeys.SocialURLGitHub, List.of("https://github.com/redcx1?tab=projects"));

        OrgMember member = OrgMember.from("garethahealy", entries);

        assertNotNull(member.linkedGitHubUsernames());
        assertFalse(member.linkedGitHubUsernames().isEmpty());
        assertEquals("redcx1", member.linkedGitHubUsernames().getFirst());
    }

    @Test
    void handlesQuayUserUrl() {
        Map<String, List<String>> entries = new HashMap<>();
        entries.put(LdapSearchService.AttributeKeys.PrimaryMail, List.of("gahealy@redhat.com"));
        entries.put(LdapSearchService.AttributeKeys.SocialURLQuay, List.of("https://quay.io/user/garethahealy"));

        OrgMember member = OrgMember.from("garethahealy", entries);

        assertNotNull(member.linkedQuayUsernames());
        assertFalse(member.linkedQuayUsernames().isEmpty());
        assertEquals("garethahealy", member.linkedQuayUsernames().getFirst());
    }

    @Test
    void handlesQuayRepository() {
        Map<String, List<String>> entries = new HashMap<>();
        entries.put(LdapSearchService.AttributeKeys.PrimaryMail, List.of("gahealy@redhat.com"));
        entries.put(LdapSearchService.AttributeKeys.SocialURLQuay, List.of("https://quay.io/repository/garethahealy"));

        OrgMember member = OrgMember.from("garethahealy", entries);

        assertNotNull(member.linkedQuayUsernames());
        assertFalse(member.linkedQuayUsernames().isEmpty());
        assertEquals("garethahealy", member.linkedQuayUsernames().getFirst());
    }

    @Test
    void handlesQuayOrganization() {
        Map<String, List<String>> entries = new HashMap<>();
        entries.put(LdapSearchService.AttributeKeys.PrimaryMail, List.of("gahealy@redhat.com"));
        entries.put(LdapSearchService.AttributeKeys.SocialURLQuay, List.of("https://quay.io/organization/garethahealy"));

        OrgMember member = OrgMember.from("garethahealy", entries);

        assertNotNull(member.linkedQuayUsernames());
        assertFalse(member.linkedQuayUsernames().isEmpty());
        assertEquals("garethahealy", member.linkedQuayUsernames().getFirst());
    }

    @Test
    void handlesQuay() {
        Map<String, List<String>> entries = new HashMap<>();
        entries.put(LdapSearchService.AttributeKeys.PrimaryMail, List.of("gahealy@redhat.com"));
        entries.put(LdapSearchService.AttributeKeys.SocialURLQuay, List.of("https://quay.io/garethahealy"));

        OrgMember member = OrgMember.from("garethahealy", entries);

        assertNotNull(member.linkedQuayUsernames());
        assertFalse(member.linkedQuayUsernames().isEmpty());
        assertEquals("garethahealy", member.linkedQuayUsernames().getFirst());
    }

    @Test
    void equalsSame() {
        Map<String, List<String>> entries = new HashMap<>();
        entries.put(LdapSearchService.AttributeKeys.PrimaryMail, List.of("gahealy@redhat.com"));
        entries.put(LdapSearchService.AttributeKeys.SocialURLQuay, List.of("https://quay.io/garethahealy"));

        OrgMember one = OrgMember.from("garethahealy", entries);
        OrgMember two = OrgMember.from("garethahealy", entries);

        assertEquals(one, two);
    }

    @Test
    void equalsDifferent() {
        Map<String, List<String>> entriesOne = new HashMap<>();
        entriesOne.put(LdapSearchService.AttributeKeys.PrimaryMail, List.of("gahealy@redhat.com"));
        entriesOne.put(LdapSearchService.AttributeKeys.SocialURLQuay, List.of("https://quay.io/garethahealy"));

        Map<String, List<String>> entriesTwo = new HashMap<>();
        entriesTwo.put(LdapSearchService.AttributeKeys.PrimaryMail, List.of("gahealy@redhat.com"));
        entriesTwo.put(LdapSearchService.AttributeKeys.SocialURLGitHub, List.of("https://github.com/garethahealy"));
        entriesTwo.put(LdapSearchService.AttributeKeys.SocialURLQuay, List.of("https://quay.io/garethahealy"));

        OrgMember one = OrgMember.from("garethahealy", entriesOne);
        OrgMember two = OrgMember.from("garethahealy", entriesTwo);

        assertNotEquals(one, two);
    }
}
