package com.hia.user_service.user.application.port.out;

public interface IdentityProvider {

    String createUser(String email, String password);
    //반환형은 String입니다. 이후 이 값을 DB User의 authSubject로 저장하며, 서비스 내부의 User.id와는 구분합니다.
}
