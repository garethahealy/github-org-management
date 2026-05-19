package com.garethahealy.githubstats.processors.users.issues;

import com.garethahealy.githubstats.model.users.OrgMemberRepository;
import org.apache.directory.api.ldap.model.exception.LdapException;
import org.kohsuke.github.GHIssue;

import java.io.IOException;

public interface Processor {

    String LGTM_LABEL = "lgtm";

    String id();

    boolean isActive(GHIssue issue);

    void process(GHIssue current, OrgMemberRepository ldapMembers, OrgMemberRepository supplementaryMembers, boolean isDryRun) throws IOException, LdapException;
}
