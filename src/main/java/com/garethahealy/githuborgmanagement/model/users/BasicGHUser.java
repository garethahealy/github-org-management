package com.garethahealy.githuborgmanagement.model.users;

import org.apache.commons.lang3.builder.CompareToBuilder;
import org.kohsuke.github.GHUser;

import java.io.IOException;

/**
 * Wrapper class for GHUser
 */
public record BasicGHUser(String name, String company, String login, String email) implements Comparable<BasicGHUser> {

    public static BasicGHUser from(GHUser user) throws IOException {
        return new BasicGHUser(user.getName(), user.getCompany(), user.getLogin(), user.getEmail());
    }

    @Override
    public int compareTo(BasicGHUser other) {
        return new CompareToBuilder()
            .append(name(), other.name())
            .append(company(), other.company())
            .append(login(), other.login())
            .append(email(), other.email())
            .toComparison();
    }
}
