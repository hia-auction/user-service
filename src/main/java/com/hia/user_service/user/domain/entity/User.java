package com.hia.user_service.user.domain.entity;



import com.hia.common.domain.BaseTime;
import com.hia.user_service.user.domain.type.UserRole;
import com.hia.user_service.user.domain.type.UserStatus;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Getter
@Entity
@Table(name = "users")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User extends BaseTime {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "auth_subject", nullable = false, updatable = false, unique = true)
    private String authSubject;

    @Column(name = "email", nullable = false, unique = true)
    private String email;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false)
    private UserRole role;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private UserStatus status;

    @Column(name = "name", nullable = false, length = 100)
    private String name;


    //생성 로직
    public static User create(
            String authSubject,
            String email,
            String name
    ) {
        User user = new User();

        user.id = UUID.randomUUID();
        user.authSubject = authSubject;
        user.email = email;
        user.name = name;
        user.role = UserRole.BUYER;
        user.status = UserStatus.ACTIVE;

        return user;
    }

    // 상태 변경 로직
}
