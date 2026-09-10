package com.garethahealy.githuborgmanagement.clients;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.garethahealy.githuborgmanagement.clients.rest.QuayUsersRestClient;
import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.logging.Logger;
import org.jboss.resteasy.reactive.ClientWebApplicationException;
import org.jboss.resteasy.reactive.RestResponse;

@ApplicationScoped
public class QuayUserService {

    private final Logger logger;
    private final ObjectMapper mapper;

    @RestClient
    QuayUsersRestClient quayUsersRestClient;

    public QuayUserService(Logger logger, ObjectMapper mapper) {
        this.logger = logger;
        this.mapper = mapper;
    }

    public String getUser(String user) {
        String answer = null;

        try {
            RestResponse<String> response = quayUsersRestClient.getUser(user);

            JsonNode node = mapper.readTree(response.getEntity());
            answer = node.get("username").asText();
        } catch (ClientWebApplicationException | JsonProcessingException ex) {
            logger.error(ex);
        }

        return answer;
    }
}
