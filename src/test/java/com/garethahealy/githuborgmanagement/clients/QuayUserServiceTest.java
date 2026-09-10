package com.garethahealy.githuborgmanagement.clients;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@QuarkusTest
class QuayUserServiceTest {

    @Inject
    QuayUserService quayUserService;

    @Test
    void getUser() {
        String resp = quayUserService.getUser("garethahealy");

        assertNotNull(resp);
        assertEquals("garethahealy", resp);
    }
}
