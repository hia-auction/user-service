package com.hia.user_service.user.presentation.dto;

import com.hia.user_service.user.application.dto.command.RegisterUserCommand;

public record RegisterUserRequest(
        String email,
        String password,
        String nickname
) {
    public RegisterUserCommand toCommand() {
        return new RegisterUserCommand(email, password, nickname);
    }

    @Override
    public String toString() {
        return "RegisterUserRequest[values omitted]";
    }
}
