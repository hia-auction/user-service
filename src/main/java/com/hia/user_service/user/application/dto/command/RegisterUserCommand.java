package com.hia.user_service.user.application.dto.command;

import jakarta.validation.constraints.*;

public record RegisterUserCommand(
        @NotBlank(message = "이메일은 필수 입니다.")
        @Email(message = "이메일 형식이 올바르지 않습니다.")
        @Size(max = 255, message = "이메일은 최대 255자 입니다.")
        String email,
        @NotNull
        @Size(min = 10, max = 128)
        String password,
        @NotBlank
        @Size(min = 1, max = 20)
        @Pattern(
                regexp = "^[가-힣a-zA-Z0-9]+$",
                message = "닉네임은 한글 완성형, 영문, 숫자만 사용할 수 있습니다."
        )
        String nickname
) {

    @Override
    public String toString() {
        return "RegisterUserCommand[values omitted]";
    }
}
