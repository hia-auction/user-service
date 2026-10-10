package com.hia.user_service.seller.infrastructure.repository;

import com.hia.user_service.seller.domain.entity.SellerAccount;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface JpaSellerAccountRepository extends JpaRepository<SellerAccount, UUID> {
}
