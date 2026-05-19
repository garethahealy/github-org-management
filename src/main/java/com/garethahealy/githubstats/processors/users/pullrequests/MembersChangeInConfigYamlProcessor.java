package com.garethahealy.githubstats.processors.users.pullrequests;

import com.garethahealy.githubstats.clients.GitHubDiffService;
import com.garethahealy.githubstats.model.users.OrgMember;
import com.garethahealy.githubstats.model.users.OrgMemberRepository;
import com.garethahealy.githubstats.predicates.GHLabelFilters;
import com.garethahealy.githubstats.services.github.GitHubFileRetrievalService;
import com.garethahealy.githubstats.services.github.GitHubRepositoryLookupService;
import com.garethahealy.githubstats.services.ldap.LdapSearchService;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import io.quarkiverse.freemarker.TemplatePath;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.apache.directory.api.ldap.model.exception.LdapException;
import org.apache.directory.ldap.client.api.LdapConnection;
import org.jboss.logging.Logger;
import org.kohsuke.github.*;

import java.io.IOException;
import java.io.StringWriter;
import java.util.*;

/**
 * Looks at OPEN pull requests for new members to the config.yaml that controls GitHub membership and searches for the raiser within LDAP.
 * If found, labels pull request with 'lgtm'
 */
@ApplicationScoped
public class MembersChangeInConfigYamlProcessor implements Processor {

    private final Logger logger;
    private final GitHubRepositoryLookupService gitHubRepositoryLookupService;
    private final GitHubFileRetrievalService gitHubFileRetrievalService;
    private final LdapSearchService ldapSearchService;

    @Inject
    GitHubDiffService gitHubDiffService;

    @Inject
    @TemplatePath("LinkSocialToLDAPComment.ftl")
    Template linkSocialToLDAPComment;

    public MembersChangeInConfigYamlProcessor(Logger logger, GitHubRepositoryLookupService gitHubRepositoryLookupService, GitHubFileRetrievalService gitHubFileRetrievalService, LdapSearchService ldapSearchService) {
        this.logger = logger;
        this.gitHubRepositoryLookupService = gitHubRepositoryLookupService;
        this.gitHubFileRetrievalService = gitHubFileRetrievalService;
        this.ldapSearchService = ldapSearchService;
    }

    @Override
    public String id() {
        return this.getClass().getSimpleName().replace("Processor", "");
    }

    /**
     * Is this a config.yaml change and has it not been labeled with 'lgtm'
     *
     * @param pullRequest
     * @return
     * @throws IOException
     */
    @Override
    public boolean isActive(GHPullRequest pullRequest) throws IOException {
        return isPullRequestConfigChange(pullRequest) && isLabeledCorrectly(pullRequest);
    }

    private boolean isPullRequestConfigChange(GHPullRequest current) throws IOException {
        boolean answer = false;

        List<GHPullRequestFileDetail> filesChanged = current.listFiles().toList();
        for (GHPullRequestFileDetail file : filesChanged) {
            if (file.getFilename().equalsIgnoreCase("config.yaml")) {
                answer = true;
                break;
            }
        }

        return answer;
    }

    private boolean isLabeledCorrectly(GHPullRequest current) throws IOException {
        boolean answer = false;

        boolean isLabeledToIgnore = hasLabel(current.getLabels(), LGTM_LABEL);
        if (isLabeledToIgnore) {
            logger.infof("%s has '%s', ignoring", current.getNumber(), LGTM_LABEL);
        } else {
            if (current.getDeletions() > 0 && current.getAdditions() == 0) {
                logger.warnf("%s PR contains only deletions. Think it is removing members, ignoring", current.getNumber());
            } else {
                answer = true;
            }
        }

        return answer;
    }

    @Override
    public void process(GHPullRequest current, Map<String, Set<String>> data, OrgMemberRepository ldapMembers, OrgMemberRepository supplementaryMembers, boolean isDryRun) throws TemplateException, LdapException {
        try {
            List<String> unknownSourceMembers = collectMembersToCheck(current, current.getHead().getRepository(), current.getHead().getCommit().getSHA1(), data.get(Processor.CONFIG_MEMBERS), ldapMembers, supplementaryMembers);
            List<OrgMember> searchedForMembers = searchViaLdapFor(unknownSourceMembers);

            labelPullRequests(current, searchedForMembers, isDryRun);

            current.refresh();
        } catch (IOException ex) {
            logger.warn(ex.getMessage());
            logger.debug(ex);
        }
    }

    private List<String> collectMembersToCheck(GHPullRequest current, GHRepository orgRepo, String sourceBranch, Set<String> mainConfigMembers, OrgMemberRepository ldapMembers, OrgMemberRepository supplementaryMembers) throws IOException {
        logger.infof("Collecting members for %s/%s on %s", orgRepo.getOwnerName(), orgRepo.getName(), sourceBranch);

        GHContent sourceConfigYaml = gitHubRepositoryLookupService.getConfigYaml(orgRepo, sourceBranch);
        if (sourceConfigYaml == null) {
            throw new IllegalArgumentException("config.yaml is null for " + orgRepo.getOwnerName() + "/" + orgRepo.getName() + "/" + sourceBranch);
        }

        Set<String> sourceMembers = gitHubFileRetrievalService.getConfigMembers(sourceConfigYaml);
        sourceMembers.removeAll(mainConfigMembers);

        List<String> unknownSourceMembers = sourceMembers.stream().filter(member -> !ldapMembers.containsKey(member) && !supplementaryMembers.containsKey(member)).toList();

        List<String> unknownMembers = new ArrayList<>();
        if (!unknownSourceMembers.isEmpty()) {
            String diffContent = gitHubDiffService.getDiff(current);
            for (String unknown : unknownSourceMembers) {
                if (diffContent.contains(unknown)) {
                    unknownMembers.add(unknown);
                }
            }

            logger.infof("Found %s unknown members, compared to redhat-cop/org on main", unknownMembers.size());
        }

        return unknownMembers;
    }

    private List<OrgMember> searchViaLdapFor(List<String> unknownSourceMembers) throws IOException, LdapException {
        List<OrgMember> answer = new ArrayList<>();

        if (!unknownSourceMembers.isEmpty()) {
            try (LdapConnection connection = ldapSearchService.open()) {
                for (String current : unknownSourceMembers) {
                    String rhEmail = ldapSearchService.searchOnGitHubSocial(connection, current);
                    if (rhEmail.isEmpty()) {
                        answer.add(OrgMember.from(current));
                    }
                }
            }
        }

        return answer;
    }

    private void labelPullRequests(GHPullRequest pullRequest, List<OrgMember> searchedForMembers, boolean isDryRun) throws IOException, TemplateException {
        if (searchedForMembers.isEmpty()) {
            labelUserInLdap(pullRequest, isDryRun);
        } else {
            boolean isLabeledCantFindUser = hasLabel(pullRequest.getLabels(), WIP_LABEL);
            if (isLabeledCantFindUser) {
                logger.infof("%s has '%s' so wont label/comment again, ignoring", pullRequest.getNumber(), WIP_LABEL);
            } else {
                labelCantFindUserInLdap(pullRequest, searchedForMembers, isDryRun);
            }
        }
    }

    private void labelUserInLdap(GHPullRequest pullRequest, boolean isDryRun) throws IOException {
        if (isDryRun) {
            logger.warnf("DRY-RUN: Would have labeled '%s' pull request %s and approved", LGTM_LABEL, pullRequest.getNumber());
        } else {
            pullRequest.removeLabels(pullRequest.getLabels());
            pullRequest.addLabels(LGTM_LABEL);
            pullRequest.createReview()
                .event(GHPullRequestReviewEvent.APPROVE)
                .create();

            logger.infof("Labeled (%s) and commented: %s", LGTM_LABEL, pullRequest.getNumber());
        }
    }

    private void labelCantFindUserInLdap(GHPullRequest pullRequest, List<OrgMember> unknownMembers, boolean isDryRun) throws TemplateException, IOException {
        unknownMembers.sort(Comparator.comparing(OrgMember::gitHubUsername, String.CASE_INSENSITIVE_ORDER));

        Map<String, Object> root = new HashMap<>();
        root.put("users", unknownMembers);
        root.put("system", "GitHub");
        root.put("permissions", GHPermissionType.READ);

        StringWriter stringWriter = new StringWriter();
        linkSocialToLDAPComment.process(root, stringWriter);

        if (isDryRun) {
            logger.warnf("DRY-RUN: Would have labeled '%s' pull request %s and added below comment", WIP_LABEL, pullRequest.getNumber());
            logger.warnf(stringWriter.toString());
        } else {
            pullRequest.addLabels(WIP_LABEL);
            pullRequest.comment(stringWriter.toString());
            pullRequest.createReview()
                .event(GHPullRequestReviewEvent.REQUEST_CHANGES)
                .comment("LDAP Link", "config.yaml", 1)
                .create();

            logger.infof("Labeled (%s) and commented: %s", WIP_LABEL, pullRequest.getNumber());
        }
    }

    private boolean hasLabel(Collection<GHLabel> labels, String key) {
        return getLabel(labels, key).isPresent();
    }

    private Optional<GHLabel> getLabel(Collection<GHLabel> labels, String key) {
        return labels.stream().filter(GHLabelFilters.equals(key)).findFirst();
    }
}
