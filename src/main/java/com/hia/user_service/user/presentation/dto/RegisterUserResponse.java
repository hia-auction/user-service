package com.hia.user_service.user.presentation.dto;

import com.hia.user_service.user.domain.entity.User;
import com.hia.user_service.user.domain.type.UserRole;
import com.hia.user_service.user.domain.type.UserStatus;

import java.util.UUID;

public record RegisterUserResponse(
        UUID id,
        String email,
        String nickname,
        UserRole role,
        UserStatus status
) {

    public static RegisterUserResponse from (User user) {
        return new RegisterUserResponse(
                user.getId(),
                user.getEmail(),
                user.getNickname(),
                user.getRole(),
                user.getStatus()
        );
    }
}
