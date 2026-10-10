package com.hia.user_service.seller.infrastructure.repository;

import com.hia.user_service.seller.domain.entity.Seller;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface JpaSellerRepository extends JpaRepository<Seller, UUID> {
}
