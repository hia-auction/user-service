package com.hia.user_service.user.application.service;

import com.hia.common.exception.CustomException;
import com.hia.user_service.user.application.dto.command.RegisterUserCommand;
import com.hia.user_service.user.application.exception.UserErrorCode;
import com.hia.user_service.user.application.port.out.IdentityProvider;
import com.hia.user_service.user.domain.entity.User;
import com.hia.user_service.user.domain.repository.UserRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

@Service
@RequiredArgsConstructor
@Validated
@Slf4j
public class UserRegisterService {

    private final UserRepository userRepository;
    private final IdentityProvider provider;
    private final UserPersistenceService userPersistenceService;

    public User register(@NotNull @Valid RegisterUserCommand command) {

        if(userRepository.existsByEmail(command.email())) {
            throw new CustomException(UserErrorCode.USER_EMAIL_EXISTS);
        } else if (userRepository.existsByNickname(command.nickname())) {
            throw new CustomException(UserErrorCode.USER_NICKNAME_EXISTS);
        }

        String authSubject = provider.createUser(command.email(), command.password());
        try {
            return userPersistenceService.saveUser(authSubject, command.email(), command.nickname());
        }catch (DataIntegrityViolationException exception) {
            log.warn("회원가입 DB 저장 실패: type={}",
                    exception.getClass().getSimpleName());
            try {
                provider.deleteUser(authSubject);
                log.info("회원가입 보상 삭제 완료");
            } catch (RuntimeException compensationException) {
                exception.addSuppressed(compensationException);
                log.warn("회원가입 보상 삭제 실패: type={}",
                        compensationException.getClass().getSimpleName());
            }
            CustomException registrationException = new CustomException(UserErrorCode.USER_DATABASE_ERROR);
            registrationException.initCause(exception);
            throw registrationException;
        }
    }
}
