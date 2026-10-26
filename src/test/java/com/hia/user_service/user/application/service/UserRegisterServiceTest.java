package com.hia.user_service.user.application.service;

import com.hia.common.exception.CustomException;
import com.hia.user_service.user.application.dto.command.RegisterUserCommand;
import com.hia.user_service.user.application.exception.UserErrorCode;
import com.hia.user_service.user.application.port.out.IdentityProvider;
import com.hia.user_service.user.domain.entity.User;
import com.hia.user_service.user.domain.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@Slf4j
class UserRegisterServiceTest {

    //0. 가짜 의존성 객체 준비
    UserRepository repository = mock(UserRepository.class);
    IdentityProvider provider = mock(IdentityProvider.class);
    UserPersistenceService persistenceService = mock(UserPersistenceService.class);
    UserRegisterService service = new UserRegisterService(repository, provider, persistenceService);

    // 0. 입력, 저장 결과 준비
    RegisterUserCommand command = new RegisterUserCommand(
            "buyer@example.com", "test-password", "경매초보");

    String authSubject = "created-keycloak-id";

    User savedUser = User.create(authSubject, command.email(), command.nickname());

    @Test
    void 정상_가입이면_생성된_인증식별자로_저장하고_회원을_반환한다() {
        when(repository.existsByEmail(command.email())).thenReturn(false);
        when(repository.existsByNickname(command.nickname())).thenReturn(false);
        when(provider.createUser(command.email(), command.password())).thenReturn(authSubject);
        when(persistenceService.saveUser(authSubject, command.email(), command.nickname()))
                .thenReturn(savedUser);

        User result = service.register(command);

        assertSame(savedUser, result);

        verify(repository).existsByEmail(command.email());
        verify(repository).existsByNickname(command.nickname());
        verify(provider).createUser(command.email(), command.password());
        verify(persistenceService).saveUser(authSubject, command.email(), command.nickname());
        verify(provider, never()).deleteUser(anyString());
    }

    @Test
    void DB_저장_실패시_생성한_계정을_삭제하고_원인을_보존한다() {
        when(repository.existsByEmail(command.email())).thenReturn(false);
        when(repository.existsByNickname(command.nickname())).thenReturn(false);
        when(provider.createUser(command.email(), command.password())).thenReturn(authSubject);

        DataIntegrityViolationException dbException =
                new DataIntegrityViolationException("테스트용 무결성 위반");

        when(persistenceService.saveUser(authSubject, command.email(), command.nickname()))
                .thenThrow(dbException);

        CustomException exception = assertThrows(
                CustomException.class,
                () -> service.register(command));

        assertEquals(UserErrorCode.USER_DATABASE_ERROR, exception.getErrorCode());
        assertSame(dbException, exception.getCause());

        verify(provider).deleteUser(authSubject);
    }

    @Test
    void DB_저장과_보상삭제가_실패하면_두_예외를_보존한다() {
        when(repository.existsByEmail(command.email())).thenReturn(false);
        when(repository.existsByNickname(command.nickname())).thenReturn(false);
        when(provider.createUser(command.email(), command.password())).thenReturn(authSubject);

        DataIntegrityViolationException dbException =
                new DataIntegrityViolationException("테스트용 DB 실패");

        CustomException deletionException =
                new CustomException(UserErrorCode.KEYCLOAK_UNAVAILABLE);

        when(persistenceService.saveUser(authSubject, command.email(), command.nickname()))
                .thenThrow(dbException);
        doThrow(deletionException).when(provider).deleteUser(authSubject);

        CustomException exception = assertThrows(
                CustomException.class,
                () -> service.register(command));

        assertEquals(UserErrorCode.USER_DATABASE_ERROR, exception.getErrorCode());
        assertSame(dbException, exception.getCause());

        Throwable[] suppressed = exception.getCause().getSuppressed();

        assertEquals(1, suppressed.length);
        assertSame(deletionException, suppressed[0]);

        verify(provider).deleteUser(authSubject);
    }

    @Test
    void Keycloak_생성에_실패하면_저장과_삭제를_실행하지_않는다() {

        when(repository.existsByEmail(command.email())).thenReturn(false);
        when(repository.existsByNickname(command.nickname())).thenReturn(false);

        CustomException creationException = new CustomException(UserErrorCode.KEYCLOAK_UNAVAILABLE);
        when(provider.createUser(command.email(), command.password())).thenThrow(creationException);

        CustomException exception = assertThrows(
                CustomException.class, () -> service.register(command)
        );

        assertSame(creationException, exception);
        verifyNoInteractions(persistenceService);
        verify(provider, never()).deleteUser(anyString());
        verify(provider).createUser(command.email(), command.password());
    }

    @Test
    void 이메일이_중복이면_계정생성과_저장을_실행하지_않는다() {
        when(repository.existsByEmail(command.email())).thenReturn(true);

        CustomException exception = assertThrows(CustomException.class,
                () -> service.register(command));

        assertEquals(UserErrorCode.USER_EMAIL_EXISTS, exception.getErrorCode());
        verify(provider,never()).createUser(anyString(),anyString());
        verifyNoInteractions(persistenceService);
    }

    @Test
    void 닉네임이_중복이면_계정생성과_저장을_실행하지_않는다() {
        when(repository.existsByEmail(command.email())).thenReturn(false);
        when(repository.existsByNickname(command.nickname())).thenReturn(true);
        when(provider.createUser(command.email(), command.password())).thenReturn(authSubject);

        CustomException exception = assertThrows(CustomException.class,
                () -> service.register(command));

        assertEquals(UserErrorCode.USER_NICKNAME_EXISTS, exception.getErrorCode());
        verifyNoInteractions(provider, persistenceService);
    }
}