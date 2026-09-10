package com.garethahealy.githuborgmanagement.clients;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@QuarkusTest
class GitHubDiffServiceTest {

    @Inject
    GitHubDiffService gitHubDiffService;

    @Test
    void getDiff() {
        String resp = gitHubDiffService.getDiff("redhat-cop", "org", 1026);

        assertNotNull(resp);
        assertTrue(resp.contains("1atest"));
    }
}
