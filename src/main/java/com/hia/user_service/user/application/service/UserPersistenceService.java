package com.hia.user_service.user.application.service;

import com.hia.user_service.user.domain.entity.User;
import com.hia.user_service.user.domain.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserPersistenceService {

    private final UserRepository userRepository;

    @Transactional
    public User saveUser(String authSubject, String email, String nickname) {
        User user = User.create(authSubject, email, nickname);
        return userRepository.save(user);
    }
}
