package com.hia.user_service.common;

import com.hia.common.exception.CustomException;
import com.hia.common.exception.ErrorCode;
import com.hia.common.response.ApiResponse;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/user")
public class CommonModuleTestController {

    @GetMapping("/test/success")
    public ApiResponse<String> test(){
        String data = "test";
        return ApiResponse.success(data);
    }

    @GetMapping("/test/error")
    public void errorTest(){
        ErrorCode errorCode = userErrorCode.TEST_ERROR;
        throw new CustomException(errorCode);
    }

    private enum userErrorCode implements ErrorCode{

        TEST_ERROR(
                HttpStatus.BAD_REQUEST,
                "USER_TEST_ERROR",
                "사용자를 찾을 수 없습니다."
        );

        private final HttpStatus status;
        private final String code;
        private final String message;

        userErrorCode(HttpStatus status, String code, String message){
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
}
