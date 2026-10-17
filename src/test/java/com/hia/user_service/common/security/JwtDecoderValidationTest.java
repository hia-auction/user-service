package com.hia.user_service.common.security;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.time.Instant;
import java.util.Date;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;

public class JwtDecoderValidationTest {

    private KeyPair trustedKeyPair;
    private KeyPair untrustedKeyPair;

//    현재 첫 테스트는 주로 서명을 검증하지만, 실제 Access Token과 비슷한 JWT를 만들기 위해 issuer와 Audience도 넣어두겠습니다.
    private static final String SUBJECT = "keycloak-test-sub";
    private static final String ISSUER = "http://localhost:8180/realms/hia";
    private static final String AUDIENCE = "hia-api";

    private NimbusJwtDecoder jwtDecoder;

    @BeforeEach
    void setUp() throws Exception {

        trustedKeyPair = createRSAKeyPair();
        untrustedKeyPair = createRSAKeyPair();

        jwtDecoder = NimbusJwtDecoder
                .withPublicKey(
                        (RSAPublicKey) trustedKeyPair.getPublic()
                ).build();
    }

    private KeyPair createRSAKeyPair() throws Exception {
        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");

        keyPairGenerator.initialize(2048);

        return keyPairGenerator.generateKeyPair();
    }

    private String createSignedToken(
            RSAPrivateKey privateKey
    ) throws Exception {

        JWTClaimsSet claims = createClaims();

        SignedJWT signedJwt = new SignedJWT(
                new JWSHeader.Builder(JWSAlgorithm.RS256)
                        .build(),
                claims
        );

        signedJwt.sign(new RSASSASigner(privateKey));

        return signedJwt.serialize();
    }

    private JWTClaimsSet createClaims() {
        Instant now = Instant.now();

        return new JWTClaimsSet.Builder()
                .subject(SUBJECT)
                .issuer(ISSUER)
                .audience(AUDIENCE)
                .issueTime(Date.from(now))
                .notBeforeTime(Date.from(now.minusSeconds(1)))
                .expirationTime(Date.from(now.plusSeconds(300)))
                .build();
    }

    @Test
    void 신뢰하는_개인키로_서명한_JWT는_검증에_성공한다()
            throws Exception {

        String token = createSignedToken(
                (RSAPrivateKey) trustedKeyPair.getPrivate()
        );

        Jwt decodedJwt = jwtDecoder.decode(token);

        assertThat(decodedJwt.getSubject())
                .isEqualTo(SUBJECT);
    }

    @Test
    void 다른_개인키로_서명한_JWT는_검증에_실패한다()
            throws Exception {

        String token = createSignedToken(
                (RSAPrivateKey) untrustedKeyPair.getPrivate()
        );

        assertThatThrownBy(() -> jwtDecoder.decode(token))
                .isInstanceOf(JwtException.class);
    }
}
