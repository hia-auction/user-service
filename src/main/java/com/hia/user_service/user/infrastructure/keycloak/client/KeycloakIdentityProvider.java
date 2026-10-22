package com.hia.user_service.user.infrastructure.keycloak.client;

import com.hia.common.exception.CustomException;
import com.hia.user_service.user.application.exception.UserErrorCode;
import com.hia.user_service.user.application.port.out.IdentityProvider;
import com.hia.user_service.user.infrastructure.keycloak.config.KeycloakAdminProperties;
import com.hia.user_service.user.infrastructure.keycloak.dto.KeycloakCreateUserRequest;
import com.hia.user_service.user.infrastructure.keycloak.dto.KeycloakTokenResponse;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

import java.net.URI;

@Component
public class KeycloakIdentityProvider implements IdentityProvider {

    private final RestClient restClient;
    private final KeycloakTokenClient tokenClient;
    private final KeycloakAdminProperties properties;


    public KeycloakIdentityProvider(
            @Qualifier("keycloakRestClient") RestClient restClient,
            KeycloakTokenClient tokenClient,
            KeycloakAdminProperties properties
    ) {
        this.restClient = restClient;
        this.tokenClient = tokenClient;
        this.properties = properties;
    }

    @Override
    public String createUser(String email, String password) {

        KeycloakTokenResponse token = tokenClient.issueToken();

        KeycloakCreateUserRequest request = KeycloakCreateUserRequest.of(email, password);

        ResponseEntity<Void> response = restClient.post()
                .uri("/admin/realms/{realm}/users", properties.getRealm())
                .headers(headers ->
                        headers.setBearerAuth(token.accessToken()))
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .retrieve()
                .toBodilessEntity();

        if(response.getStatusCode().value() != 201) {
            throw new CustomException(UserErrorCode.KEYCLOAK_USER_INVALID_RESPONSE);
        }

        URI location = response.getHeaders().getLocation();

        if(location == null) {
            throw new CustomException(UserErrorCode.KEYCLOAK_USER_INVALID_RESPONSE);
        }

        String path = location.getPath();

        if(!StringUtils.hasText(path) || path.endsWith("/")) {
            throw new CustomException(UserErrorCode.KEYCLOAK_USER_INVALID_RESPONSE);
        }

        return path.substring(path.lastIndexOf('/')+1);
    }
}
