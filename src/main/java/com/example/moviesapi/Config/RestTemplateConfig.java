package com.example.moviesapi.Config;

import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;

@Configuration
public class RestTemplateConfig {

    @Bean
    public RestTemplate restTemplate(RestTemplateBuilder builder) {
        return builder
                .connectTimeout(Duration.ofSeconds(10))
                .readTimeout(Duration.ofSeconds(30))
                .errorHandler(new org.springframework.web.client.ResponseErrorHandler() {
                    @Override
                    public boolean hasError(org.springframework.http.client.ClientHttpResponse response) {
                        return false;
                    }

                    @Override
                    public void handleError(java.net.URI url, org.springframework.http.HttpMethod method, org.springframework.http.client.ClientHttpResponse response) {
                        // Empty implementation to suppress error handling
                    }
                })
                .build();
    }
}