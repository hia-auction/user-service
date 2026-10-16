package com.hia.user_service.common.api;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthenticationController {
//    인증 결과를 확인하는 HTTP API Controller


    @GetMapping("/me")
    public AuthenticationUserResponse me(
            @AuthenticationPrincipal Jwt jwt
            ) {
        return new AuthenticationUserResponse(
                jwt.getSubject(),
                jwt.getClaimAsString("email"),
                jwt.getClaimAsString("preferred_username")
        );
    }


    public record AuthenticationUserResponse(
            String subject,
            String email,
            String preferredUsername
    ) {

    }
}
