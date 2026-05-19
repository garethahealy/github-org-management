package com.garethahealy.githubstats.services.github;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;
import org.kohsuke.github.*;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
class GitHubOrganizationLookupServiceTest {

    @Inject
    GitHubOrganizationLookupService gitHubOrganizationLookupService;

    @Test
    void getOrganization() throws IOException {
        GHOrganization org = gitHubOrganizationLookupService.getOrganization("redhat-cop");

        assertNotNull(org);
    }

    @Test
    void getRepositories() throws IOException {
        GHOrganization org = gitHubOrganizationLookupService.getOrganization("redhat-cop");
        Map<String, GHRepository> repos = gitHubOrganizationLookupService.getRepositories(org);

        assertNotNull(org);
        assertNotNull(repos);
        assertFalse(repos.isEmpty());
        assertTrue(repos.containsKey("org"));
    }

    @Test
    void getRepositoryViaOrg() throws IOException {
        GHOrganization org = gitHubOrganizationLookupService.getOrganization("redhat-cop");
        GHRepository repo = gitHubOrganizationLookupService.getRepository(org, "org");

        assertNotNull(org);
        assertNotNull(repo);
        assertEquals("org", repo.getName());
    }

    @Test
    void getMembers() throws IOException {
        GHOrganization org = gitHubOrganizationLookupService.getOrganization("redhat-cop");
        List<GHUser> members = gitHubOrganizationLookupService.getMembers(org);

        assertNotNull(org);
        assertNotNull(members);
        assertFalse(members.isEmpty());

        Optional<GHUser> me = members.stream().filter(m -> m.getLogin().equalsIgnoreCase("garethahealy")).findFirst();
        assertTrue(me.isPresent());
        assertNotNull(me.get());
    }

    @Test
    void listTeams() throws IOException {
        GHOrganization org = gitHubOrganizationLookupService.getOrganization("redhat-cop");
        PagedIterable<GHTeam> teams = gitHubOrganizationLookupService.listTeams(org);

        assertNotNull(org);
        assertNotNull(teams);
        assertFalse(teams.toList().isEmpty());

        Optional<GHTeam> sre = teams.toList().stream().filter(t -> t.getName().equalsIgnoreCase("redhat-cop-sre")).findFirst();
        assertTrue(sre.isPresent());
        assertNotNull(sre.get());
    }
}
