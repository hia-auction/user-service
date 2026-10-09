package com.hia.user_service.user.infrastructure.repository;

import com.hia.user_service.user.domain.entity.User;
import com.hia.user_service.user.domain.repository.UserRepository;
import com.hia.user_service.user.domain.type.UserRole;
import com.hia.user_service.user.domain.type.UserStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.*;

@ActiveProfiles("test")
@SpringBootTest
@Transactional
class UserRepositoryImplTest {

    @Autowired
    private UserRepository userRepository;

    @Test
    void 사용자를_저장하고_조회할_수_있다() {

        User user = User.create(
                "keycloak-sub-123",
                "test@example.com",
                "tester"
        );

        User savedUser = userRepository.save(user);

        Optional<User> foundUser =
                userRepository.findById(savedUser.getId());

        assertThat(foundUser).isPresent();
        assertThat(foundUser.get().getEmail())
                .isEqualTo("test@example.com");

        assertThat(savedUser.getCreatedAt()).isNotNull();
        assertThat(savedUser.getUpdatedAt()).isNotNull();
    }

    @Test
    void 사용자를_생성하면_기본_역할과_상태가_설정된다() {

        User user = User.create(
                "sub-123",
                "test@example.com",
                "tester"
        );

        assertThat(user.getRole())
                .isEqualTo(UserRole.BUYER);

        assertThat(user.getStatus())
                .isEqualTo(UserStatus.ACTIVE);
    }
}