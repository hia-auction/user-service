package com.hia.user_service.user.presentation.controller;

import com.hia.common.response.ApiResponse;
import com.hia.user_service.user.application.dto.query.GetCurrentUserQuery;
import com.hia.user_service.user.application.service.UserService;
import com.hia.user_service.user.domain.entity.User;
import com.hia.user_service.user.presentation.dto.CurrentUserResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping("/me")
    public ApiResponse<CurrentUserResponse> getCurrentUser(@AuthenticationPrincipal Jwt jwt) {

        GetCurrentUserQuery query = new GetCurrentUserQuery(jwt.getSubject());
        User user = userService.getCurrentUser(query);
        return ApiResponse.success(CurrentUserResponse.from(user));
    }
}
