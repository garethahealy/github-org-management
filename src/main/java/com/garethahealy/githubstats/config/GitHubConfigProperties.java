package com.garethahealy.githubstats.config;

import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithName;

@ConfigMapping(prefix = "github")
public interface GitHubConfigProperties {

    String login();

    String oauth();

    @WithName("write-oauth")
    String writeOauth();
}
