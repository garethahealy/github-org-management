package com.garethahealy.githuborgmanagement.config;

import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;

@QuarkusTest
class YAMLMapperConfigTest {

    @Inject
    YAMLMapperConfig yamlMapperConfig;

    @Inject
    YAMLMapper mapper;

    @Test
    void configGetsMapper() {
        assertNotNull(yamlMapperConfig.mapper());
    }

    @Test
    void mapperInjected() {
        assertNotNull(mapper);
    }
}
