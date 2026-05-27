package com.garethahealy.githubstats.services.github;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Named;
import org.kohsuke.github.GHOrganization;
import org.kohsuke.github.GHRepository;
import org.kohsuke.github.GHTeam;
import org.kohsuke.github.GHUser;
import org.kohsuke.github.GitHub;
import org.kohsuke.github.PagedIterable;

import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * Retrieves GitHub Organization data
 */
@ApplicationScoped
public class GitHubOrganizationLookupService {

    private final GitHub client;

    public GitHubOrganizationLookupService(@Named("read") GitHub client) {
        this.client = client;
    }

    public GHOrganization getOrganization(String organization) throws IOException {
        return client.getOrganization(organization);
    }

    public Map<String, GHRepository> getRepositories(GHOrganization org) throws IOException {
        return org.getRepositories();
    }

    public GHRepository getRepository(GHOrganization org, String repo) throws IOException {
        return org.getRepository(repo);
    }

    /**
     * @param ownerRepo i.e.: garethahealy/org
     * @return
     * @throws IOException
     */
    public GHRepository getRepository(String ownerRepo) throws IOException {
        return client.getRepository(ownerRepo);
    }

    public List<GHUser> getMembers(GHOrganization org) throws IOException {
        return org.listMembers().toList();
    }

    /**
     * update as we sometimes only want a certain number
     * @param org
     * @return
     * @throws IOException
     */
    public PagedIterable<GHTeam> listTeams(GHOrganization org) throws IOException {
        return org.listTeams();
    }
}
