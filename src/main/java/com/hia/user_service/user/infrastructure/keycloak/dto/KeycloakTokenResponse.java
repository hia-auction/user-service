package com.hia.user_service.user.infrastructure.keycloak.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record KeycloakTokenResponse(
        @JsonProperty("access_token") String accessToken,
        @JsonProperty("expires_in") long expiresIn,
        @JsonProperty("token_type") String tokenType
) {

    @Override
    public String toString() {
        return "KeycloakTokenResponse[values omitted]";
    }
}
