package com.garethahealy.githuborgmanagement.predicates;

import org.kohsuke.github.GHIssue;
import org.kohsuke.github.GHPermissionType;

import java.util.function.Predicate;

public final class GHIssueFilters {

    private static final String LINK_USERS_TITLE_PREFIX = "Request GitHub to Red Hat ID linkage for users with ";
    private static final String REMOVE_NON_RH_TITLE = "Remove users - Not in RH LDAP";

    private GHIssueFilters() {
    }

    /**
     * Is the issue a request to link their GitHub to rover created by this tool
     *
     * @param permissions
     * @return
     */
    public static Predicate<GHIssue> isLinkUsersWith(GHPermissionType permissions) {
        return issue -> !issue.isPullRequest() && issue.getTitle().equalsIgnoreCase(LINK_USERS_TITLE_PREFIX + permissions);
    }

    /**
     * Is the issue a request to remove people who have left Red Hat created by this tool
     *
     * @return
     */
    public static Predicate<GHIssue> isRemoveNonRH() {
        return issue -> !issue.isPullRequest() && issue.getTitle().equalsIgnoreCase(REMOVE_NON_RH_TITLE);
    }
}
