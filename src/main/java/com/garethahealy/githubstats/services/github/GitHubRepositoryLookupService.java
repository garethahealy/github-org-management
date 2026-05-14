package com.garethahealy.githubstats.services.github;

import jakarta.enterprise.context.ApplicationScoped;
import org.jboss.logging.Logger;
import org.kohsuke.github.*;

import java.io.IOException;
import java.util.List;

/**
 * Retrieves GitHub Repository data
 */
@ApplicationScoped
public class GitHubRepositoryLookupService {

    private final Logger logger;

    public GitHubRepositoryLookupService(Logger logger) {
        this.logger = logger;
    }

    public List<GHIssue> listOpenIssues(GHRepository repo) throws IOException {
        logger.debugf("-> listOpenIssues", repo.getName());
        List<GHIssue> issues = repo.getIssues(GHIssueState.OPEN);
        return issues.stream().filter(issue -> !issue.isPullRequest()).toList();
    }

    public List<GHPullRequest> listOpenPullRequests(GHRepository repo) throws IOException {
        logger.debugf("-> listOpenPullRequests", repo.getName());
        return repo.getPullRequests(GHIssueState.OPEN);
    }

    public GHContent getConfigYaml(GHRepository coreOrg, boolean validateOrgConfig) throws IOException {
        if (validateOrgConfig) {
            return getContent(coreOrg, "main", "config.yaml");
        } else {
            return null;
        }
    }

    public GHContent getConfigYaml(GHRepository coreOrg, String branch) throws IOException {
        return getContent(coreOrg, branch, "config.yaml");
    }

    public GHContent getAnsibleInventoryGroupVarsAllYml(GHRepository coreOrg) throws IOException {
        return getAnsibleInventoryGroupVarsAllYml(coreOrg, "main");
    }

    public GHContent getAnsibleInventoryGroupVarsAllYml(GHRepository coreOrg, String sourceBranch) throws IOException {
        return getContent(coreOrg, sourceBranch, "ansible/inventory/group_vars/all.yml");
    }

    private GHContent getContent(GHRepository coreOrg, String branch, String fileName) throws IOException {
        GHContent answer = null;

        try {
            logger.infof("Downloading %s/%s/%s from %s", coreOrg.getOwnerName(), coreOrg.getName(), fileName, branch == null ? "default branch" : branch);

            answer = coreOrg.getFileContent(fileName, branch);
        } catch (GHFileNotFoundException ex) {
            logger.warnf("Did not find %s/%s/%s from %s - maybe branch has been deleted? %s", coreOrg.getOwnerName(), coreOrg.getName(), fileName, branch == null ? "default branch" : branch, ex.getMessage());
            logger.debug("Failure", ex);
        }

        return answer;
    }
}
