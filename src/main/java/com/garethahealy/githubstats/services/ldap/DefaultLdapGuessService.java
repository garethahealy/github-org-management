package com.garethahealy.githubstats.services.ldap;

import com.garethahealy.githubstats.factories.LdapConnectionLease;
import com.garethahealy.githubstats.model.users.OrgMember;
import jakarta.enterprise.context.ApplicationScoped;
import org.apache.directory.api.ldap.model.exception.LdapException;
import org.apache.directory.ldap.client.api.LdapConnection;
import org.jboss.logging.Logger;

import java.io.IOException;

/**
 * Handles searching LDAP and attempts to 'guess' who someone might be, based on their name or user id
 */
@ApplicationScoped
public class DefaultLdapGuessService implements LdapGuessService {

    private final Logger logger;
    private final LdapSearchService ldapSearchService;

    public DefaultLdapGuessService(Logger logger, LdapSearchService ldapSearchService) {
        this.logger = logger;
        this.ldapSearchService = ldapSearchService;
    }

    /**
     * Attempt to guess the 'userToGuess' using several rules
     *
     * @param userToGuess
     * @return
     * @throws IOException
     * @throws LdapException
     */
    public OrgMember attempt(OrgMember userToGuess) throws IOException, LdapException {
        OrgMember guessed = null;

        try (LdapConnectionLease lease = ldapSearchService.open()) {
            LdapConnection connection = lease.connection();
            logger.infof("Attempting to guess %s", userToGuess.gitHubUsername());

            OrgMember guess = guessViaGithubLoginLinked(connection, userToGuess);
            if (guess == null) {
                guess = guessViaGithubProfileEmail(connection, userToGuess);
            }

            if (guess == null) {
                guess = guessViaGithubLoginMatchingUID(connection, userToGuess);
            }

            if (guess == null) {
                guess = guessViaGithubProfileName(connection, userToGuess);
            }

            if (guess == null) {
                if (guessWorksForRedHat(userToGuess)) {
                    guessed = userToGuess;
                }
            } else {
                guessed = guess;
            }
        }

        return guessed;
    }

    private OrgMember guessViaGithubLoginLinked(LdapConnection connection, OrgMember user) throws IOException, LdapException {
        OrgMember answer = null;

        String rhEmail = ldapSearchService.searchOnGitHubSocial(connection, user.gitHubUsername());
        if (!rhEmail.isEmpty()) {
            answer = user.withRedhatEmailAddress(rhEmail);
        }

        return answer;
    }

    /**
     * Search LDAP for their email, if it contains @redhat.com
     *
     * @param connection
     * @param user
     * @return
     * @throws IOException
     * @throws LdapException
     */
    private OrgMember guessViaGithubProfileEmail(LdapConnection connection, OrgMember user) throws IOException, LdapException {
        OrgMember answer = null;

        if (user.basicGHUser().email() != null && !user.basicGHUser().email().isEmpty()) {
            if (user.basicGHUser().email().contains("@redhat.com")) {
                String rhEmail = ldapSearchService.searchOnPrimaryMail(connection, user.basicGHUser().email());
                if (!rhEmail.isEmpty()) {
                    answer = user.withRedhatEmailAddress(rhEmail);
                }
            }
        }

        return answer;
    }

    /**
     * Search LDAP for their GitHub ID matching their Red Hat ID
     *
     * @param connection
     * @param user
     * @return
     * @throws IOException
     * @throws LdapException
     */
    private OrgMember guessViaGithubLoginMatchingUID(LdapConnection connection, OrgMember user) throws IOException, LdapException {
        OrgMember answer = null;

        String rhEmail = ldapSearchService.searchOnGitHubLogin(connection, user.basicGHUser().login());
        if (!rhEmail.isEmpty()) {
            answer = user.withRedhatEmailAddress(rhEmail);
        }

        return answer;
    }

    /**
     * Search LDAP for their human-readable name
     *
     * @param connection
     * @param user
     * @return
     * @throws IOException
     * @throws LdapException
     */
    private OrgMember guessViaGithubProfileName(LdapConnection connection, OrgMember user) throws IOException, LdapException {
        OrgMember answer = null;

        if (user.basicGHUser().name() != null && !user.basicGHUser().name().isEmpty()) {
            String rhEmail = ldapSearchService.searchOnName(connection, user.basicGHUser().name());
            if (!rhEmail.isEmpty()) {
                answer = user.withRedhatEmailAddress(rhEmail);
            }
        }

        return answer;
    }

    /**
     * Check if their profile says they work for Red Hat
     *
     * @param user
     * @return
     * @throws IOException
     */
    private boolean guessWorksForRedHat(OrgMember user) {
        boolean answer = false;

        if (user.basicGHUser().company() != null && !user.basicGHUser().company().isEmpty()) {
            String companyLower = user.basicGHUser().company().toLowerCase();
            if (companyLower.contains("redhat") || companyLower.contains("red hat")) {
                answer = true;
            }
        }

        return answer;
    }
}
