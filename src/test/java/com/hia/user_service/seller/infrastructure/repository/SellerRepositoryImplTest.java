package com.hia.user_service.seller.infrastructure.repository;

import com.hia.user_service.seller.domain.entity.Seller;
import com.hia.user_service.seller.domain.entity.SellerAccount;
import com.hia.user_service.seller.domain.repository.SellerRepository;
import com.hia.user_service.user.domain.entity.User;
import com.hia.user_service.user.domain.repository.UserRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class SellerRepositoryImplTest {

    @Autowired
    private SellerRepository sellerRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EntityManager entityManager;

    @Test
    void 판매자를_저장할_수_있다() {

        Seller seller = Seller.create(
                UUID.randomUUID(),
                "홍길동",
                "1234-5678"
        );

        Seller savedSeller = sellerRepository.save(seller);

        assertThat(savedSeller).isNotNull();
        assertThat(savedSeller.getUserId()).isNotNull();
        assertThat(savedSeller.getCreatedAt()).isNotNull();
        assertThat(savedSeller.getUpdatedAt()).isNotNull();
    }

    @Test
    void 판매자_은행정보를_저장할_수_있다() {

        SellerAccount sellerAccount = SellerAccount.create(
                UUID.randomUUID(),
                "503666-34999-12",
                "국민은행",
                "홍길동"
        );

        SellerAccount savedAccount = sellerRepository.save(sellerAccount);

        assertThat(savedAccount).isNotNull();
        assertThat(savedAccount.getSellerId()).isNotNull();
        assertThat(savedAccount.getCreatedAt()).isNotNull();
        assertThat(savedAccount.getUpdatedAt()).isNotNull();
    }

    @Test
    void user_seller_sellerAccount_통합_test() {

        User user = User.create(
                "keycloak-test-sub",
                "seller@example.com",
                "tester"
        );

        User savedUser = userRepository.save(user);

        Seller seller = Seller.create(
                savedUser.getId(),
                "최고의_작가",
                "123-456"
        );

        Seller savedSeller = sellerRepository.save(seller);

        SellerAccount sellerAccount = SellerAccount.create(
                savedSeller.getId(),
                "503666-34999-12",
                "국민은행",
                "tester"
        );

        SellerAccount savedAccount = sellerRepository.save(sellerAccount);

        entityManager.flush();

        assertThat(savedSeller.getUserId())
                .isEqualTo(savedUser.getId());

        assertThat(savedAccount.getSellerId())
                .isEqualTo(savedSeller.getId());

        assertThat(savedSeller.getCreatedAt()).isNotNull();
        assertThat(savedAccount.getCreatedAt()).isNotNull();
    }
}