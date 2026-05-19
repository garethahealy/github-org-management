package com.garethahealy.githubstats.services.github;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Named;
import org.jboss.logging.Logger;
import org.kohsuke.github.GHOrganization;
import org.kohsuke.github.GHRateLimit;
import org.kohsuke.github.GHRepository;
import org.kohsuke.github.GitHub;

import java.io.IOException;

@ApplicationScoped
public class GitHubOrganizationWriterService {

    private final Logger logger;
    private final GitHub client;

    public GitHubOrganizationWriterService(Logger logger, @Named("write") GitHub client) {
        this.client = client;
        this.logger = logger;
    }

    @PostConstruct
    void init() {
        try {
            logRateLimit();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public GHOrganization getOrganization(String organization) throws IOException {
        return client.getOrganization(organization);
    }

    public GHRepository getRepository(GHOrganization org, String repo) throws IOException {
        return client.getRepository(org.getLogin() + "/" + repo);
    }

    /**
     * todo: remove
     *
     * @param owner
     * @param repo
     * @return
     * @throws IOException
     */
    public GHRepository getRepository(String owner, String repo) throws IOException {
        return client.getRepository(owner + "/" + repo);
    }

    private void logRateLimit() throws IOException {
        GHRateLimit rateLimit = client.getRateLimit();
        logger.infof("RateLimit: limit %s, remaining %s, resetDate %s", rateLimit.getLimit(), rateLimit.getRemaining(), rateLimit.getResetDate());
    }
}
