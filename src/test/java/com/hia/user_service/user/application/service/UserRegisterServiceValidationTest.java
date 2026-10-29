package com.hia.user_service.user.application.service;

import com.hia.user_service.user.application.dto.command.RegisterUserCommand;
import com.hia.user_service.user.application.port.out.IdentityProvider;
import com.hia.user_service.user.domain.repository.UserRepository;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.validation.beanvalidation.MethodValidationPostProcessor;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verifyNoInteractions;

//테스트 컨테이너 실행
@Slf4j
@SpringJUnitConfig(UserRegisterServiceValidationTest.TestConfig.class)
public class UserRegisterServiceValidationTest {

    // 검증 기능이 연결된 실제 서비스를 받아오는 역할
    @Autowired
    private UserRegisterService service;

    //Spring에 가짜 의존성을 등록
    @MockitoBean
    private UserRepository repository;

    //Spring에 가짜 의존성을 등록
    @MockitoBean
    private IdentityProvider provider;

    //Spring에 가짜 의존성을 등록
    @MockitoBean
    private UserPersistenceService persistenceService;

    //@Bean 메서드끼리 직접 호출할 때 Spring이 그 호출을 가로채 관리할지 = false
    @Configuration(proxyBeanMethods = false)
    @Import(UserRegisterService.class)
    static class TestConfig {

        @Bean
        static MethodValidationPostProcessor methodValidationPostProcessor() {
            //@Validated 서비스에 메서드 검증 기능 연결
            return new MethodValidationPostProcessor();
        }
    }



    @Test
    void 이메일_형식이_잘못되면_서비스_본문을_실행하지_않는다() {
        RegisterUserCommand command = new RegisterUserCommand(
                "wrongEmail", "test-password", "nickname"
        );

        assertThrows(ConstraintViolationException.class,
                () -> service.register(command));

        verifyNoInteractions(repository, provider, persistenceService);
    }

    @Test
    void 비밀번호가_10자_미만이면_서비스_본문을_실행하지_않는다() {
        RegisterUserCommand command = new RegisterUserCommand(
                "testEmail@example.com", "invalid" , "nickname"
        );

        ConstraintViolationException exception = assertThrows(ConstraintViolationException.class,
                () -> service.register(command));
//        log.warn(exception.toString());

        verifyNoInteractions(repository, provider, persistenceService);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {
            " ",
            "nick name",
            "nick!",
            "abcdefghijklmnopqrstu"
    })
    void 닉네임이_유효하지_않으면_서비스_본문을_실행하지_않는다(String nickname) {
        RegisterUserCommand command = new RegisterUserCommand(
                "test@example.com", "test-password", nickname
        );

        assertThrows(ConstraintViolationException.class,
                () -> service.register(command));

        verifyNoInteractions(repository, provider, persistenceService);
    }

}
