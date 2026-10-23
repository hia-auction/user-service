package com.hia.user_service.user.infrastructure.keycloak.client;

import com.hia.common.exception.CustomException;
import com.hia.user_service.user.application.exception.UserErrorCode;
import com.hia.user_service.user.infrastructure.keycloak.config.KeycloakAdminProperties;
import com.hia.user_service.user.infrastructure.keycloak.dto.KeycloakTokenResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Slf4j
@Component
public class KeycloakTokenClient {

    private final RestClient restClient;
    private final KeycloakAdminProperties properties;

    public KeycloakTokenClient(
            @Qualifier("keycloakRestClient") RestClient restClient,
            KeycloakAdminProperties properties
    ) {
        this.restClient = restClient;
        this.properties = properties;
    }

    public KeycloakTokenResponse issueToken() {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();

        form.add("grant_type", "client_credentials");

        KeycloakTokenResponse response;
        try {
            response = restClient.post()
                    .uri("/realms/{realm}/protocol/openid-connect/token",
                            properties.getRealm())
                    .headers(headers -> headers.setBasicAuth(
                            properties.getClientId(),
                            properties.getClientSecret()
                    ))
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(form)
                    .retrieve()
                    .onStatus(HttpStatusCode::isError,
                            ((request, httpResponse) -> {
                                log.warn("Keycloak token request failed: status={}", httpResponse.getStatusCode().value());
                                throw new CustomException(UserErrorCode.KEYCLOAK_TOKEN_REQUEST_FAILED);
                            }))
                    .body(KeycloakTokenResponse.class);
        } catch (ResourceAccessException exception) {
            log.warn("Keycloak token request failed: exception={}", exception.getClass().getSimpleName());
            throw new CustomException(UserErrorCode.KEYCLOAK_UNAVAILABLE);
        } catch (RestClientException exception) {
            log.warn("Keycloak token request failed: exception={}", exception.getClass().getSimpleName());
            throw new CustomException(UserErrorCode.KEYCLOAK_TOKEN_INVALID_RESPONSE);
        }

        if(response == null
                || !StringUtils.hasText(response.accessToken())
                || response.expiresIn() <= 0
                || !"Bearer".equalsIgnoreCase(response.tokenType())) {
            throw new CustomException(UserErrorCode.KEYCLOAK_TOKEN_INVALID_RESPONSE);
        }
        return response;
    }
}
