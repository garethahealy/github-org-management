package com.garethahealy.githubstats.factories;

import com.garethahealy.githubstats.config.LdapConfigProperties;
import com.garethahealy.githubstats.services.ldap.LdapSearchService;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.apache.directory.api.ldap.model.cursor.EntryCursor;
import org.apache.directory.api.ldap.model.entry.Attribute;
import org.apache.directory.api.ldap.model.entry.Entry;
import org.apache.directory.api.ldap.model.exception.LdapException;
import org.apache.directory.api.ldap.model.message.SearchScope;
import org.apache.directory.api.ldap.model.name.Dn;
import org.apache.directory.ldap.client.api.*;
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
    private final LdapConfigProperties ldapConfig;

    private final AtomicBoolean warmedUp = new AtomicBoolean(false);
    private Dn systemDn;
    private LdapConnectionPool connectionPool;

    public LdapConnectionFactory(Logger logger, LdapConfigProperties ldapConfig) {
        this.logger = logger;
        this.ldapConfig = ldapConfig;
    }

    @PostConstruct
    void init() {
        setupSystemDn();
        this.connectionPool = createLdapConnectionPool();
        ensureWarmedUp();
    }

    @PreDestroy
    void destroy() {
        if (connectionPool == null) {
            return;
        }

        try {
            connectionPool.close();
            logger.debug("LDAP connection pool closed");
        } catch (Exception e) {
            logger.warnf(e, "Error closing LDAP connection pool");
        } finally {
            connectionPool = null;
        }
    }

    private void setupSystemDn() {
        try {
            systemDn = new Dn(ldapConfig.dn());
        } catch (LdapException ex) {
            throw new IllegalStateException("Invalid LDAP DN in redhat.ldap.dn: " + ldapConfig.dn(), ex);
        }
    }

    private LdapConnectionPool createLdapConnectionPool() {
        LdapConnectionConfig config = new LdapConnectionConfig();
        config.setLdapHost(ldapConfig.connection());
        config.setLdapPort(ldapConfig.port());

        DefaultLdapConnectionFactory factory = new DefaultLdapConnectionFactory(config);
        GenericObjectPoolConfig<LdapConnection> poolConfig = new GenericObjectPoolConfig<>();
        poolConfig.setMaxTotal(Runtime.getRuntime().availableProcessors());
        poolConfig.setMaxIdle(Math.max(1, Runtime.getRuntime().availableProcessors() / 2));

        return new LdapConnectionPool(new DefaultPoolableLdapConnectionFactory(factory), poolConfig);
    }

    private void ensureWarmedUp() {
        try {
            try (LdapConnectionLease lease = open()) {
                LdapConnection connection = lease.connection();

                String filter = FilterBuilder.equal("uid", ldapConfig.warmupUser()).toString();
                try (EntryCursor cursor = connection.search(systemDn, filter, SearchScope.SUBTREE, "dn")) {
                    for (Entry entry : cursor) {
                        logger.infof("Warmup found %s", entry.getDn());
                        warmedUp.set(true);
                        break;
                    }
                }
            }
        } catch (IOException | LdapException e) {
            logger.error("Failed to open connection to LDAP", e);
        }
    }

    public boolean canConnect() {
        if (!warmedUp.get()) {
            ensureWarmedUp();
        }

        return warmedUp.get();
    }

    public LdapConnectionLease open() throws LdapException {
        return new LdapConnectionLease(connectionPool, connectionPool.getConnection());
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

        try (EntryCursor cursor = connection.search(systemDn, filter.toString(), SearchScope.SUBTREE, attributes)) {
            for (Entry entry : cursor) {
                logger.debugf("Found %s", filter);

                answer.addAll(entry.getAttributes());
            }
        }

        return answer;
    }
}
