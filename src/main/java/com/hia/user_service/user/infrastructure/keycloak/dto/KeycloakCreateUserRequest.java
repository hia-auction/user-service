package com.hia.user_service.user.infrastructure.keycloak.dto;

import java.util.List;

public record KeycloakCreateUserRequest(
        String username,
        String email,
        boolean enabled,
        boolean emailVerified,
        List<KeycloakCredentialRequest> credentials
) {

    public static KeycloakCreateUserRequest of(
            String email,
            String password
    ) {
        return new KeycloakCreateUserRequest(
                email,
                email,
                true,
                false,
                List.of(new KeycloakCredentialRequest(
                        "password",
                        password,
                        false
                ))
        );
    }

    @Override
    public String toString() {
        return "KeycloakCreateUserRequest[values omitted]";
    }
}
