package com.hia.user_service.user.application.exception;

import com.hia.common.exception.ErrorCode;
import org.springframework.http.HttpStatus;

public enum UserErrorCode implements ErrorCode {

    USER_NOT_FOUND(
            /*상태코드 404*/
            HttpStatus.NOT_FOUND,
            "USER_NOT_FOUND",
            "사용자를 찾을 수 없습니다."
    ),

    USER_ALREADY_EXISTS(
            /*상태코드 409*/
            HttpStatus.CONFLICT,
            "USER_ALREADY_EXISTS",
            "이미 존재하는 계정입니다."
    ),

    KEYCLOAK_TOKEN_INVALID_RESPONSE(
            /*상태코드 502*/
            HttpStatus.BAD_GATEWAY,
            "KEYCLOAK_TOKEN_INVALID_RESPONSE",
            "인증 서버에서 올바른 토큰 응답을 받지 못했습니다."
    ),

    KEYCLOAK_TOKEN_REQUEST_FAILED(
            /*상태코드 502*/
            HttpStatus.BAD_GATEWAY,
            "KEYCLOAK_TOKEN_REQUEST_FAILED",
            "인증 서버의 토큰 발급 요청에 실패했습니다."
    ),

    KEYCLOAK_USER_INVALID_RESPONSE(
            /*상태코드 502*/
            HttpStatus.BAD_GATEWAY,
            "KEYCLOAK_USER_INVALID_RESPONSE",
            "인증 서버에서 올바른 계정 생성 응답을 받지 못했습니다."
    ),

    KEYCLOAK_USER_CREATION_FAILED(
            /*상태코드 502*/
            HttpStatus.BAD_GATEWAY,
            "KEYCLOAK_USER_CREATION_FAILED",
            "인증 서버의 계정 생성 요청에 실패했습니다."
    ),

    KEYCLOAK_UNAVAILABLE(
            /*상태코드 503*/
            HttpStatus.SERVICE_UNAVAILABLE,
            "KEYCLOAK_UNAVAILABLE",
            "인증 서버와 통신할 수 없습니다."
    );

    private final HttpStatus status;
    private final String code;
    private final String message;

    UserErrorCode(
            HttpStatus status,
            String code,
            String message
    ) {
        this.status = status;
        this.code = code;
        this.message = message;
    }

    @Override
    public HttpStatus getStatus() {
        return status;
    }

    @Override
    public String getCode() {
        return code;
    }

    @Override
    public String getMessage() {
        return message;
    }
}
