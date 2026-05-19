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

    private final GitHub client;

    public GitHubOrganizationWriterService(@Named("write") GitHub client) {
        this.client = client;
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
}
