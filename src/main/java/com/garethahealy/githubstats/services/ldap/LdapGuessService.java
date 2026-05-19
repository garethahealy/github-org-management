package com.garethahealy.githubstats.services.ldap;

import com.garethahealy.githubstats.model.users.OrgMember;
import org.apache.directory.api.ldap.model.exception.LdapException;

import java.io.IOException;

public interface LdapGuessService {

    OrgMember attempt(OrgMember userToGuess) throws IOException, LdapException;
}
