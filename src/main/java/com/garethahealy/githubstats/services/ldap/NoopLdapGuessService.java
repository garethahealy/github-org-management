package com.garethahealy.githubstats.services.ldap;

import com.garethahealy.githubstats.model.users.OrgMember;
import jakarta.enterprise.context.ApplicationScoped;

/**
 * NoOp guess service
 */
@ApplicationScoped
public class NoopLdapGuessService implements LdapGuessService {

    @Override
    public OrgMember attempt(OrgMember userToGuess) {
        //NOOP
        return null;
    }
}
