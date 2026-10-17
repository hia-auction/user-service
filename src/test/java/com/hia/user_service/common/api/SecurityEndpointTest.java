package com.hia.user_service.common.api;

import com.hia.user_service.common.config.SecurityConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
        controllers = {
                PublicController.class,
                AuthenticationController.class
        },
        properties = {
                "spring.security.oauth2.resourceserver.jwt.issuer-uri=http://test-issuer",
                "spring.security.oauth2.resourceserver.jwt.audiences=hia-api"
        }
)


@Import(SecurityConfig.class)
public class SecurityEndpointTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @Test
    void 공개API_토큰없이_호출할_수_있다() throws Exception {

        mockMvc.perform(get("/api/public/ping"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ok"));
    }

    @Test
    void 인증API는_인증을_요구할_수_있다() throws Exception {

        mockMvc.perform(get("/api/auth/me"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void 인증API_Controller_올바른_작동_테스트() throws Exception {

        mockMvc.perform(get("/api/auth/me")
                .with(jwt().jwt(jwt -> jwt
                        .subject("keycloak-test-sub")
                        .claim("email", "test@example.com")
                        .claim("preferred_username", "mockTester")
                )))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.subject").value("keycloak-test-sub"))
                .andExpect(jsonPath("$.email").value("test@example.com"))
                .andExpect(jsonPath("$.preferredUsername").value("mockTester"));

    }
}
