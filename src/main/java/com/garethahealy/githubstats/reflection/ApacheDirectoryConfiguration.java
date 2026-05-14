package com.garethahealy.githubstats.reflection;

import io.quarkus.runtime.annotations.RegisterForReflection;
import org.apache.directory.api.ldap.codec.standalone.StandaloneLdapApiService;

@RegisterForReflection(targets = {StandaloneLdapApiService.class})
public class ApacheDirectoryConfiguration {
}
