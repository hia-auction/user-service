package com.hia.user_service.common.security;

import com.hia.user_service.common.api.AuthenticationController;
import com.hia.user_service.common.config.SecurityConfig;
import com.nimbusds.jose.JOSEObjectType;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimValidator;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.test.web.servlet.MockMvc;

import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.time.Instant;
import java.util.Date;
import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
        controllers = AuthenticationController.class,
        properties = {
                "spring.security.oauth2.resourceserver.jwt.issuer-uri=http://test-issuer",
                "spring.security.oauth2.resourceserver.jwt.audiences[0]=hia-api"
        }
)
@Import({
        SecurityConfig.class,
        JwtResourceServerIntegrationTest.TestJwtDecoderConfig.class
})
class JwtResourceServerIntegrationTest {

    private static final String SUBJECT = "keycloak-test-sub";
    private static final String ISSUER = "http://test-issuer";
    private static final String AUDIENCE = "hia-api";

    private static final KeyPair TRUSTED_KEY_PAIR =
            createRsaKeyPair();

    private static final KeyPair UNTRUSTED_KEY_PAIR =
            createRsaKeyPair();

    @Autowired
    private MockMvc mockMvc;

    @Test
    void 정상_JWT는_보호_API를_호출할_수_있다()
            throws Exception {

        Instant now = Instant.now();

        String token = createSignedToken(
                TRUSTED_KEY_PAIR,
                ISSUER,
                List.of(AUDIENCE, "account"),
                now.minusSeconds(10),
                now.plusSeconds(300)
        );

        mockMvc.perform(
                        get("/api/auth/me")
                                .header(
                                        HttpHeaders.AUTHORIZATION,
                                        "Bearer " + token
                                )
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.subject")
                        .value(SUBJECT))
                .andExpect(jsonPath("$.email")
                        .value("test@example.com"))
                .andExpect(jsonPath("$.preferredUsername")
                        .value("tester"));
    }

    @Test
    void 잘못된_서명의_JWT는_401을_반환한다()
            throws Exception {

        Instant now = Instant.now();

        String token = createSignedToken(
                UNTRUSTED_KEY_PAIR,
                ISSUER,
                List.of(AUDIENCE),
                now.minusSeconds(10),
                now.plusSeconds(300)
        );

        mockMvc.perform(
                        get("/api/auth/me")
                                .header(
                                        HttpHeaders.AUTHORIZATION,
                                        "Bearer " + token
                                )
                )
                .andExpect(status().isUnauthorized());
    }

    @Test
    void 형식이_잘못된_JWT는_401을_반환한다()
            throws Exception {

        mockMvc.perform(
                        get("/api/auth/me")
                                .header(
                                        HttpHeaders.AUTHORIZATION,
                                        "Bearer not-a-jwt"
                                )
                )
                .andExpect(status().isUnauthorized());
    }

    @Test
    void 만료된_JWT는_401을_반환한다()
            throws Exception {

        Instant now = Instant.now();

        String token = createSignedToken(
                TRUSTED_KEY_PAIR,
                ISSUER,
                List.of(AUDIENCE),
                now.minusSeconds(420),
                now.minusSeconds(120)
        );

        mockMvc.perform(
                        get("/api/auth/me")
                                .header(
                                        HttpHeaders.AUTHORIZATION,
                                        "Bearer " + token
                                )
                )
                .andExpect(status().isUnauthorized());
    }

    @Test
    void 다른_issuer의_JWT는_401을_반환한다()
            throws Exception {

        Instant now = Instant.now();

        String token = createSignedToken(
                TRUSTED_KEY_PAIR,
                "http://other-issuer",
                List.of(AUDIENCE),
                now.minusSeconds(10),
                now.plusSeconds(300)
        );

        mockMvc.perform(
                        get("/api/auth/me")
                                .header(
                                        HttpHeaders.AUTHORIZATION,
                                        "Bearer " + token
                                )
                )
                .andExpect(status().isUnauthorized());
    }

    @Test
    void hia_api_Audience가_없는_JWT는_401을_반환한다()
            throws Exception {

        Instant now = Instant.now();

        String token = createSignedToken(
                TRUSTED_KEY_PAIR,
                ISSUER,
                List.of("account"),
                now.minusSeconds(10),
                now.plusSeconds(300)
        );

        mockMvc.perform(
                        get("/api/auth/me")
                                .header(
                                        HttpHeaders.AUTHORIZATION,
                                        "Bearer " + token
                                )
                )
                .andExpect(status().isUnauthorized());
    }

    private static String createSignedToken(
            KeyPair signingKeyPair,
            String issuer,
            List<String> audiences,
            Instant issuedAt,
            Instant expiresAt
    ) throws Exception {

        JWTClaimsSet claims = new JWTClaimsSet.Builder()
                .subject(SUBJECT)
                .issuer(issuer)
                .audience(audiences)
                .issueTime(Date.from(issuedAt))
                .notBeforeTime(Date.from(issuedAt))
                .expirationTime(Date.from(expiresAt))
                .claim("email", "test@example.com")
                .claim("preferred_username", "tester")
                .build();

        SignedJWT signedJwt = new SignedJWT(
                new JWSHeader.Builder(JWSAlgorithm.RS256)
                        .type(JOSEObjectType.JWT)
                        .build(),
                claims
        );

        signedJwt.sign(
                new RSASSASigner(
                        (RSAPrivateKey) signingKeyPair.getPrivate()
                )
        );

        return signedJwt.serialize();
    }

    private static KeyPair createRsaKeyPair() {
        try {
            KeyPairGenerator generator =
                    KeyPairGenerator.getInstance("RSA");

            generator.initialize(2048);

            return generator.generateKeyPair();
        } catch (GeneralSecurityException exception) {
            throw new IllegalStateException(
                    "테스트 RSA 키 생성에 실패했습니다.",
                    exception
            );
        }
    }

    @TestConfiguration
    static class TestJwtDecoderConfig {

        @Bean
        JwtDecoder jwtDecoder() {

            NimbusJwtDecoder decoder = NimbusJwtDecoder
                    .withPublicKey(
                            (RSAPublicKey) TRUSTED_KEY_PAIR.getPublic()
                    )
                    .build();

            OAuth2TokenValidator<Jwt> issuerAndTimestampValidator =
                    JwtValidators.createDefaultWithIssuer(ISSUER);

            OAuth2TokenValidator<Jwt> audienceValidator =
                    new JwtClaimValidator<List<String>>(
                            "aud",
                            audiences -> audiences != null
                                    && audiences.contains(AUDIENCE)
                    );

            decoder.setJwtValidator(
                    new DelegatingOAuth2TokenValidator<>(
                            issuerAndTimestampValidator,
                            audienceValidator
                    )
            );

            return decoder;
        }
    }
}