package com.momently.orchestrator.adapter.in.web;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.momently.orchestrator.adapter.in.web.request.LoginRequest;
import com.momently.orchestrator.config.MomentlySecurityProperties;
import com.momently.orchestrator.security.AuthService;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

/**
 * 로그인 API의 HTTP 계약을 검증한다.
 */
@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private static final MomentlySecurityProperties TEST_PROPS = new MomentlySecurityProperties(
        "console",
        "changeme",
        "local-dev-console-jwt-signing-secret-min32b",
        3600L
    );

    @Mock
    private AuthService authService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();
        mockMvc = MockMvcBuilders.standaloneSetup(new AuthController(authService, TEST_PROPS))
            .setValidator(validator)
            .build();
    }

    @Test
    @DisplayName("로그인 성공 시 토큰과 만료 시간을 반환한다")
    void returnsTokenWhenCredentialsOk() throws Exception {
        when(authService.issueToken(anyString(), anyString()))
            .thenReturn(Optional.of("signed-jwt-value"));

        mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(MAPPER.writeValueAsString(new LoginRequest("console", "changeme"))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.accessToken").value("signed-jwt-value"))
            .andExpect(jsonPath("$.tokenType").value("Bearer"))
            .andExpect(jsonPath("$.expiresInSeconds").value(3600));

        verify(authService).issueToken("console", "changeme");
    }

    @Test
    @DisplayName("로그인 실패 시 401과 메시지를 반환한다")
    void returnsUnauthorizedWhenBadCredentials() throws Exception {
        when(authService.issueToken(anyString(), anyString())).thenReturn(Optional.empty());

        mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(MAPPER.writeValueAsString(new LoginRequest("x", "y"))))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.error").exists());
    }

    @Test
    @DisplayName("로그인 요청 검증 오류 시 400")
    void rejectsBlankLoginFields() throws Exception {
        mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"username":"   ","password":""}
                    """))
            .andExpect(status().isBadRequest());
    }
}
