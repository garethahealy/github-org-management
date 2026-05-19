package com.garethahealy.githubstats.processors.users.issues;

import com.garethahealy.githubstats.model.users.OrgMemberRepository;
import com.garethahealy.githubstats.predicates.GHLabelFilters;
import com.garethahealy.githubstats.services.ldap.LdapSearchService;
import jakarta.enterprise.context.ApplicationScoped;
import org.apache.directory.api.ldap.model.exception.LdapException;
import org.apache.directory.ldap.client.api.LdapConnection;
import org.jboss.logging.Logger;
import org.kohsuke.github.GHIssue;
import org.kohsuke.github.GHLabel;
import org.kohsuke.github.GHUser;

import java.io.IOException;
import java.util.Collection;
import java.util.Optional;

/**
 * Looks at OPEN issues for new members and searches for the raiser within LDAP.
 * If found, labels issue with 'lgtm'
 */
@ApplicationScoped
public class AddMeAsMemberProcessor implements Processor {

    private final Logger logger;
    private final LdapSearchService ldapSearchService;

    public AddMeAsMemberProcessor(Logger logger, LdapSearchService ldapSearchService) {
        this.logger = logger;
        this.ldapSearchService = ldapSearchService;
    }

    @Override
    public String id() {
        return this.getClass().getSimpleName().replace("Processor", "");
    }

    /**
     * Is this a new member request issue and has it not been labeled with 'lgtm'
     *
     * @param issue
     * @return
     */
    @Override
    public boolean isActive(GHIssue issue) {
        return isIssueNewMemberRequest(issue) && isLabeledCorrectly(issue);
    }

    private boolean isIssueNewMemberRequest(GHIssue current) {
        return current.getTitle().equalsIgnoreCase("Please add me as a member of the redhat-cop organization");
    }

    private boolean isLabeledCorrectly(GHIssue current) {
        boolean answer = false;

        boolean isLabeledToIgnore = hasLabel(current.getLabels(), LGTM_LABEL);
        if (isLabeledToIgnore) {
            logger.infof("%s has '%s', ignoring", current.getNumber(), LGTM_LABEL);
        } else {
            answer = true;
        }

        return answer;
    }

    @Override
    public void process(GHIssue current, OrgMemberRepository ldapMembers, OrgMemberRepository supplementaryMembers, boolean isDryRun) throws IOException, LdapException {
        if (isAlreadyKnownMember(current.getUser(), ldapMembers, supplementaryMembers)) {
            logger.infof("Issue was raised by %s, but they are a known member, ignoring", current.getUser());
        } else {
            boolean found = searchViaLdapFor(current.getUser());
            if (found) {
                labelUserInLdap(current, isDryRun);
            }
        }
    }

    private boolean isAlreadyKnownMember(GHUser user, OrgMemberRepository ldapMembers, OrgMemberRepository supplementaryMembers) {
        return ldapMembers.containsKey(user.getLogin()) || supplementaryMembers.containsKey(user.getLogin());
    }

    private boolean searchViaLdapFor(GHUser user) throws IOException, LdapException {
        String answer;
        try (LdapConnection connection = ldapSearchService.open()) {
            answer = ldapSearchService.searchOnGitHubSocial(connection, user.getLogin());
        }

        return answer != null && !answer.isEmpty();
    }

    private void labelUserInLdap(GHIssue issue, boolean isDryRun) throws IOException {
        if (isDryRun) {
            logger.warnf("DRY-RUN: Would have labeled '%s' issue %s", LGTM_LABEL, issue.getNumber());
        } else {
            issue.addLabels(LGTM_LABEL);

            logger.infof("Labeled (%s): %s", LGTM_LABEL, issue.getNumber());
        }
    }

    private boolean hasLabel(Collection<GHLabel> labels, String key) {
        return getLabel(labels, key).isPresent();
    }

    private Optional<GHLabel> getLabel(Collection<GHLabel> labels, String key) {
        return labels.stream().filter(GHLabelFilters.equals(key)).findFirst();
    }
}
