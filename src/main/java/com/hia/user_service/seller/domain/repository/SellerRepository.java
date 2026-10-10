package com.hia.user_service.seller.domain.repository;

import com.hia.user_service.seller.domain.entity.Seller;
import com.hia.user_service.seller.domain.entity.SellerAccount;

public interface SellerRepository {

    Seller save(Seller seller);

    SellerAccount save(SellerAccount sellerAccount);
    //todo: 추후 판매자 프로필과 계좌 각각의 조회,수정 기능이 많아지면 Repository인터페이스를 분리하는 방안도 고려 가능
}
