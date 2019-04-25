package de.adorsys.keycloak.config.configuration;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ObjectMapperConfiguration {

    @Bean
    @Qualifier("json")
    public ObjectMapper createJsonObjectMapper() {
        return new ObjectMapper();
    }
}
