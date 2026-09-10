package com.garethahealy.githuborgmanagement.clients.rest;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.resteasy.reactive.RestResponse;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
class GitHubDiffRestClientTest {

    @RestClient
    GitHubDiffRestClient gitHubDiffRestClient;

    @Test
    void getDiff() {
        RestResponse<String> resp = gitHubDiffRestClient.getDiff("redhat-cop", "org", 1026);

        assertNotNull(resp);
        assertEquals(Response.Status.OK, resp.getStatusInfo().toEnum());
        assertTrue(resp.getEntity().contains("1atest"));
    }

    @Test
    void handlesNoDiff() {
        assertThrows(WebApplicationException.class, () -> {
            gitHubDiffRestClient.getDiff("redhat-cop", "org", Integer.MAX_VALUE);
        });
    }
}
