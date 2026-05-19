package com.garethahealy.githubstats.model.users;

import com.garethahealy.githubstats.services.github.GitHubOrganizationLookupService;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;
import org.kohsuke.github.GHOrganization;
import org.kohsuke.github.GHUser;

import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
class BasicGHUserTest {

    @Inject
    GitHubOrganizationLookupService gitHubOrganizationLookupService;

    @Test
    void from() throws IOException {
        GHOrganization org = gitHubOrganizationLookupService.getOrganization("redhat-cop");
        List<GHUser> users = gitHubOrganizationLookupService.getMembers(org);
        GHUser me = users.stream().filter(user -> user.getLogin().equalsIgnoreCase("garethahealy")).findFirst().get();

        BasicGHUser answer = BasicGHUser.from(me);
        assertNotNull(answer);
        assertNotNull(answer.login());
        assertEquals("garethahealy", answer.login());
        assertNull(answer.email());
        assertNotNull(answer.name());
        assertNotNull(answer.company());
    }
}
