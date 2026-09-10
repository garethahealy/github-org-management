package com.garethahealy.githuborgmanagement.config;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import org.junit.jupiter.api.Test;
import org.kohsuke.github.GitHub;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertNotNull;

@QuarkusTest
class GitHubClientConfigTest {

    @Inject
    @Named(value = "read")
    GitHub reader;

    @Inject
    @Named(value = "write")
    GitHub writer;

    @Test
    void getWriteClient() throws IOException {
        assertNotNull(writer);
        assertNotNull(writer.getRateLimit());
    }

    @Test
    void getClient() throws IOException {
        assertNotNull(reader);
        assertNotNull(reader.getRateLimit());
    }
}
