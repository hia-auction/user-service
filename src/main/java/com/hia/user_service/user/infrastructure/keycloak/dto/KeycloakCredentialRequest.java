package com.hia.user_service.user.infrastructure.keycloak.dto;

public record KeycloakCredentialRequest(
        String type,
        String value,
        boolean temporary
) {

    @Override
    public String toString() {
        return "KeycloakCredentialRequest[values omitted]";
    }
}
