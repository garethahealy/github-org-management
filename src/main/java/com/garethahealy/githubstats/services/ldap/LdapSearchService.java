package com.garethahealy.githubstats.services.ldap;

import com.garethahealy.githubstats.factories.LdapConnectionFactory;
import com.garethahealy.githubstats.factories.LdapConnectionLease;
import com.garethahealy.githubstats.model.users.OrgMember;
import jakarta.enterprise.context.ApplicationScoped;
import org.apache.directory.api.ldap.model.entry.Attribute;
import org.apache.directory.api.ldap.model.entry.Value;
import org.apache.directory.api.ldap.model.exception.LdapException;
import org.apache.directory.ldap.client.api.LdapConnection;
import org.apache.directory.ldap.client.api.search.FilterBuilder;
import org.jboss.logging.Logger;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Handles searching Red Hat LDAP
 */
@ApplicationScoped
public class LdapSearchService {

    public static class AttributeKeys {
        public static final String Dn = "dn";
        public static final String PrimaryMail = "rhatPrimaryMail";
        public static final String SocialURL = "rhatSocialURL";
        public static final String SocialURLGitHub = SocialURL + "->Github";
        public static final String SocialURLQuay = SocialURL + "->Quay";
    }

    private final Logger logger;
    private final LdapConnectionFactory connectionFactory;

    public LdapSearchService(Logger logger, LdapConnectionFactory connectionFactory) {
        this.logger = logger;
        this.connectionFactory = connectionFactory;
    }

    /**
     * Can connect to LDAP
     *
     * @return
     */
    public boolean canConnect() {
        return connectionFactory.canConnect();
    }

    /**
     * Create a LdapConnection
     *
     * @return
     */
    public LdapConnectionLease open() throws LdapException {
        return connectionFactory.open();
    }

    /**
     * Search based on uid=${param}
     *
     * @param connection
     * @param uid
     * @return
     * @throws LdapException
     * @throws IOException
     */
    public boolean searchOnUser(LdapConnection connection, String uid) throws LdapException, IOException {
        FilterBuilder filter = FilterBuilder.equal("uid", uid);
        String answer = connectionFactory.searchDn(connection, filter);
        return !answer.isEmpty();
    }

    /**
     * Search based on cn=${param}
     *
     * @param connection
     * @param name
     * @return
     * @throws LdapException
     * @throws IOException
     */
    public String searchOnName(LdapConnection connection, String name) throws LdapException, IOException {
        FilterBuilder filter = FilterBuilder.equal("cn", name);
        return searchAndGetPrimaryMail(connection, filter);
    }

    /**
     * Search based on uid=${param}
     *
     * @param connection
     * @param githubId
     * @return
     * @throws LdapException
     * @throws IOException
     */
    public String searchOnGitHubLogin(LdapConnection connection, String githubId) throws LdapException, IOException {
        FilterBuilder filter = FilterBuilder.equal("uid", githubId);
        return searchAndGetPrimaryMail(connection, filter);
    }

    /**
     * Search based on ${AttributeKeys.SocialURL}=Github->${param}
     *
     * @param connection
     * @param githubId
     * @return
     * @throws LdapException
     * @throws IOException
     */
    public String searchOnGitHubSocial(LdapConnection connection, String githubId) throws LdapException, IOException {
        String found = searchOnGitHubSocialExact(connection, githubId);
        if (found.isEmpty()) {
            found = searchOnGitHubSocialFuzzy(connection, githubId);
        }

        return found;
    }

    private String searchOnGitHubSocialExact(LdapConnection connection, String githubId) throws LdapException, IOException {
        FilterBuilder filter = FilterBuilder.equal(AttributeKeys.SocialURL, "Github->https://github.com/" + githubId);
        return searchAndGetPrimaryMail(connection, filter);
    }

    private String searchOnGitHubSocialFuzzy(LdapConnection connection, String githubId) throws LdapException, IOException {
        FilterBuilder filter = FilterBuilder.contains(AttributeKeys.SocialURL, "Github->", githubId);
        return searchAndGetPrimaryMail(connection, filter);
    }

    /**
     * Search based on ${AttributeKeys.SocialURL}=Quay->${param}
     *
     * @param connection
     * @param quayId
     * @return
     * @throws LdapException
     * @throws IOException
     */
    public String searchOnQuaySocial(LdapConnection connection, String quayId) throws LdapException, IOException {
        FilterBuilder filter = FilterBuilder.contains(AttributeKeys.SocialURL, "Quay->", quayId);
        return searchAndGetPrimaryMail(connection, filter);
    }

    /**
     * Search based on ${AttributeKeys.PrimaryMail}=${param}
     *
     * @param connection
     * @param email
     * @return
     * @throws LdapException
     * @throws IOException
     */
    public String searchOnPrimaryMail(LdapConnection connection, String email) throws LdapException, IOException {
        FilterBuilder filter = FilterBuilder.equal(AttributeKeys.PrimaryMail, email);
        return searchAndGetPrimaryMail(connection, filter);
    }

    /**
     * Search based on a filter and return the PrimaryMail
     *
     * @param connection
     * @param filter
     * @return
     * @throws LdapException
     * @throws IOException
     */
    private String searchAndGetPrimaryMail(LdapConnection connection, FilterBuilder filter) throws LdapException, IOException {
        Map<String, List<String>> attributes = parseAttributes(connectionFactory.search(connection, filter, AttributeKeys.PrimaryMail));
        List<String> values = attributes.getOrDefault(AttributeKeys.PrimaryMail, new ArrayList<>());
        return values.size() == 1 ? values.getFirst() : "";
    }

    /**
     * Retrieve OrgMember based on ${AttributeKeys.PrimaryMail}=${email} and ${AttributeKeys.SocialURL}=Github->${githubId}
     *
     * @param connection
     * @param member
     * @return
     * @throws LdapException
     * @throws IOException
     */
    public OrgMember retrieve(LdapConnection connection, OrgMember member) throws LdapException, IOException {
        FilterBuilder filter = FilterBuilder.and(
            FilterBuilder.equal(AttributeKeys.PrimaryMail, member.redhatEmailAddress()),
            FilterBuilder.contains(AttributeKeys.SocialURL, "Github->", member.gitHubUsername())
        );

        OrgMember found = OrgMember.from(member.gitHubUsername(), parseAttributes(connectionFactory.search(connection, filter, AttributeKeys.PrimaryMail, AttributeKeys.SocialURL)));
        return found.redhatEmailAddress() == null || found.redhatEmailAddress().isEmpty() ? null : found;
    }

    /**
     * Retrieve OrgMember based on ${AttributeKeys.PrimaryMail}=${param}
     *
     * @param connection
     * @param email
     * @return
     * @throws LdapException
     * @throws IOException
     */
    public OrgMember retrieve(LdapConnection connection, String githubUsername, String email) throws LdapException, IOException {
        FilterBuilder filter = FilterBuilder.equal(AttributeKeys.PrimaryMail, email);

        OrgMember found = OrgMember.from(githubUsername, parseAttributes(connectionFactory.search(connection, filter, AttributeKeys.PrimaryMail, AttributeKeys.SocialURL)));
        return found.redhatEmailAddress() == null || found.redhatEmailAddress().isEmpty() ? null : found;
    }

    private Map<String, List<String>> parseAttributes(List<Attribute> attributes) {
        Map<String, List<String>> answer = new HashMap<>();

        for (Attribute found : attributes) {
            if (found.getId().equalsIgnoreCase(LdapSearchService.AttributeKeys.PrimaryMail)) {
                logger.debugf("- returning %s == %s", LdapSearchService.AttributeKeys.PrimaryMail, found.get().toString());
                answer.put(LdapSearchService.AttributeKeys.PrimaryMail, List.of(found.get().toString()));
            } else if (found.getId().equalsIgnoreCase(LdapSearchService.AttributeKeys.SocialURL)) {
                for (Value current : found) {
                    String[] split = current.toString().split("->");

                    String key = LdapSearchService.AttributeKeys.SocialURL + "->" + split[0];
                    String valueStr = split[1];

                    logger.debugf("- returning %s == %s", key, valueStr);
                    if (answer.containsKey(key)) {
                        List<String> items = answer.get(key);
                        items.add(valueStr);
                    } else {
                        answer.put(key, new ArrayList<>(List.of(valueStr)));
                    }
                }
            } else {
                logger.debugf("- returning %s == %s", found.getId(), found.get().toString());
                answer.put(found.getId(), List.of(found.get().toString()));
            }
        }

        return answer;
    }
}
