package com.hia.user_service.common;

import com.hia.common.response.ErrorResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;


import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "eureka.client.enabled=false"
})
@AutoConfigureMockMvc
@Import(CommonModuleTestController.class)
public class CommonModuleIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void ApiResponse_정상_응답_검증() throws Exception {

        mockMvc.perform(get("/user/test/success"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message")
                        .value("요청이 성공적으로 처리되었습니다."))
                .andExpect(jsonPath("$.data").value("test"))
                .andDo(print());
    }

    @Test
    void customException_예외_응답_검증() throws Exception {

        mockMvc.perform(get("/user/test/error"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("USER_TEST_ERROR"))
                .andExpect(jsonPath("$.message").value("사용자를 찾을 수 없습니다."))
                .andDo(print());
    }
}
