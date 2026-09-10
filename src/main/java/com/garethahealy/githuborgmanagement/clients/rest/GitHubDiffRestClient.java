package com.garethahealy.githuborgmanagement.clients.rest;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;
import org.jboss.resteasy.reactive.RestResponse;

@RegisterRestClient
public interface GitHubDiffRestClient {

    @GET
    @Path("/{org}/{repo}/pull/{number}.diff")
    RestResponse<String> getDiff(@PathParam("org") String org, @PathParam("repo") String repo, @PathParam("number") int number);
}
