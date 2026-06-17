package com.ck10.inventory.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

@Data
@Configuration
@ConfigurationProperties(prefix = "python.service")
public class PythonServiceConfig {

    private String baseUrl = "http://localhost:5002";

    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}
