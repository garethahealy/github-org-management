package com.garethahealy.githubstats.reflection;

import io.quarkus.runtime.annotations.RegisterForReflection;
import org.apache.directory.api.ldap.codec.standalone.StandaloneLdapApiService;
import org.apache.directory.ldap.client.api.SaslGssApiRequest;

@RegisterForReflection(targets = {StandaloneLdapApiService.class, SaslGssApiRequest.class})
public class ApacheDirectoryConfiguration {
}
