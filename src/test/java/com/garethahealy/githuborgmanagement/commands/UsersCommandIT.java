package com.garethahealy.githuborgmanagement.commands;

import io.quarkus.test.junit.main.Launch;
import io.quarkus.test.junit.main.LaunchResult;
import io.quarkus.test.junit.main.QuarkusMainIntegrationTest;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@QuarkusMainIntegrationTest
public class UsersCommandIT {

    @Test
    @Launch({"users", "help"})
    void test(LaunchResult result) {
        result.echoSystemOut();

        assertNotNull(result.getErrorOutput());
        assertEquals(0, result.exitCode());
    }
}
