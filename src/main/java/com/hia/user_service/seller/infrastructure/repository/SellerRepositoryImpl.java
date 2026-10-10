package com.hia.user_service.seller.infrastructure.repository;

import com.hia.user_service.seller.domain.entity.Seller;
import com.hia.user_service.seller.domain.entity.SellerAccount;
import com.hia.user_service.seller.domain.repository.SellerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class SellerRepositoryImpl implements SellerRepository {

    private final JpaSellerRepository jpaSellerRepository;
    private final JpaSellerAccountRepository jpaSellerAccountRepository;

    @Override
    public Seller save(Seller seller) {
        return jpaSellerRepository.save(seller);
    }

    @Override
    public SellerAccount save(SellerAccount sellerAccount) {
        return jpaSellerAccountRepository.save(sellerAccount);
    }
}
