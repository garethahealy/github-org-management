package com.garethahealy.githuborgmanagement.services.github;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;
import org.kohsuke.github.GHContent;
import org.kohsuke.github.GHOrganization;
import org.kohsuke.github.GHRepository;

import java.io.IOException;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
class GitHubFileRetrievalServiceTest {

    @Inject
    GitHubFileRetrievalService gitHubFileRetrievalService;

    @Inject
    GitHubOrganizationLookupService gitHubOrganizationLookupService;

    @Inject
    GitHubRepositoryLookupService gitHubRepositoryLookupService;

    @Test
    void getRepos() throws IOException {
        GHOrganization org = gitHubOrganizationLookupService.getOrganization("redhat-cop");
        GHRepository orgRepo = gitHubOrganizationLookupService.getRepository(org, "org");
        GHContent content = gitHubRepositoryLookupService.getConfigYaml(orgRepo, "main");
        Set<String> repos = gitHubFileRetrievalService.getRepos(content, true);

        assertNotNull(repos);
        assertFalse(repos.isEmpty());
        assertTrue(repos.contains("org"));
    }

    @Test
    void getArchivedRepos() throws IOException {
        GHOrganization org = gitHubOrganizationLookupService.getOrganization("redhat-cop");
        GHRepository orgRepo = gitHubOrganizationLookupService.getRepository(org, "org");
        GHContent content = gitHubRepositoryLookupService.getConfigYaml(orgRepo, "main");
        Set<String> archivedRepos = gitHubFileRetrievalService.getArchivedRepos(content, true);

        assertNotNull(archivedRepos);
        assertFalse(archivedRepos.isEmpty());
        assertTrue(archivedRepos.contains("pathfinder"));
    }

    @Test
    void getConfigMembers() throws IOException {
        GHOrganization org = gitHubOrganizationLookupService.getOrganization("redhat-cop");
        GHRepository orgRepo = gitHubOrganizationLookupService.getRepository(org, "org");
        GHContent content = gitHubRepositoryLookupService.getConfigYaml(orgRepo, "main");
        Set<String> members = gitHubFileRetrievalService.getConfigMembers(content);

        assertNotNull(members);
        assertFalse(members.isEmpty());
        assertTrue(members.contains("garethahealy"));
    }

    @Test
    void getAnsibleMembers() throws IOException {
        GHOrganization org = gitHubOrganizationLookupService.getOrganization("redhat-cop");
        GHRepository orgRepo = gitHubOrganizationLookupService.getRepository(org, "org");
        GHContent content = gitHubRepositoryLookupService.getAnsibleInventoryGroupVarsAllYml(orgRepo);
        Set<String> members = gitHubFileRetrievalService.getAnsibleMembers(content);

        assertNotNull(members);
        assertFalse(members.isEmpty());
        assertTrue(members.contains("ablock"));
    }
}
