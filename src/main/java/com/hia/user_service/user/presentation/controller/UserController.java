package com.hia.user_service.user.presentation.controller;

import com.hia.common.response.ApiResponse;
import com.hia.user_service.user.application.dto.query.GetCurrentUserQuery;
import com.hia.user_service.user.application.service.UserRegisterService;
import com.hia.user_service.user.application.service.UserService;
import com.hia.user_service.user.domain.entity.User;
import com.hia.user_service.user.presentation.dto.CurrentUserResponse;
import com.hia.user_service.user.presentation.dto.RegisterUserRequest;
import com.hia.user_service.user.presentation.dto.RegisterUserResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final UserRegisterService userRegisterService;

    @GetMapping("/me")
    public ApiResponse<CurrentUserResponse> getCurrentUser(@AuthenticationPrincipal Jwt jwt) {

        GetCurrentUserQuery query = new GetCurrentUserQuery(jwt.getSubject());
        User user = userService.getCurrentUser(query);
        return ApiResponse.success(CurrentUserResponse.from(user));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<RegisterUserResponse> register(@RequestBody RegisterUserRequest request) {
        User user = userRegisterService.register(request.toCommand());
        return ApiResponse.success("가입이 성공적으로 이루어졌습니다.",RegisterUserResponse.from(user));
    }
}
