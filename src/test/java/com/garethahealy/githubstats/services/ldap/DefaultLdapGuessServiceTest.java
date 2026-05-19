package com.garethahealy.githubstats.services.ldap;

import com.garethahealy.githubstats.testutils.BaseRequiresLdapConnection;
import com.garethahealy.githubstats.testutils.OrgMemberMockData;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import org.apache.directory.api.ldap.model.exception.LdapException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;
import org.kohsuke.github.GitHub;

import java.io.IOException;

@QuarkusTest
class DefaultLdapGuessServiceTest extends BaseRequiresLdapConnection {

    @Inject
    DefaultLdapGuessService defaultLdapGuessService;

    @Inject
    @Named("read")
    GitHub client;

    @Test
    @EnabledIf("canConnectVpn")
    void attempt() throws IOException, LdapException {
        defaultLdapGuessService.attempt(OrgMemberMockData.getMe(client));
    }
}
