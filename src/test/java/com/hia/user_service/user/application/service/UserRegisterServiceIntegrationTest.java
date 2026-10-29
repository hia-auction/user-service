package com.hia.user_service.user.application.service;

import com.hia.common.exception.CustomException;
import com.hia.user_service.common.config.JpaConfig;
import com.hia.user_service.user.application.dto.command.RegisterUserCommand;
import com.hia.user_service.user.application.exception.UserErrorCode;
import com.hia.user_service.user.application.port.out.IdentityProvider;
import com.hia.user_service.user.domain.repository.UserRepository;
import com.hia.user_service.user.infrastructure.repository.UserRepositoryImpl;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@DataJpaTest(showSql = false)
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({
        UserRegisterService.class,
        UserPersistenceService.class,
        UserRepositoryImpl.class,
        JpaConfig.class
})
@Transactional(propagation = Propagation.NOT_SUPPORTED)
public class UserRegisterServiceIntegrationTest {

    @Autowired
    private UserRegisterService service;

    @Autowired
    private UserPersistenceService persistenceService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    //실제 객체의 일부 동작만 대체할 때 사용(이메일 사전 조회만 대체)
    @MockitoSpyBean
    private UserRepository repository;


    @MockitoBean
    private IdentityProvider provider;

    @Test
    void 실제_DB_중복_저장에_실패하면_신규_계정만_보상삭제한다() {
        //지금 unique컬럼은 subject, email, 닉네임이다.
        String suffix = UUID.randomUUID().toString();
        String email = suffix + "@example.com";
        String existingSubject = "existing-" + suffix;
        String createdSubject = "created-"+ suffix;

        try{
            //먼저 같은 이메일로 저장
            persistenceService.saveUser(existingSubject, email, "기존" + suffix.substring(0,8));

            //이메일 사전 중복 조회만 false로 대체
            doReturn(false).when(repository).existsByEmail(email);

            //실제 같은 이메일로 신규 준비
            RegisterUserCommand command = new RegisterUserCommand(
                    email, "test-password", "신규" + suffix.substring(0, 8)
            );

            //keycloak mock으로 대체 응답만 신규 subject대체
            when(provider.createUser(email, command.password())).thenReturn(createdSubject);

            //신규로 생성 진행
            CustomException exception = assertThrows(
                    CustomException.class,
                    () -> service.register(command));

            //그럼 같은 이메일 때문에 에러 발생
            assertEquals(UserErrorCode.USER_DATABASE_ERROR, exception.getErrorCode());
            assertInstanceOf(DataIntegrityViolationException.class, exception.getCause());

            verify(provider).deleteUser(createdSubject);
            verify(provider, never()).deleteUser(existingSubject);

            String countSql =
                    "SELECT COUNT(*) FROM users WHERE auth_subject = ?";

            long existingCount = jdbcTemplate.queryForObject(
                    countSql, Long.class, existingSubject);

            long createdCount = jdbcTemplate.queryForObject(
                    countSql, Long.class, createdSubject);

            assertEquals(1L, existingCount);
            assertEquals(0L, createdCount);

        } finally {
            jdbcTemplate.update(
                    "DELETE FROM users WHERE auth_subject IN (?, ?)",
                    existingSubject, createdSubject);
        }
    }
}
