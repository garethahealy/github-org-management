package com.garethahealy.githubstats.reflection;

import io.quarkus.runtime.annotations.RegisterForReflection;
import org.apache.mina.transport.socket.nio.NioProcessor;

@RegisterForReflection(targets = {NioProcessor.class})
public class ApacheMinaConfiguration {
}
