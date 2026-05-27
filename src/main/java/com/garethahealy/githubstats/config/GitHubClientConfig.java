package com.garethahealy.githubstats.config;

import io.quarkus.runtime.Startup;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import org.jboss.logging.Logger;
import org.kohsuke.github.GHRateLimit;
import org.kohsuke.github.GitHub;
import org.kohsuke.github.GitHubBuilder;

import java.io.IOException;

@Startup
@Singleton
public class GitHubClientConfig {

    private final Logger logger;
    private final String githubLogin;
    private final String githubOauth;
    private final String githubWriteOauth;

    public GitHubClientConfig(Logger logger, GitHubConfigProperties config) {
        this.logger = logger;
        this.githubLogin = config.login();
        this.githubOauth = config.oauth();
        this.githubWriteOauth = config.writeOauth();
    }

    @Singleton
    @Produces
    @Named(value = "write")
    public GitHub getWriteClient() throws IOException {
        return getClientVia(new GitHubBuilder().withOAuthToken(githubWriteOauth, githubLogin), "write");
    }

    @Singleton
    @Produces
    @Named(value = "read")
    public GitHub getClient() throws IOException {
        return getClientVia(new GitHubBuilder().withOAuthToken(githubOauth, githubLogin), "read");
    }

    private GitHub getClientVia(GitHubBuilder builder, String id) throws IOException {
        GitHub gitHub = builder.build();
        if (gitHub.isAnonymous()) {
            throw new IllegalStateException(id + ": isAnonymous - have you set GITHUB_LOGIN / GITHUB_OAUTH ?");
        }

        if (!gitHub.isCredentialValid()) {
            throw new IllegalStateException(id + ": isCredentialValid - are GITHUB_LOGIN / GITHUB_OAUTH valid?");
        }

        GHRateLimit rateLimit = gitHub.getRateLimit();
        if (rateLimit.getRemaining() == 0) {
            throw new IllegalStateException(id + ": RateLimit - is zero, you need to wait until the reset date");
        }

        logger.infof("%s: limit %s, remaining %s, resetDate %s", id, rateLimit.getLimit(), rateLimit.getRemaining(), rateLimit.getResetDate());
        return gitHub;
    }
}
