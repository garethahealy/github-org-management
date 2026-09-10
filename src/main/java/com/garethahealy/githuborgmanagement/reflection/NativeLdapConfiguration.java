package com.garethahealy.githuborgmanagement.reflection;

import io.quarkus.runtime.annotations.RegisterForReflection;
import org.apache.commons.pool2.impl.DefaultEvictionPolicy;
import org.apache.directory.api.ldap.codec.standalone.StandaloneLdapApiService;
import org.apache.mina.transport.socket.nio.NioProcessor;

/**
 * Native-image reflection for LDAP, MINA, Commons Pool, and JGSS Kerberos.
 */
@RegisterForReflection(
        targets = {
                StandaloneLdapApiService.class,
                NioProcessor.class,
                DefaultEvictionPolicy.class
        },
        classNames = {
                "sun.security.jgss.krb5.Krb5MechFactory",
                "sun.security.jgss.GSSContextImpl"
        }
)
public class NativeLdapConfiguration {
}
