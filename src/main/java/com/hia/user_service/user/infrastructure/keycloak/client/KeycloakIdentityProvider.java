package com.hia.user_service.user.infrastructure.keycloak.client;

import com.hia.common.exception.CustomException;
import com.hia.user_service.user.application.exception.UserErrorCode;
import com.hia.user_service.user.application.port.out.IdentityProvider;
import com.hia.user_service.user.infrastructure.keycloak.config.KeycloakAdminProperties;
import com.hia.user_service.user.infrastructure.keycloak.dto.KeycloakCreateUserRequest;
import com.hia.user_service.user.infrastructure.keycloak.dto.KeycloakTokenResponse;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

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

        ResponseEntity<Void> response;
        try {
            response = restClient.post()
                    .uri("/admin/realms/{realm}/users", properties.getRealm())
                    .headers(headers ->
                            headers.setBearerAuth(token.accessToken()))
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(request)
                    .retrieve()
                    .onStatus(status -> status.value() == 409,
                            (httpRequest, httpResponse) ->
                            {
                                throw new CustomException(UserErrorCode.USER_ALREADY_EXISTS);
                            })
                    .onStatus(HttpStatusCode::isError,
                            (httpRequest, httpResponse) ->
                            {
                                throw new CustomException(UserErrorCode.KEYCLOAK_USER_CREATION_FAILED);
                            })
                    .toBodilessEntity();
        } catch (ResourceAccessException exception) {
            throw new CustomException(UserErrorCode.KEYCLOAK_UNAVAILABLE);
        } catch (RestClientException exception) {
            throw new CustomException(UserErrorCode.KEYCLOAK_USER_CREATION_FAILED);
        }

        if(response.getStatusCode().value() != 201) {
            throw new CustomException(UserErrorCode.KEYCLOAK_USER_INVALID_RESPONSE);
        }

        URI location;

        try {
            location = response.getHeaders().getLocation();
        } catch (IllegalArgumentException exception) {
            throw new CustomException(UserErrorCode.KEYCLOAK_USER_INVALID_RESPONSE);
        }

        if(location == null) {
            throw new CustomException(UserErrorCode.KEYCLOAK_USER_INVALID_RESPONSE);
        }

        String path = location.getPath();

        String expectedPrefix = "/admin/realms/" + properties.getRealm() + "/users/";

        if(path == null || !path.startsWith(expectedPrefix)) {
            throw new CustomException(UserErrorCode.KEYCLOAK_USER_INVALID_RESPONSE);
        }

        String authSubject = path.substring(expectedPrefix.length());

        if(!StringUtils.hasText(authSubject)
                || authSubject.contains("/")
                || ".".equals(authSubject)
                || "..".equals(authSubject)) {
            throw new CustomException(UserErrorCode.KEYCLOAK_USER_INVALID_RESPONSE);
        }

        return authSubject;
    }
}
