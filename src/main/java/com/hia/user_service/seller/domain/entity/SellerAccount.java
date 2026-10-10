package com.hia.user_service.seller.domain.entity;

import com.hia.common.domain.BaseTime;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Getter
@Entity
@Table(name = "seller_accounts")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SellerAccount extends BaseTime {

    @Id
    @Column(nullable = false, updatable = false)
    private UUID id;

    @Column(name = "seller_id", nullable = false, updatable = false, unique = true)
    private UUID sellerId;

    @Column(name = "account_number", nullable = false, length = 50)
    private String accountNumber;

    @Column(name = "bank_name", nullable = false, length = 50)
    private String bankName;

    @Column(name = "account_holder", nullable = false, length = 50)
    private String accountHolder;

    public static SellerAccount create(
            UUID sellerId,
            String accountNumber,
            String bankName,
            String accountHolder
    ) {
        SellerAccount sellerAccount = new SellerAccount();

        sellerAccount.id = UUID.randomUUID();
        sellerAccount.sellerId = sellerId;
        sellerAccount.accountNumber = accountNumber;
        sellerAccount.bankName = bankName;
        sellerAccount.accountHolder = accountHolder;

        return sellerAccount;
    }
}
