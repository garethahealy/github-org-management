package com.garethahealy.githubstats.factories;

import com.garethahealy.githubstats.config.LdapConfigProperties;
import com.garethahealy.githubstats.services.ldap.LdapSearchService;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.apache.directory.api.ldap.model.cursor.EntryCursor;
import org.apache.directory.api.ldap.model.entry.Attribute;
import org.apache.directory.api.ldap.model.entry.Entry;
import org.apache.directory.api.ldap.model.exception.LdapException;
import org.apache.directory.api.ldap.model.message.SearchScope;
import org.apache.directory.api.ldap.model.name.Dn;
import org.apache.directory.ldap.client.api.LdapConnection;
import org.apache.directory.ldap.client.api.LdapNetworkConnection;
import org.apache.directory.ldap.client.api.search.FilterBuilder;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

@ApplicationScoped
public class LdapConnectionFactory {

    private final Logger logger;
    private final String ldapConnection;
    private final String ldapDn;
    private final String ldapWarmupUser;

    private final AtomicBoolean warmedUp = new AtomicBoolean(false);
    private Dn systemDn;

    @Inject
    public LdapConnectionFactory(Logger logger, LdapConfigProperties config) {
        this.logger = logger;
        this.ldapConnection = config.connection();
        this.ldapDn = config.dn();
        this.ldapWarmupUser = config.warmupUser();
    }

    @PostConstruct
    void init() {
        ensureWarmedUp();
    }

    public boolean canConnect() {
        if (!warmedUp.get()) {
            ensureWarmedUp();
        }

        return warmedUp.get();
    }

    public LdapConnection open() {
        return new LdapNetworkConnection(ldapConnection);
    }

    private Dn getSystemDn() throws LdapException {
        if (systemDn == null) {
            systemDn = new Dn(ldapDn);
        }

        return systemDn;
    }

    private void ensureWarmedUp() {
        try {
            try (LdapConnection connection = open()) {
                FilterBuilder filter = FilterBuilder.equal("uid", ldapWarmupUser);
                String uid = searchDn(connection, filter);
                if (!uid.isEmpty()) {
                    logger.infof("Warmup found %s", uid);
                    warmedUp.set(true);
                }
            }
        } catch (IOException | LdapException e) {
            logger.error("Failed to open connection to LDAP", e);
        }
    }

    /**
     * Search based on Dn
     *
     * @param connection
     * @param filter
     * @return
     * @throws LdapException
     * @throws IOException
     */
    public String searchDn(LdapConnection connection, FilterBuilder filter) throws LdapException, IOException {
        String answer = "";

        Dn systemDn = getSystemDn();
        try (EntryCursor cursor = connection.search(systemDn, filter.toString(), SearchScope.SUBTREE, LdapSearchService.AttributeKeys.Dn)) {
            for (Entry entry : cursor) {
                logger.debugf("Found %s", filter);

                if (entry.getAttributes().isEmpty()) {
                    logger.debugf("- returning dn == %s", entry.getDn().getName());
                    answer = entry.getDn().getName();
                }
            }
        }

        return answer;
    }

    public List<Attribute> search(LdapConnection connection, FilterBuilder filter, String... attributes) throws LdapException, IOException {
        List<Attribute> answer = new ArrayList<>();

        logger.debugf("Searching on: %s", filter);

        Dn systemDn = getSystemDn();
        try (EntryCursor cursor = connection.search(systemDn, filter.toString(), SearchScope.SUBTREE, attributes)) {
            for (Entry entry : cursor) {
                logger.debugf("Found %s", filter);

                answer.addAll(entry.getAttributes());
            }
        }

        return answer;
    }
}
