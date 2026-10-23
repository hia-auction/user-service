package com.hia.user_service.user.infrastructure.keycloak.client;

import com.hia.common.exception.CustomException;
import com.hia.user_service.user.application.exception.UserErrorCode;
import com.hia.user_service.user.infrastructure.keycloak.config.KeycloakAdminProperties;
import com.hia.user_service.user.infrastructure.keycloak.dto.KeycloakTokenResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

@ExtendWith(OutputCaptureExtension.class)
class KeycloakTokenClientTest {

    @Test
    void 서비스_계정_토큰을_발급받는다() {
        /*준비: 실제 값 대신 테스트 전용 주소·자격증명 설정*/
        KeycloakAdminProperties properties = new KeycloakAdminProperties();

        properties.setBaseUrl("http://keycloak.test");
        properties.setRealm("hia");
        properties.setClientId("test-client");
        properties.setClientSecret("test-secret");

        RestClient.Builder builder = RestClient.builder()
                .baseUrl(properties.getBaseUrl());

        MockRestServiceServer server = MockRestServiceServer
                .bindTo(builder).build();

        KeycloakTokenClient client = new KeycloakTokenClient(builder.build(), properties);

        MultiValueMap<String, String> expectedForm = new LinkedMultiValueMap<>();
        expectedForm.add("grant_type", "client_credentials");
        /*기대 요청 정의: URL, POST, Basic Auth, 폼 본문 검증*/
        server.expect(requestTo(
                        "http://keycloak.test/realms/hia/protocol/openid-connect/token"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header(
                        HttpHeaders.AUTHORIZATION,
                        "Basic " + HttpHeaders.encodeBasicAuth(
                                "test-client",
                                "test-secret",
                                StandardCharsets.ISO_8859_1
                        )
                ))
                .andExpect(content().contentTypeCompatibleWith(
                        MediaType.APPLICATION_FORM_URLENCODED))
                .andExpect(content().formData(expectedForm))

                /*가짜 응답 제공: Keycloak 대신 성공 JSON 반환*/
                .andRespond(withSuccess("""
                        {
                          "access_token": "test-access-token",
                          "expires_in": 300,
                          "token_type": "Bearer"
                        }
                        """, MediaType.APPLICATION_JSON));

        KeycloakTokenResponse response = client.issueToken();

        /*검증: DTO 변환 결과와 예상 요청 실행 여부 확인*/
        assertThat(response.accessToken()).isEqualTo("test-access-token");
        assertThat(response.expiresIn()).isEqualTo(300);
        assertThat(response.tokenType()).isEqualTo("Bearer");

        server.verify();
    }

    @Test
    void 토큰_발급이_거절되면_요청실패_예외가_발생한다(CapturedOutput output) {
        /*준비: 실제 값 대신 테스트 전용 주소·자격증명 설정*/
        KeycloakAdminProperties properties = new KeycloakAdminProperties();

        properties.setBaseUrl("http://keycloak.test");
        properties.setRealm("hia");
        properties.setClientId("test-client");
        properties.setClientSecret("test-secret");

        RestClient.Builder builder = RestClient.builder()
                .baseUrl(properties.getBaseUrl());

        MockRestServiceServer server = MockRestServiceServer
                .bindTo(builder).build();

        KeycloakTokenClient client = new KeycloakTokenClient(builder.build(), properties);

        MultiValueMap<String, String> expectedForm = new LinkedMultiValueMap<>();
        expectedForm.add("grant_type", "client_credentials");
        // 기대 요청 정의: URL, POST
        // Keycloak이 401로 응답하는 상황 준비
        server.expect(requestTo(
                        "http://keycloak.test/realms/hia/protocol/openid-connect/token"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withStatus(HttpStatus.UNAUTHORIZED).body("private-response-body"));

        /*비교
        server.expect(requestTo(
                        "http://keycloak.test/realms/hia/protocol/openid-connect/token"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header(
                        HttpHeaders.AUTHORIZATION,
                        "Basic " + HttpHeaders.encodeBasicAuth(
                                "test-client",
                                "test-secret",
                                StandardCharsets.ISO_8859_1
                        )
                ))
                .andExpect(content().contentTypeCompatibleWith(
                        MediaType.APPLICATION_FORM_URLENCODED))
                .andExpect(content().formData(expectedForm))*/

        // 실행 시 발생하는 예외와 오류 코드 검증
        assertThatThrownBy(() -> client.issueToken())
                .isInstanceOfSatisfying(
                        CustomException.class,
                        exception -> assertThat(exception.getErrorCode())
                                .isEqualTo(UserErrorCode.KEYCLOAK_TOKEN_REQUEST_FAILED)
                );

        server.verify();
        assertThat(output.getAll()).contains("Keycloak token request failed: status=401")
                .doesNotContain("private-response-body", "private-exception-message", "test-client", "test-secret",
                        "Basic dGVzdC1jbGllbnQ6dGVzdC1zZWNyZXQ=", "grant_type");
    }


    @Test
    void 성공_응답에_토큰이_없으면_잘못된_응답_예외가_발생한다() {
        /*준비: 실제 값 대신 테스트 전용 주소·자격증명 설정*/
        KeycloakAdminProperties properties = new KeycloakAdminProperties();

        properties.setBaseUrl("http://keycloak.test");
        properties.setRealm("hia");
        properties.setClientId("test-client");
        properties.setClientSecret("test-secret");

        RestClient.Builder builder = RestClient.builder()
                .baseUrl(properties.getBaseUrl());

        MockRestServiceServer server = MockRestServiceServer
                .bindTo(builder).build();

        KeycloakTokenClient client = new KeycloakTokenClient(builder.build(), properties);

//        MultiValueMap<String, String> expectedForm = new LinkedMultiValueMap<>();
//        expectedForm.add("grant_type", "client_credentials");

        // Keycloak이 200로 응답하는 상황 준비 / accessToken을 제외
        server.expect(requestTo(
                        "http://keycloak.test/realms/hia/protocol/openid-connect/token"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess("""
                {
                  "expires_in": 300,
                  "token_type": "Bearer"
                }
                """, MediaType.APPLICATION_JSON));

        // 실행 시 발생하는 예외와 오류 코드 검증
        assertThatThrownBy(() -> client.issueToken())
                .isInstanceOfSatisfying(
                        CustomException.class,
                        exception -> assertThat(exception.getErrorCode())
                                .isEqualTo(UserErrorCode.KEYCLOAK_TOKEN_INVALID_RESPONSE)
                );

        server.verify();
    }

    @Test
    void 응답을_DTO로_변환하지_못하면_예외가_발생한다(CapturedOutput output) {
        /*준비: 실제 값 대신 테스트 전용 주소·자격증명 설정*/
        KeycloakAdminProperties properties = new KeycloakAdminProperties();

        properties.setBaseUrl("http://keycloak.test");
        properties.setRealm("hia");
        properties.setClientId("test-client");
        properties.setClientSecret("test-secret");

        RestClient.Builder builder = RestClient.builder()
                .baseUrl(properties.getBaseUrl());

        MockRestServiceServer server = MockRestServiceServer
                .bindTo(builder).build();

        KeycloakTokenClient client = new KeycloakTokenClient(builder.build(), properties);

//        MultiValueMap<String, String> expectedForm = new LinkedMultiValueMap<>();
//        expectedForm.add("grant_type", "client_credentials");

        // Keycloak이 200로 응답하지만 JSON 반환에 실패 / expires_in은 long타입
        server.expect(requestTo(
                        "http://keycloak.test/realms/hia/protocol/openid-connect/token"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess("""
                {
                  "access_token": "private-response-body",
                  "expires_in": "not-a-number",
                  "token_type": "Bearer"
                }
                """, MediaType.APPLICATION_JSON));

        // 실행 시 발생하는 예외와 오류 코드 검증
        assertThatThrownBy(() -> client.issueToken())
                .isInstanceOfSatisfying(
                        CustomException.class,
                        exception -> assertThat(exception.getErrorCode())
                                .isEqualTo(UserErrorCode.KEYCLOAK_TOKEN_INVALID_RESPONSE)
                );

        server.verify();
        assertThat(output.getAll()).contains("Keycloak token request failed: exception=RestClientException")
                .doesNotContain("private-response-body", "private-exception-message", "test-client", "test-secret",
                        "Basic dGVzdC1jbGllbnQ6dGVzdC1zZWNyZXQ=", "grant_type");
    }

    @Test
    void 통신_도중_실패로_인한_예외가_발생한다(CapturedOutput output) {
        /*준비: 실제 값 대신 테스트 전용 주소·자격증명 설정*/
        KeycloakAdminProperties properties = new KeycloakAdminProperties();

        properties.setBaseUrl("http://keycloak.test");
        properties.setRealm("hia");
        properties.setClientId("test-client");
        properties.setClientSecret("test-secret");

        RestClient.Builder builder = RestClient.builder()
                .baseUrl(properties.getBaseUrl());

        MockRestServiceServer server = MockRestServiceServer
                .bindTo(builder).build();

        KeycloakTokenClient client = new KeycloakTokenClient(builder.build(), properties);

        MultiValueMap<String, String> expectedForm = new LinkedMultiValueMap<>();
        expectedForm.add("grant_type", "client_credentials");
        /*기대 요청 정의: URL, POST, Basic Auth, 폼 본문 검증*/
        server.expect(requestTo(
                        "http://keycloak.test/realms/hia/protocol/openid-connect/token"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header(
                        HttpHeaders.AUTHORIZATION,
                        "Basic " + HttpHeaders.encodeBasicAuth(
                                "test-client",
                                "test-secret",
                                StandardCharsets.ISO_8859_1
                        )
                ))
                .andExpect(content().contentTypeCompatibleWith(
                        MediaType.APPLICATION_FORM_URLENCODED))
                .andExpect(content().formData(expectedForm))

                /*가짜 응답 제공: Keycloak 대신 성공 JSON 반환*/
                .andRespond(request -> {
                    throw new IOException("private-exception-message");
                });

        assertThatThrownBy(() -> client.issueToken())
                .isInstanceOfSatisfying(
                        CustomException.class,
                        exception -> assertThat(exception.getErrorCode())
                                .isEqualTo(UserErrorCode.KEYCLOAK_UNAVAILABLE)
                );

        server.verify();
        assertThat(output.getAll()).contains("Keycloak token request failed: exception=ResourceAccessException")
                .doesNotContain("private-response-body", "private-exception-message", "test-client", "test-secret",
                        "Basic dGVzdC1jbGllbnQ6dGVzdC1zZWNyZXQ=", "grant_type");
    }
}
