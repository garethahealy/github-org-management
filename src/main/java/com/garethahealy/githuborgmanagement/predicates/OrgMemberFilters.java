package com.garethahealy.githuborgmanagement.predicates;

import com.garethahealy.githuborgmanagement.model.users.OrgMember;

import java.time.LocalDate;
import java.util.function.Predicate;

public final class OrgMemberFilters {

    private OrgMemberFilters() {
    }

    /**
     * Filter members where the 'date' is after the members.deleteAfter
     *
     * @param date
     * @return
     */
    public static Predicate<OrgMember> deleteAfter(LocalDate date) {
        return member -> member.deleteAfter() != null && date.isAfter(member.deleteAfter());
    }

    /**
     * Filter members without a deleteAfter set
     *
     * @return
     */
    public static Predicate<OrgMember> deleteAfterIsNull() {
        return member -> member.deleteAfter() == null;
    }

    /**
     * Filter members with a deleteAfter set
     *
     * @return
     */
    public static Predicate<OrgMember> deleteAfterIsNotNull() {
        return member -> member.deleteAfter() != null;
    }

    /**
     * Filter to exclude the 'redhat-cop-ci-bot' account
     *
     * @return
     */
    private static Predicate<OrgMember> memberNotBot() {
        return member -> !member.gitHubUsername().equalsIgnoreCase(OrgMember.botGithubUsername());
    }

    /**
     * Filter members without a deleteAfter set and exclude the 'redhat-cop-ci-bot' account
     *
     * @return
     */
    public static Predicate<OrgMember> deleteAfterIsNullAndMemberNotBot() {
        return deleteAfterIsNull().and(memberNotBot());
    }
}
