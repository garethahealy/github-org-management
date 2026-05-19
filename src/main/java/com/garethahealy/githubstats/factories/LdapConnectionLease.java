package com.garethahealy.githubstats.factories;

import org.apache.directory.api.ldap.model.exception.LdapException;
import org.apache.directory.ldap.client.api.LdapConnection;
import org.apache.directory.ldap.client.api.LdapConnectionPool;

import java.util.Objects;

/**
 * Try-with-resources handle for a pooled {@link LdapConnection}. {@link #close()} returns the connection to the pool
 * using the same reference {@link LdapConnectionPool#getConnection()} returned—unlike
 * {@link LdapConnection#close()} on Apache Directory's pooled wrapper, which can trigger
 * {@code IllegalStateException: Returned object not currently part of this pool}.
 */
public final class LdapConnectionLease implements AutoCloseable {

    private final LdapConnectionPool pool;
    private final LdapConnection connection;
    private boolean released;

    LdapConnectionLease(LdapConnectionPool pool, LdapConnection connection) {
        this.pool = Objects.requireNonNull(pool);
        this.connection = Objects.requireNonNull(connection);
    }

    public LdapConnection connection() {
        return connection;
    }

    @Override
    public void close() throws LdapException {
        if (released) {
            return;
        }

        released = true;
        pool.releaseConnection(connection);
    }
}
