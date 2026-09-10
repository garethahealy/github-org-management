package com.garethahealy.githuborgmanagement.services.users;

import com.garethahealy.githuborgmanagement.model.users.OrgMember;
import com.garethahealy.githuborgmanagement.testutils.OrgMemberMockData;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import org.junit.jupiter.api.Test;
import org.kohsuke.github.GitHub;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

@QuarkusTest
class OrgMemberValidationServiceTest {

    @Inject
    OrgMemberValidationService orgMemberValidationService;

    @Inject
    @Named("read")
    GitHub client;

    @Test
    void validate() throws IOException {
        orgMemberValidationService.validate(OrgMemberMockData.getMe(client));

        assertTrue(true);
    }

    @Test
    void validateMissingGitHubUser() throws IOException {
        OrgMember invalid = new OrgMember("gahealy@redhat.com", "garethahealy", new ArrayList<>(List.of("missing-user1")), List.of("garethahealy"), OrgMember.Source.Manual, null, null);

        orgMemberValidationService.validate(invalid);

        assertTrue(invalid.linkedGitHubUsernames().isEmpty());
    }

    @Test
    void validateGitHubOrg() throws IOException {
        OrgMember invalid = new OrgMember("gahealy@redhat.com", "garethahealy", new ArrayList<>(List.of("redhat-cop/org")), List.of("garethahealy"), OrgMember.Source.Manual, null, null);

        orgMemberValidationService.validate(invalid);

        assertTrue(invalid.linkedGitHubUsernames().isEmpty());
    }

    @Test
    void validateGitHubRepo() throws IOException {
        OrgMember invalid = new OrgMember("gahealy@redhat.com", "garethahealy", new ArrayList<>(List.of("garethahealy/org")), List.of("garethahealy"), OrgMember.Source.Manual, null, null);

        orgMemberValidationService.validate(invalid);

        assertTrue(invalid.linkedGitHubUsernames().isEmpty());
    }

    @Test
    void validateGitHubRepoExtraSlash() throws IOException {
        OrgMember invalid = new OrgMember("gahealy@redhat.com", "garethahealy", new ArrayList<>(List.of("garethahealy/org/")), List.of("garethahealy"), OrgMember.Source.Manual, null, null);

        orgMemberValidationService.validate(invalid);

        assertTrue(invalid.linkedGitHubUsernames().isEmpty());
    }

    @Test
    void validateMissingQuayUser() throws IOException {
        OrgMember invalid = new OrgMember("gahealy@redhat.com", "garethahealy", List.of("garethahealy"), new ArrayList<>(List.of("missing-user1")), OrgMember.Source.Manual, null, null);

        orgMemberValidationService.validate(invalid);

        assertTrue(invalid.linkedQuayUsernames().isEmpty());
    }
}
