package de.adorsys.keycloak.config.configuration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ObjectMapperConfiguration {

    @Bean
    @Qualifier("yaml")
    public ObjectMapper createYamlObjectMapper() {
        final YAMLFactory ymlFactory = new YAMLFactory();
        return new ObjectMapper(ymlFactory);
    }

    @Bean
    @Qualifier("json")
    public ObjectMapper createJsonObjectMapper() {
        return new ObjectMapper();
    }
}
