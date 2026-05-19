package com.garethahealy.githubstats.processors.users.pullrequests;

import com.garethahealy.githubstats.model.users.OrgMemberRepository;
import freemarker.template.TemplateException;
import org.apache.directory.api.ldap.model.exception.LdapException;
import org.kohsuke.github.GHPullRequest;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Map;
import java.util.Set;

public interface Processor {

    String LGTM_LABEL = "lgtm";
    String WIP_LABEL = "work-in-progress";

    String CONFIG_MEMBERS = "CONFIG_MEMBERS";
    String VARS_MEMBERS = "VARS_MEMBERS";

    String id();

    boolean isActive(GHPullRequest pullRequest) throws IOException;

    void process(GHPullRequest current, Map<String, Set<String>> data, OrgMemberRepository ldapMembers, OrgMemberRepository supplementaryMembers, boolean isDryRun) throws IOException, TemplateException, URISyntaxException, LdapException;
}
