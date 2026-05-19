package com.garethahealy.githubstats.services.github;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Named;
import org.jboss.logging.Logger;
import org.kohsuke.github.*;

import java.io.IOException;
import java.util.List;
import java.util.Map;

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
            logger.error(ex);
        }

        return answer;
    }
}
