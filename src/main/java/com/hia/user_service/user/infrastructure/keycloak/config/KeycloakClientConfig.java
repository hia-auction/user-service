package com.hia.user_service.user.infrastructure.keycloak.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.net.http.HttpClient;
import java.time.Duration;

@Configuration(proxyBeanMethods = false)
public class KeycloakClientConfig {

    @Bean
    public RestClient keycloakRestClient(
            RestClient.Builder builder,
            KeycloakAdminProperties properties
    ) {
        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(3))
                .build();

        JdkClientHttpRequestFactory requestFactory =
                new JdkClientHttpRequestFactory(httpClient);

        requestFactory.setReadTimeout(Duration.ofSeconds(5));

        return builder.baseUrl(properties.getBaseUrl())
                .requestFactory(requestFactory)
                .build();
    }
}
