package com.garethahealy.githuborgmanagement.config;

import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Singleton;

@Singleton
public class YAMLMapperConfig {

    @Produces
    @Singleton
    public YAMLMapper mapper() {
        return new YAMLMapper();
    }
}
