package com.garethahealy.githubstats.services.users;

import com.garethahealy.githubstats.clients.QuayUserService;
import com.garethahealy.githubstats.concurrent.BoundedVirtualThreadExecutor;
import com.garethahealy.githubstats.model.users.OrgMember;
import com.garethahealy.githubstats.model.users.OrgMemberRepository;
import com.garethahealy.githubstats.services.github.GitHubOrganizationLookupService;
import com.garethahealy.githubstats.services.github.GitHubUserLookupService;
import jakarta.enterprise.context.ApplicationScoped;
import org.jboss.logging.Logger;
import org.kohsuke.github.GHRepository;
import org.kohsuke.github.GHUser;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

@ApplicationScoped
public class OrgMemberValidationService {

    private final Logger logger;
    private final GitHubOrganizationLookupService gitHubOrganizationLookupService;
    private final GitHubUserLookupService gitHubUserLookupService;
    private final QuayUserService quayUserService;

    public OrgMemberValidationService(Logger logger, GitHubOrganizationLookupService gitHubOrganizationLookupService, GitHubUserLookupService gitHubUserLookupService, QuayUserService quayUserService) {
        this.logger = logger;
        this.gitHubOrganizationLookupService = gitHubOrganizationLookupService;
        this.gitHubUserLookupService = gitHubUserLookupService;
        this.quayUserService = quayUserService;
    }

    public void validate(OrgMemberRepository members) throws IOException, ExecutionException, InterruptedException {
        // Use bounded because of GitHub rate limit: https://docs.github.com/en/rest/using-the-rest-api/rate-limits-for-the-rest-api#about-secondary-rate-limits
        try (ExecutorService executor = new BoundedVirtualThreadExecutor(Runtime.getRuntime().availableProcessors())) {
            List<Future<OrgMember>> futures = new ArrayList<>();
            for (OrgMember current : members.items()) {
                futures.add(executor.submit(() -> {
                    try {
                        return validate(current);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }));
            }

            for (Future<OrgMember> future : futures) {
                future.get();
            }
        }
    }

    /**
     * Validate the GitHub and Quay usernames are valid against github.com and quay.io.
     */
    public OrgMember validate(OrgMember member) throws IOException {
        if (member.gitHubUsername() == null || member.gitHubUsername().isEmpty()) {
            throw new IllegalStateException("GitHubUsername is null or empty. Should never happen!");
        }

        if (!member.linkedGitHubUsernames().isEmpty()) {
            validateLinkedGitHubUsernames(member);
        }

        if (!member.linkedQuayUsernames().isEmpty()) {
            validateLinkedQuayUsernames(member);
        }

        return member;
    }

    private void validateLinkedGitHubUsernames(OrgMember member) throws IOException {
        List<String> remove = new ArrayList<>();

        for (String githubUsername : member.linkedGitHubUsernames()) {
            String userValue = githubUsername;
            if (githubUsername.contains("/")) {
                userValue = githubUsername.split("/")[0];
            }

            GHUser user = gitHubUserLookupService.getUser(userValue);
            if (user == null) {
                logger.warnf("%s was not found via the GitHub API, removing", githubUsername);

                remove.add(githubUsername);
            } else if (!user.getType().equalsIgnoreCase("User")) {
                logger.warnf("%s is not a `User`, its a %s, removing", githubUsername, user.getType());

                remove.add(githubUsername);
            } else {
                if (githubUsername.contains("/")) {
                    GHRepository repo = gitHubOrganizationLookupService.getRepository(githubUsername);
                    if (repo == null) {
                        throw new IllegalStateException("Expected GitHub Username but got '" + githubUsername + "' - Not sure what it is...");
                    } else {
                        logger.warnf("%s is a repository, removing", githubUsername);

                        remove.add(githubUsername);
                    }
                }
            }
        }

        if (!remove.isEmpty()) {
            logger.infof("-> Removing %s from linkedGitHubUsernames for %s", remove.size(), member.gitHubUsername());

            member.linkedGitHubUsernames().removeAll(remove);
        }
    }

    private void validateLinkedQuayUsernames(OrgMember member) {
        List<String> remove = new ArrayList<>();

        for (String quayUsername : member.linkedQuayUsernames()) {
            String response = quayUserService.getUser(quayUsername);
            if (!quayUsername.equalsIgnoreCase(response)) {
                logger.warnf("%s was not found via the Quay API, response was: %s, removing", quayUsername, response);

                remove.add(quayUsername);
            }
        }

        if (!remove.isEmpty()) {
            logger.infof("-> Removing %s from linkedQuayUsernames for %s", remove.size(), member.gitHubUsername());

            member.linkedQuayUsernames().removeAll(remove);
        }
    }
}
