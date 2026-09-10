package com.garethahealy.githuborgmanagement.factories;

import com.garethahealy.githuborgmanagement.config.LdapConfigProperties;
import org.apache.directory.api.ldap.model.exception.LdapException;
import org.apache.directory.api.ldap.model.exception.LdapOperationException;
import org.apache.directory.api.ldap.model.message.BindResponse;
import org.apache.directory.api.ldap.model.message.LdapResult;
import org.apache.directory.api.ldap.model.message.ResultCodeEnum;
import org.apache.directory.ldap.client.api.DefaultLdapConnectionFactory;
import org.apache.directory.ldap.client.api.LdapConnection;
import org.apache.directory.ldap.client.api.LdapConnectionConfig;
import org.apache.directory.ldap.client.api.LdapNetworkConnection;
import org.apache.directory.ldap.client.api.SaslGssApiRequest;
import org.jboss.logging.Logger;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Path;

/**
 * Binds each pooled LDAP connection with SASL/GSSAPI using a FILE Kerberos ticket cache.
 */
public final class GssapiLdapConnectionFactory extends DefaultLdapConnectionFactory {

    private final Logger logger;
    private final LdapConfigProperties.Kerberos kerberos;
    private final String krb5Conf;

    public GssapiLdapConnectionFactory(LdapConnectionConfig config, Logger logger, LdapConfigProperties.Kerberos kerberos) {
        super(config);
        this.logger = logger;
        this.kerberos = kerberos;
        try {
            this.krb5Conf = KerberosKrb5ConfGenerator.write(kerberos.realm(), kerberos.kdc()).toString();
            System.setProperty("java.security.krb5.conf", this.krb5Conf);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to write krb5.conf for LDAP GSSAPI", e);
        }
    }

    @Override
    public LdapConnection bindConnection(LdapConnection connection) throws LdapException {
        try {
            Path ticketCache = KerberosTicketCache.resolve(kerberos.ticketCache());

            logger.debugf("LDAP GSSAPI bind using ticket cache %s (realm=%s, kdc=%s)", ticketCache, kerberos.realm(), kerberos.kdc());

            BindResponse response = bindGssapi(connection, createRequest(ticketCache));
            LdapResult result = response.getLdapResult();
            if (result.getResultCode() != ResultCodeEnum.SUCCESS) {
                String diagnostic = result.getDiagnosticMessage();
                throw new LdapOperationException(result.getResultCode(), diagnostic == null || diagnostic.isBlank() ? "GSSAPI bind failed" : diagnostic);
            }

            return connection;
        } catch (LdapException e) {
            closeQuietly(connection);
            throw e;
        } catch (Exception | UnsatisfiedLinkError e) {
            closeQuietly(connection);
            throw new LdapException(e.getMessage(), e);
        }
    }

    private SaslGssApiRequest createRequest(Path ticketCache) {
        SaslGssApiRequest request = new SaslGssApiRequest();
        request.setRealmName(kerberos.realm());
        request.setKrb5ConfFilePath(krb5Conf);
        request.setLoginModuleConfiguration(new TicketCacheLoginConfiguration(ticketCache.toString()));
        request.setMutualAuthentication(true);
        return request;
    }

    private static BindResponse bindGssapi(LdapConnection connection, SaslGssApiRequest request) throws LdapException {
        if (!(connection instanceof LdapNetworkConnection networkConnection)) {
            throw new LdapException("GSSAPI bind requires LdapNetworkConnection, got " + connection.getClass().getName());
        }

        // Must use LdapNetworkConnection.bind(SaslGssApiRequest); LdapConnection.bind(SaslRequest) skips JAAS.
        return networkConnection.bind(request);
    }

    private static void closeQuietly(LdapConnection connection) {
        try {
            connection.close();
        } catch (IOException ignored) {
            // already failing the bind
        }
    }
}
