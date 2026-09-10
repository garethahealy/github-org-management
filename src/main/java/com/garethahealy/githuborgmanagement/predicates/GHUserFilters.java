package com.garethahealy.githuborgmanagement.predicates;

import com.garethahealy.githuborgmanagement.model.users.OrgMember;
import com.garethahealy.githuborgmanagement.model.users.OrgMemberRepository;
import org.kohsuke.github.GHUser;

import java.util.function.Predicate;

public final class GHUserFilters {

    private GHUserFilters() {
    }

    /**
     * Filter the GitHub user to this member
     *
     * @param member
     * @return
     */
    public static Predicate<GHUser> equals(OrgMember member) {
        String memberLogin = member.gitHubUsername();
        return user -> user.getLogin().equalsIgnoreCase(memberLogin);
    }

    /**
     * Filter GitHub users who we don't know about
     *
     * @param ldapMembers
     * @param supplementaryMembers
     * @return
     */
    public static Predicate<GHUser> notContains(OrgMemberRepository ldapMembers, OrgMemberRepository supplementaryMembers) {
        return user -> {
            String login = user.getLogin();
            return !ldapMembers.containsKey(login) && !supplementaryMembers.containsKey(login);
        };
    }
}
