package com.garethahealy.githubstats.reflection;

import io.quarkus.runtime.annotations.RegisterForReflection;
import org.apache.commons.pool2.impl.DefaultEvictionPolicy;

@RegisterForReflection(targets = {DefaultEvictionPolicy.class})
public class ApacheCommonsPoolConfiguration {
}
