package com.garethahealy.githuborgmanagement.services.ldap;

import com.garethahealy.githuborgmanagement.model.users.OrgMember;
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
