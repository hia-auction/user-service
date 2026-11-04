package com.hia.user_service.user.presentation.controller;

import com.hia.common.exception.CustomException;
import com.hia.user_service.user.application.exception.UserErrorCode;
import com.hia.user_service.user.application.port.out.IdentityProvider;
import com.hia.user_service.user.application.service.UserPersistenceService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "eureka.client.enabled=false",
        "spring.security.oauth2.resourceserver.jwt.issuer-uri=http://test-issuer",
        "spring.security.oauth2.resourceserver.jwt.audiences=hia-api",
        "keycloak.admin.base-url=http://keycloak.test",
        "keycloak.admin.realm=hia",
        "keycloak.admin.client-id=test-client",
        "keycloak.admin.client-secret=test-only"
})
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class UserRegisterControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @MockitoSpyBean
    private UserPersistenceService persistenceService;

    @MockitoBean
    private IdentityProvider provider;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @Test
    void 토큰없이_가입하면_201과_민감정보가_없는_응답을_반환한다() throws Exception {

        try {
            when(provider.createUser("test@example.com", "test-password")).thenReturn("created-keycloak-id");

            mockMvc.perform(post("/api/users")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                      "email": "test@example.com",
                                      "password": "test-password",
                                      "nickname": "경매초보"
                                    }
                                    """))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.message").value("가입이 성공적으로 이루어졌습니다."))
                    .andExpect(jsonPath("$.data.email").value("test@example.com"))
                    .andExpect(jsonPath("$.data.nickname").value("경매초보"))
                    .andExpect(jsonPath("$.data.role").value("BUYER"))
                    .andExpect(jsonPath("$.data.status").value("ACTIVE"))
                    .andExpect(jsonPath("$.data.password").doesNotExist())
                    .andExpect(jsonPath("$.data.authSubject").doesNotExist());

            verify(provider, never()).deleteUser(anyString());

            String countSql =
                    "SELECT COUNT(*) FROM users WHERE auth_subject = ?";

            long count = jdbcTemplate.queryForObject(
                    countSql, Long.class, "created-keycloak-id");

            assertEquals(1L, count);
        } finally {
            jdbcTemplate.update(
                    "DELETE FROM users WHERE auth_subject = ?",
                    "created-keycloak-id");
        }
    }

    @Test
    void 가입요청_keycloak_error() throws Exception{
        String authSubject = "registration-test";
        String email = "test@example.com";
        String password = "test-password";
        String nickname = "경매초보";

        try {
            CustomException createException = new CustomException(UserErrorCode.KEYCLOAK_UNAVAILABLE);
            when(provider.createUser(email, password)).thenThrow(createException);

            mockMvc.perform(post("/api/users")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                            {
                              "email": "test@example.com",
                              "password": "test-password",
                              "nickname": "경매초보"
                            }
                            """))
                    .andExpect(status().isServiceUnavailable())
                    .andExpect(jsonPath("$.status").value(503))
                    .andExpect(jsonPath("$.error").value("KEYCLOAK_UNAVAILABLE"));

            verify(provider).createUser(email, password);
            verify(provider, never()).deleteUser(anyString());
            verify(persistenceService, never()).saveUser(any(), any(), any());

            long count = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM users WHERE email = ?",
                    Long.class, email);

            assertEquals(0L, count);

        } finally {
            jdbcTemplate.update(
                    "DELETE FROM users WHERE email = ?",
                    email);
        }
    }
}
