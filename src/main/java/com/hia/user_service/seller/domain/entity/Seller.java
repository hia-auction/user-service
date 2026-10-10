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
@Table(name = "seller_profiles")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Seller extends BaseTime {

    @Id
    @Column(nullable = false, updatable = false)
    private UUID id;

    @Column(name = "user_id", nullable = false, updatable = false, unique = true)
    private UUID userId;
    //todo: 객체 연관관계를 사용하지 않고 UUID를 참조 추후 생성책임을 명확히 하면서 객체 참조로 변경

    @Column(name = "artist_name", nullable = false, length = 100)
    private String artistName;
    //todo: 빈 문자열을 통과 가능 도메인에서 검증할지 또는 DTO에서 검증할지 이후 정책에서 결정

    @Column(name = "business_number", length = 20)
    private String businessNumber;

    public static Seller create(
            UUID userId,
            String artistName,
            String businessNumber
    ) {

        Seller seller = new Seller();

        seller.id = UUID.randomUUID();
        seller.userId = userId;
        seller.artistName = artistName;
        seller.businessNumber = businessNumber;

        return seller;
    }
}
