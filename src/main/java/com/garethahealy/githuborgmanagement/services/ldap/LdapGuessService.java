package com.garethahealy.githuborgmanagement.services.ldap;

import com.garethahealy.githuborgmanagement.model.users.OrgMember;
import org.apache.directory.api.ldap.model.exception.LdapException;

import java.io.IOException;

public interface LdapGuessService {

    OrgMember attempt(OrgMember userToGuess) throws IOException, LdapException;
}
