package com.hia.user_service.user.presentation.dto;

import com.hia.user_service.user.domain.entity.User;
import com.hia.user_service.user.domain.type.UserRole;
import com.hia.user_service.user.domain.type.UserStatus;

import java.util.UUID;

public record CurrentUserResponse(
        UUID id,
        String email,
        String name,
        UserRole role,
        UserStatus status
) {

    public static CurrentUserResponse from(User user) {
        return new CurrentUserResponse(
                user.getId(),
                user.getEmail(),
                user.getName(),
                user.getRole(),
                user.getStatus()
        );
    }
}
