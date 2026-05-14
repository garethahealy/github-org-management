package com.garethahealy.githubstats.model.users;

import com.garethahealy.githubstats.services.ldap.LdapSearchService;
import io.quarkus.runtime.annotations.RegisterForReflection;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.lang3.builder.CompareToBuilder;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.kohsuke.github.GHUser;

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

/**
 * Holds data about members that are part of the GitHub Org and have had their data collected from LDAP
 */
@RegisterForReflection // NOTE: Needed by freemarker to deserialize in native mode
public record OrgMember(String redhatEmailAddress, String gitHubUsername, List<String> linkedGitHubUsernames,
                        List<String> linkedQuayUsernames, Source source, LocalDate deleteAfter,
                        BasicGHUser basicGHUser) implements Comparable<OrgMember> {

    private static final String BOT_GITHUB_USERNAME = "redhat-cop-ci-bot";

    public enum Headers {
        RedHatEmailAddress,
        GitHubUsername,
        LinkedGitHubUsernames,
        LinkedQuayUsernames,
        Source,
        DeleteAfter
    }

    public enum Source {
        Automated,
        Manual,
        GoogleSheet
    }

    public static String botGithubUsername() {
        return BOT_GITHUB_USERNAME;
    }

    /**
     * Bot account
     *
     * @return
     */
    public static OrgMember bot() {
        return new OrgMember("ablock@redhat.com", BOT_GITHUB_USERNAME, Collections.emptyList(), Collections.emptyList(), Source.Manual, null, null);
    }

    /**
     * When they are in the config but not a GitHub member yet
     *
     * @param gitHubUsername
     * @return
     */
    public static OrgMember from(String gitHubUsername) {
        return new OrgMember(null, gitHubUsername, Collections.emptyList(), Collections.emptyList(), Source.Automated, null, null);
    }

    /**
     * When they are a GitHub member
     *
     * @param ghUser
     * @return
     * @throws IOException
     */
    public static OrgMember from(GHUser ghUser) throws IOException {
        return new OrgMember(null, ghUser.getLogin(), Collections.emptyList(), Collections.emptyList(), Source.Automated, null, BasicGHUser.from(ghUser));
    }

    /**
     * When they've been stored into the CSVs
     *
     * @param record
     * @return
     */
    public static OrgMember from(CSVRecord record) {
        String linkedGithub = record.get(OrgMember.Headers.LinkedGitHubUsernames);
        List<String> linkedGithubSplit = Stream.of(linkedGithub.split(":"))
            .map(String::trim)
            .filter(s -> !s.isEmpty())
            .toList();

        String linkedQuay = record.get(OrgMember.Headers.LinkedQuayUsernames);
        List<String> linkedQuaySplit = Stream.of(linkedQuay.split(":"))
            .map(String::trim)
            .filter(s -> !s.isEmpty())
            .toList();

        String deleteAfter = record.get(Headers.DeleteAfter);

        return new OrgMember(record.get(OrgMember.Headers.RedHatEmailAddress),
            record.get(OrgMember.Headers.GitHubUsername),
            new ArrayList<>(linkedGithubSplit),
            new ArrayList<>(linkedQuaySplit),
            Source.valueOf(record.get(OrgMember.Headers.Source)),
            deleteAfter == null || deleteAfter.isEmpty() ? null : LocalDate.parse(deleteAfter),
            null);
    }

    /**
     * When we've got them back from LDAP
     *
     * @param githubId
     * @param ldapAttributes
     * @return
     */
    public static OrgMember from(String githubId, Map<String, List<String>> ldapAttributes) {
        String email = ldapAttributes.containsKey(LdapSearchService.AttributeKeys.PrimaryMail)
            ? ldapAttributes.get(LdapSearchService.AttributeKeys.PrimaryMail).getFirst()
            : "";
        return new OrgMember(email,
            githubId,
            cleanupValues(ldapAttributes, LdapSearchService.AttributeKeys.SocialURLGitHub),
            cleanupValues(ldapAttributes, LdapSearchService.AttributeKeys.SocialURLQuay),
            Source.Automated,
            null, null);
    }

    public OrgMember withDeleteAfter(LocalDate deleteAfter) {
        return new OrgMember(redhatEmailAddress, gitHubUsername, linkedGitHubUsernames, linkedQuayUsernames, source, deleteAfter, basicGHUser);
    }

    public OrgMember withRedhatEmailAddress(String redhatEmailAddress) {
        return new OrgMember(redhatEmailAddress, gitHubUsername, linkedGitHubUsernames, linkedQuayUsernames, source, deleteAfter, basicGHUser);
    }

    private static List<String> cleanupValues(Map<String, List<String>> ldapAttributes, String key) {
        if (!ldapAttributes.containsKey(key)) {
            return List.of();
        }

        List<String> values = ldapAttributes.get(key);
        if (values.isEmpty()) {
            return List.of();
        }

        List<String> answer = new ArrayList<>(values.size());
        for (String current : values) {
            answer.add(removeDomainName(current));
        }

        return answer;
    }

    /**
     * Remove any domain names so we've just got their username
     *
     * @param current
     * @return
     */
    private static String removeDomainName(String current) {
        String value = current.replace("quay.io/user/", "")
            .replace("quay.io/repository/", "")
            .replace("quay.io/organization/", "")
            .replace("quay.io/", "")
            .replace("github.com/", "")
            .replace("www", "")
            .replace("https://", "")
            .replace("http://", "")
            .replace(".", "");

        if (value.endsWith("/")) {
            value = value.substring(0, value.length() - 1);
        }

        return value;
    }

    public List<String> toArray() {
        return List.of(redhatEmailAddress, gitHubUsername, String.join(":", linkedGitHubUsernames), String.join(":", linkedQuayUsernames), source.toString(), deleteAfter == null ? "" : deleteAfter.format(DateTimeFormatter.ISO_LOCAL_DATE));
    }

    @Override
    public int compareTo(OrgMember other) {
        return new CompareToBuilder()
            .append(redhatEmailAddress, other.redhatEmailAddress())
            .append(gitHubUsername, other.gitHubUsername())
            .append(linkedGitHubUsernames, other.linkedGitHubUsernames())
            .append(linkedQuayUsernames, other.linkedQuayUsernames())
            .append(source, other.source())
            .toComparison();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        OrgMember orgMember = (OrgMember) o;

        return new EqualsBuilder().append(source, orgMember.source)
            .append(gitHubUsername, orgMember.gitHubUsername)
            .append(redhatEmailAddress, orgMember.redhatEmailAddress)
            .append(linkedQuayUsernames, orgMember.linkedQuayUsernames)
            .append(linkedGitHubUsernames, orgMember.linkedGitHubUsernames).isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(17, 37)
            .append(redhatEmailAddress)
            .append(gitHubUsername)
            .append(linkedGitHubUsernames)
            .append(linkedQuayUsernames)
            .append(source).toHashCode();
    }
}
