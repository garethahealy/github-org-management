package com.garethahealy.githuborgmanagement.clients.rest;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.resteasy.reactive.RestResponse;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
class QuayUsersRestClientTest {

    @Inject
    ObjectMapper mapper;

    @RestClient
    QuayUsersRestClient quayUsersRestClient;

    @Test
    void getUser() throws JsonProcessingException {
        RestResponse<String> resp = quayUsersRestClient.getUser("garethahealy");

        assertNotNull(resp);
        assertEquals(Response.Status.OK, resp.getStatusInfo().toEnum());

        JsonNode node = mapper.readTree(resp.getEntity());
        assertNotNull(node);
        assertEquals("garethahealy", node.get("username").asText());
        assertEquals("user", node.get("avatar").get("kind").asText());
    }

    @Test
    void handlesUserNotExists() {
        assertThrows(WebApplicationException.class, () -> {
            quayUsersRestClient.getUser("does-not-exist");
        });
    }
}
