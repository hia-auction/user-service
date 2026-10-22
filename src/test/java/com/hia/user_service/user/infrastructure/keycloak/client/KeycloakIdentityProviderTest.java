package com.hia.user_service.user.infrastructure.keycloak.client;

import com.hia.common.exception.CustomException;
import com.hia.user_service.user.application.exception.UserErrorCode;
import com.hia.user_service.user.infrastructure.keycloak.config.KeycloakAdminProperties;
import com.hia.user_service.user.infrastructure.keycloak.dto.KeycloakTokenResponse;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.HttpHeaders;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.io.IOException;
import java.net.URI;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withCreatedEntity;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;


class KeycloakIdentityProviderTest {

    @Test
    void 계정을_생성하고_인증_식별자를_반환한다() {

        KeycloakAdminProperties properties = new KeycloakAdminProperties();
        properties.setBaseUrl("http://keycloak.test");
        properties.setRealm("hia");

        RestClient.Builder builder = RestClient.builder().baseUrl(properties.getBaseUrl());

        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();

        //토큰 발급 과정 생략
        KeycloakTokenClient tokenClient = mock(KeycloakTokenClient.class);
        when(tokenClient.issueToken()).thenReturn(
                new KeycloakTokenResponse(
                        "test-admin-access-token",
                        300,
                        "Bearer"
                )
        );

        KeycloakIdentityProvider provider = new KeycloakIdentityProvider(builder.build(), tokenClient, properties);

        server.expect(requestTo(properties.getBaseUrl() + "/admin/realms/" + properties.getRealm() + "/users"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer test-admin-access-token"))
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(content().json("""
                        {
                          "username": "tester@example.com",
                          "email": "tester@example.com",
                          "enabled": true,
                          "emailVerified": false,
                          "credentials": [
                            {
                              "type": "password",
                              "value": "test-password",
                              "temporary": false
                            }
                          ]
                        }
                        """))
                .andRespond(withCreatedEntity(URI.create("http://keycloak.test/admin/realms/hia/users/test-user-id")));

        String authSubject = provider.createUser(
                "tester@example.com",
                "test-password");

        assertEquals("test-user-id", authSubject);

        verify(tokenClient).issueToken();
        server.verify();
    }

    @Test
    void 중복_계정이면_USER_ALREADY_EXISTS_예외가_발생한다() {

        KeycloakAdminProperties properties = new KeycloakAdminProperties();
        properties.setBaseUrl("http://keycloak.test");
        properties.setRealm("hia");

        RestClient.Builder builder = RestClient.builder().baseUrl(properties.getBaseUrl());

        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();

        //토큰 발급 과정 생략
        KeycloakTokenClient tokenClient = mock(KeycloakTokenClient.class);
        when(tokenClient.issueToken()).thenReturn(
                new KeycloakTokenResponse(
                        "test-admin-access-token",
                        300,
                        "Bearer"
                )
        );

        KeycloakIdentityProvider provider = new KeycloakIdentityProvider(builder.build(), tokenClient, properties);

        server.expect(requestTo(properties.getBaseUrl() + "/admin/realms/" + properties.getRealm() + "/users"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withStatus(HttpStatus.CONFLICT));

        CustomException exception = assertThrows(
                CustomException.class,
                () -> provider.createUser(
                        "tester@example.com",
                        "test-password"
                )
        );

        assertEquals(UserErrorCode.USER_ALREADY_EXISTS, exception.getErrorCode());

        verify(tokenClient).issueToken();
        server.verify();
    }

    @Test
    void 인증_서버가_오류를_반환하면_KEYCLOAK_USER_CREATION_FAILED_예외가_발생한다() {

        KeycloakAdminProperties properties = new KeycloakAdminProperties();
        properties.setBaseUrl("http://keycloak.test");
        properties.setRealm("hia");

        RestClient.Builder builder = RestClient.builder().baseUrl(properties.getBaseUrl());

        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();

        //토큰 발급 과정 생략
        KeycloakTokenClient tokenClient = mock(KeycloakTokenClient.class);
        when(tokenClient.issueToken()).thenReturn(
                new KeycloakTokenResponse(
                        "test-admin-access-token",
                        300,
                        "Bearer"
                )
        );

        KeycloakIdentityProvider provider = new KeycloakIdentityProvider(builder.build(), tokenClient, properties);

        server.expect(requestTo(properties.getBaseUrl() + "/admin/realms/" + properties.getRealm() + "/users"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withStatus(HttpStatus.INTERNAL_SERVER_ERROR));

        CustomException exception = assertThrows(
                CustomException.class,
                () -> provider.createUser(
                        "tester@example.com",
                        "test-password"
                )
        );

        assertEquals(UserErrorCode.KEYCLOAK_USER_CREATION_FAILED, exception.getErrorCode());

        verify(tokenClient).issueToken();
        server.verify();
    }

    @Test
    void 계정_생성_중_통신에_실패하면_KEYCLOAK_UNAVAILABLE_예외가_발생한다() {

        KeycloakAdminProperties properties = new KeycloakAdminProperties();
        properties.setBaseUrl("http://keycloak.test");
        properties.setRealm("hia");

        RestClient.Builder builder = RestClient.builder().baseUrl(properties.getBaseUrl());

        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();

        //토큰 발급 과정 생략
        KeycloakTokenClient tokenClient = mock(KeycloakTokenClient.class);
        when(tokenClient.issueToken()).thenReturn(
                new KeycloakTokenResponse(
                        "test-admin-access-token",
                        300,
                        "Bearer"
                )
        );

        KeycloakIdentityProvider provider = new KeycloakIdentityProvider(builder.build(), tokenClient, properties);

        server.expect(requestTo(properties.getBaseUrl() + "/admin/realms/" + properties.getRealm() + "/users"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(request -> {
                    throw new IOException("테스트용 통신 실패");
                });
        // RestClient가 ResourceAccessException으로 감쌈 → 해당 catch에서 KEYCLOAK_UNAVAILABLE로 변환
        CustomException exception = assertThrows(
                CustomException.class,
                () -> provider.createUser(
                        "tester@example.com",
                        "test-password"
                )
        );

        assertEquals(UserErrorCode.KEYCLOAK_UNAVAILABLE, exception.getErrorCode());

        verify(tokenClient).issueToken();
        server.verify();
    }

    @Test
    void 생성응답에_location이_없으면_KEYCLOAK_USER_INVALID_RESPONSE_예외가_발생한다() {

        KeycloakAdminProperties properties = new KeycloakAdminProperties();
        properties.setBaseUrl("http://keycloak.test");
        properties.setRealm("hia");

        RestClient.Builder builder = RestClient.builder().baseUrl(properties.getBaseUrl());

        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();

        //토큰 발급 과정 생략
        KeycloakTokenClient tokenClient = mock(KeycloakTokenClient.class);
        when(tokenClient.issueToken()).thenReturn(
                new KeycloakTokenResponse(
                        "test-admin-access-token",
                        300,
                        "Bearer"
                )
        );

        KeycloakIdentityProvider provider = new KeycloakIdentityProvider(builder.build(), tokenClient, properties);

        server.expect(requestTo(properties.getBaseUrl() + "/admin/realms/" + properties.getRealm() + "/users"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withStatus(HttpStatus.CREATED));
        // withCreatedEntity(...)와 달리, 이 코드는 201 상태만 설정하고 Location은 추가하지 않습니다.

        CustomException exception = assertThrows(
                CustomException.class,
                () -> provider.createUser(
                        "tester@example.com",
                        "test-password"
                )
        );

        assertEquals(UserErrorCode.KEYCLOAK_USER_INVALID_RESPONSE, exception.getErrorCode());

        verify(tokenClient).issueToken();
        server.verify();
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "http://[",
            "http://keycloak.test/admin/realms/other/users/user-123",
            "http://keycloak.test/admin/realms/hia/users",
            "http://keycloak.test/admin/realms/hia/users/",
            "http://keycloak.test/admin/realms/hia/users/.",
            "http://keycloak.test/admin/realms/hia/users/..",
            "http://keycloak.test/admin/realms/hia/users/user-123/extra"
    })
    void Location이_잘못되면_응답검증_예외가_발생한다(String location) {

        KeycloakAdminProperties properties = new KeycloakAdminProperties();
        properties.setBaseUrl("http://keycloak.test");
        properties.setRealm("hia");

        RestClient.Builder builder = RestClient.builder().baseUrl(properties.getBaseUrl());

        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();

        //토큰 발급 과정 생략
        KeycloakTokenClient tokenClient = mock(KeycloakTokenClient.class);
        when(tokenClient.issueToken()).thenReturn(
                new KeycloakTokenResponse(
                        "test-admin-access-token",
                        300,
                        "Bearer"
                )
        );

        KeycloakIdentityProvider provider = new KeycloakIdentityProvider(builder.build(), tokenClient, properties);

        server.expect(requestTo(properties.getBaseUrl() + "/admin/realms/" + properties.getRealm() + "/users"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withStatus(HttpStatus.CREATED)
                        .header(HttpHeaders.LOCATION, location));

        CustomException exception = assertThrows(
                CustomException.class,
                () -> provider.createUser(
                        "tester@example.com",
                        "test-password"
                )
        );

        assertEquals(UserErrorCode.KEYCLOAK_USER_INVALID_RESPONSE, exception.getErrorCode());

        verify(tokenClient).issueToken();
        server.verify();
    }
}