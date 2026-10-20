package com.hia.user_service.user.presentation.controller;

import com.hia.user_service.user.domain.entity.User;
import com.hia.user_service.user.domain.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;


import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "eureka.client.enabled=false",
        "spring.security.oauth2.resourceserver.jwt.issuer-uri=http://test-issuer",
        "spring.security.oauth2.resourceserver.jwt.audiences=hia-api"
})
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @Test
    void 인증된_subject에_해당하는_DB_사용자를_반환한다() throws Exception {

        String authSubject = UUID.randomUUID().toString();

        User savedUser = userRepository.save(User.create(
                authSubject,
                "db-user@example.com",
                "DB 사용자"
        ));

        mockMvc.perform(get("/api/users/me")
                .with(jwt().jwt(token -> token
                        .subject(authSubject)
                        .claim("email", "jwt-user@example.com")
                )))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id")
                        .value(savedUser.getId().toString()))
                .andExpect(jsonPath("$.data.email")
                        .value("db-user@example.com"))
                .andExpect(jsonPath("$.data.name")
                        .value("DB 사용자"))
                .andExpect(jsonPath("$.data.role").value("BUYER"))
                .andExpect(jsonPath("$.data.status").value("ACTIVE"));
    }

    @Test
    void 인증된_subject에_해당하는_회원이_없으면_404를_반환한다() throws Exception {

        String unknownSubject = UUID.randomUUID().toString();

        mockMvc.perform(get("/api/users/me")
                        .with(jwt().jwt(token ->
                                token.subject(unknownSubject))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("USER_NOT_FOUND"))
                .andExpect(jsonPath("$.message")
                        .value("사용자를 찾을 수 없습니다."));
    }

    @Test
    void 인증정보가_없으면_401을_반환한다() throws Exception {
        mockMvc.perform(get("/api/users/me"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void 요청_파라미터로_조회_대상을_바꿀_수_없다() throws Exception {

        String subject1 = UUID.randomUUID().toString();
        String subject2 = UUID.randomUUID().toString();

        User userA = userRepository.save(User.create(
                subject1,
                "tester1@example.com",
                "tester1"
        ));

        User userB = userRepository.save(User.create(
                subject2,
                "tester2@example.com",
                "tester2"
        ));

        mockMvc.perform(get("/api/users/me")
                .param("authSubject", userB.getAuthSubject())
                .with(jwt().jwt(token -> token
                        .subject(userA.getAuthSubject()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id")
                        .value(userA.getId().toString()));


    }
}