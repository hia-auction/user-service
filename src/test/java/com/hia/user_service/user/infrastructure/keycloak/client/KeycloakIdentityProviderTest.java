package com.hia.user_service.user.infrastructure.keycloak.client;

import com.hia.common.exception.CustomException;
import com.hia.user_service.user.application.exception.UserErrorCode;
import com.hia.user_service.user.infrastructure.keycloak.config.KeycloakAdminProperties;
import com.hia.user_service.user.infrastructure.keycloak.dto.KeycloakTokenResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.io.IOException;
import java.net.URI;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withCreatedEntity;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;


@ExtendWith(OutputCaptureExtension.class)
class KeycloakIdentityProviderTest {

    @ParameterizedTest
    @CsvSource({
            "http://keycloak.test, http://keycloak.test/admin/realms/hia/users",
            "http://keycloak.test/, http://keycloak.test/admin/realms/hia/users",
            "http://keycloak.test/auth, http://keycloak.test/auth/admin/realms/hia/users",
            "http://keycloak.test/auth/, http://keycloak.test/auth/admin/realms/hia/users",
            "http://keycloak.test/gateway/auth, http://keycloak.test/gateway/auth/admin/realms/hia/users",
            "http://keycloak.test/gateway/auth/, http://keycloak.test/gateway/auth/admin/realms/hia/users"
    })
    void 계정을_생성하고_인증_식별자를_반환한다(String baseUrl, String usersUrl) {

        KeycloakAdminProperties properties = new KeycloakAdminProperties();
        properties.setBaseUrl(baseUrl);
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

        server.expect(requestTo(usersUrl))
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
                .andRespond(withCreatedEntity(URI.create(usersUrl + "/test-user-id")));

        String authSubject = provider.createUser(
                "tester@example.com",
                "test-password");

        assertEquals("test-user-id", authSubject);

        verify(tokenClient).issueToken();
        server.verify();
    }

    @Test
    void 중복_계정이면_USER_ALREADY_EXISTS_예외가_발생한다(CapturedOutput output) {

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
                .andRespond(withStatus(HttpStatus.CONFLICT).body("private-response-body"));

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
        assertThat(output.getAll()).contains("Keycloak user creation failed: status=409")
                .doesNotContain("private-response-body", "tester@example.com", "test-password", "test-admin-access-token");
    }

    @Test
    void 인증_서버가_오류를_반환하면_KEYCLOAK_USER_CREATION_FAILED_예외가_발생한다(CapturedOutput output) {

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
                .andRespond(withStatus(HttpStatus.INTERNAL_SERVER_ERROR).body("private-response-body"));

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
        assertThat(output.getAll()).contains("Keycloak user creation failed: status=500")
                .doesNotContain("private-response-body", "tester@example.com", "test-password", "test-admin-access-token");
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void 계정_생성_중_클라이언트_예외를_변환하고_클래스명만_기록한다(boolean networkFailure, CapturedOutput output) {

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
                    if (networkFailure) {
                        throw new IOException("private-exception-message");
                    }
                    throw new RestClientException("private-exception-message");
                });
        CustomException exception = assertThrows(
                CustomException.class,
                () -> provider.createUser(
                        "tester@example.com",
                        "test-password"
                )
        );

        assertEquals(networkFailure ? UserErrorCode.KEYCLOAK_UNAVAILABLE : UserErrorCode.KEYCLOAK_USER_CREATION_FAILED,
                exception.getErrorCode());
        assertThat(output.getAll()).contains("Keycloak user creation failed: exception="
                        + (networkFailure ? "ResourceAccessException" : "RestClientException"))
                .doesNotContain("private-exception-message", "tester@example.com", "test-password", "test-admin-access-token");

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

    static Stream<Arguments> invalidLocations() {
        return Stream.of("", "/auth").flatMap(basePath -> Stream.of(
                "http://[",
                "http://keycloak.test" + basePath + "/admin/realms/other/users/user-123",
                "http://keycloak.test" + basePath + "/admin/realms/hia/users",
                "http://keycloak.test" + basePath + "/admin/realms/hia/users/",
                "http://keycloak.test" + basePath + "/admin/realms/hia/users/.",
                "http://keycloak.test" + basePath + "/admin/realms/hia/users/..",
                "http://keycloak.test" + basePath + "/admin/realms/hia/users/user-123/extra",
                "http://keycloak.test/wrong/admin/realms/hia/users/user-123"
        ).map(location -> Arguments.of(basePath, location)));
    }

    @ParameterizedTest
    @MethodSource("invalidLocations")
    void Location이_잘못되면_응답검증_예외가_발생한다(String basePath, String location) {

        KeycloakAdminProperties properties = new KeycloakAdminProperties();
        properties.setBaseUrl("http://keycloak.test" + basePath);
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
