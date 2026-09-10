package com.garethahealy.githuborgmanagement.services.github;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;
import org.kohsuke.github.GHContent;
import org.kohsuke.github.GHOrganization;
import org.kohsuke.github.GHRepository;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@QuarkusTest
class GitHubRepositoryLookupServiceTest {

    @Inject
    GitHubRepositoryLookupService gitHubRepositoryLookupService;

    @Inject
    GitHubOrganizationLookupService gitHubOrganizationLookupService;

    @Test
    void getConfigYaml() throws IOException {
        GHOrganization org = gitHubOrganizationLookupService.getOrganization("redhat-cop");
        GHRepository repo = gitHubOrganizationLookupService.getRepository(org, "org");

        GHContent content = gitHubRepositoryLookupService.getConfigYaml(repo, "main");

        assertNotNull(content);
        assertTrue(content.isFile());
    }

    @Test
    void getAnsibleInventoryGroupVarsAllYml() throws IOException {
        GHOrganization org = gitHubOrganizationLookupService.getOrganization("redhat-cop");
        GHRepository repo = gitHubOrganizationLookupService.getRepository(org, "org");

        GHContent content = gitHubRepositoryLookupService.getAnsibleInventoryGroupVarsAllYml(repo);

        assertNotNull(content);
        assertTrue(content.isFile());
    }
}
