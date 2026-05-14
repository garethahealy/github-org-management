package com.garethahealy.githubstats.services.github;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Named;
import org.jboss.logging.Logger;
import org.kohsuke.github.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Retrieves GitHub Organization data
 */
@ApplicationScoped
public class GitHubOrganizationLookupService {

    private final Logger logger;
    private final GitHub client;

    public GitHubOrganizationLookupService(Logger logger, @Named("read") GitHub client) {
        this.logger = logger;
        this.client = client;
    }

    @PostConstruct
    void init() {
        try {
            logRateLimit();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public GHUser getUser(String user) {
        GHUser answer = null;
        try {
            answer = client.getUser(user);
        } catch (IOException ex) {
            logger.error(ex);
        }

        return answer;
    }

    public GHOrganization getOrganization(String organization) throws IOException {
        return client.getOrganization(organization);
    }

    public Map<String, GHRepository> getRepositories(GHOrganization org) throws IOException {
        return org.getRepositories();
    }

    public GHRepository getRepository(String ownerRepo) throws IOException {
        return client.getRepository(ownerRepo);
    }

    public GHRepository getRepository(String owner, String repo) throws IOException {
        return client.getRepository(owner + "/" + repo);
    }

    public GHRepository getRepository(GHOrganization org, String repo) throws IOException {
        return org.getRepository(repo);
    }

    public List<GHUser> listMembers(GHOrganization org) throws IOException {
        return org.listMembers().toList();
    }

    public PagedIterable<GHTeam> listTeams(GHOrganization org) throws IOException {
        return org.listTeams();
    }

    private void logRateLimit() throws IOException {
        GHRateLimit rateLimit = client.getRateLimit();
        logger.infof("RateLimit: limit %s, remaining %s, resetDate %s", rateLimit.getLimit(), rateLimit.getRemaining(), rateLimit.getResetDate());
    }

    public void hasRateLimit(Integer need) throws IOException {
        int remaining = client.getRateLimit().getRemaining();
        if ((remaining - need) <= 0) {
            logRateLimit();

            throw new IllegalStateException("RateLimit - we think you need " + need + " which is not enough to complete");
        }
    }
}
