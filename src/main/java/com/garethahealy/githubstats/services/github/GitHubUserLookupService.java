package com.garethahealy.githubstats.services.github;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Named;
import org.jboss.logging.Logger;
import org.kohsuke.github.GHUser;
import org.kohsuke.github.GitHub;

import java.io.IOException;

/**
 * Retrieves GitHub Organization data
 */
@ApplicationScoped
public class GitHubUserLookupService {

    private final Logger logger;
    private final GitHub client;

    public GitHubUserLookupService(Logger logger, @Named("read") GitHub client) {
        this.logger = logger;
        this.client = client;
    }

    public GHUser getUser(String user) {
        GHUser answer = null;
        try {
            answer = client.getUser(user);
        } catch (IOException ex) {
            logger.errorf(ex, "Failed for %s", user);
        }

        return answer;
    }
}
