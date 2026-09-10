package com.garethahealy.githuborgmanagement.clients.rest;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;
import org.jboss.resteasy.reactive.RestResponse;

@RegisterRestClient
@Path("/api/v1/users")
public interface QuayUsersRestClient {

    @GET
    @Path("/{user}")
    RestResponse<String> getUser(@PathParam("user") String user);
}
