package com.hia.user_service.user.infrastructure.repository;

import com.hia.user_service.user.domain.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface JpaUserRepository extends JpaRepository<User, UUID> {

}
