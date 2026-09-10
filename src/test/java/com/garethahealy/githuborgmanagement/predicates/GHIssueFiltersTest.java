package com.garethahealy.githuborgmanagement.predicates;

import org.junit.jupiter.api.Test;
import org.kohsuke.github.GHIssue;
import org.kohsuke.github.GHPermissionType;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

class GHIssueFiltersTest {

    @Test
    void isLinkUsersWithMatchesTitle() {
        GHIssue issue = Mockito.mock(GHIssue.class);
        when(issue.getTitle()).thenReturn("Request GitHub to Red Hat ID linkage for users with READ");
        when(issue.isPullRequest()).thenReturn(false);

        assertTrue(GHIssueFilters.isLinkUsersWith(GHPermissionType.READ).test(issue));
    }

    @Test
    void isRemoveNonRHMatchesTitle() {
        GHIssue issue = Mockito.mock(GHIssue.class);
        when(issue.getTitle()).thenReturn("Remove users - Not in RH LDAP");
        when(issue.isPullRequest()).thenReturn(false);

        assertTrue(GHIssueFilters.isRemoveNonRH().test(issue));
    }
}
