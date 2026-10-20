package com.hia.user_service.user.application.service;

import com.hia.common.exception.CustomException;
import com.hia.user_service.user.application.dto.query.GetCurrentUserQuery;
import com.hia.user_service.user.application.exception.UserErrorCode;
import com.hia.user_service.user.domain.entity.User;
import com.hia.user_service.user.domain.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public User getCurrentUser(GetCurrentUserQuery query) {
        return userRepository.findByAuthSubject(query.authSubject())
                .orElseThrow(() ->
                        new CustomException(UserErrorCode.USER_NOT_FOUND));
    }
}
