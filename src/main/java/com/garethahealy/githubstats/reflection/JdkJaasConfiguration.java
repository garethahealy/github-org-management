package com.garethahealy.githubstats.reflection;

import com.sun.security.auth.module.JndiLoginModule;
import com.sun.security.auth.module.KeyStoreLoginModule;
import com.sun.security.auth.module.Krb5LoginModule;
import com.sun.security.auth.module.LdapLoginModule;
import com.sun.security.auth.module.NTLoginModule;
import com.sun.security.auth.module.UnixLoginModule;
import com.sun.security.auth.module.UnixSystem;
import io.quarkus.runtime.annotations.RegisterForReflection;

/**
 * Native image must include every LoginModule listed by {@code jdk.security.auth}, because
 * {@code LoginContext} walks the ServiceLoader catalog and fails if any provider class is missing.
 */
@RegisterForReflection(
        targets = {
                Krb5LoginModule.class,
                UnixLoginModule.class,
                JndiLoginModule.class,
                KeyStoreLoginModule.class,
                LdapLoginModule.class,
                NTLoginModule.class,
                UnixSystem.class
        }
)
public class JdkJaasConfiguration {
}
