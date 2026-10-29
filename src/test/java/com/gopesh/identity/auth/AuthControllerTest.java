package com.gopesh.identity.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gopesh.identity.auth.controller.AuthController;
import com.gopesh.identity.auth.dto.LoginRequest;
import com.gopesh.identity.auth.dto.RefreshRequest;
import com.gopesh.identity.auth.dto.TokenResponse;
import com.gopesh.identity.auth.service.AuthService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AuthController.class)
@AutoConfigureMockMvc(addFilters = false)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AuthService authService;

    @Test
    @DisplayName("POST /v1/auth/login — should return tokens on valid credentials")
    void shouldReturnTokensOnLogin() throws Exception {
        LoginRequest loginRequest = LoginRequest.builder()
                .email("user@sky.com")
                .password("SecurePass123!")
                .build();

        TokenResponse tokenResponse = TokenResponse.of(
                "access-token-xyz", "refresh-token-abc", 900, "USER", "sky-uk");

        when(authService.login(any(LoginRequest.class))).thenReturn(tokenResponse);

        mockMvc.perform(post("/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("access-token-xyz"))
                .andExpect(jsonPath("$.refreshToken").value("refresh-token-abc"))
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.role").value("USER"))
                .andExpect(jsonPath("$.tenantId").value("sky-uk"));
    }

    @Test
    @DisplayName("POST /v1/auth/login — should return 401 on bad credentials")
    void shouldReturn401OnBadCredentials() throws Exception {
        LoginRequest loginRequest = LoginRequest.builder()
                .email("user@sky.com")
                .password("wrong")
                .build();

        when(authService.login(any(LoginRequest.class)))
                .thenThrow(new BadCredentialsException("Invalid credentials"));

        mockMvc.perform(post("/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("POST /v1/auth/login — should return 400 on missing email")
    void shouldReturn400OnInvalidRequest() throws Exception {
        LoginRequest loginRequest = LoginRequest.builder()
                .password("password")
                .build();

        mockMvc.perform(post("/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /v1/auth/refresh — should return new tokens on valid refresh token")
    void shouldRefreshTokens() throws Exception {
        RefreshRequest refreshRequest = new RefreshRequest("valid-refresh-token");

        TokenResponse tokenResponse = TokenResponse.of(
                "new-access-token", "new-refresh-token", 900, "USER", "sky-uk");

        when(authService.refresh(any(RefreshRequest.class))).thenReturn(tokenResponse);

        mockMvc.perform(post("/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(refreshRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("new-access-token"))
                .andExpect(jsonPath("$.refreshToken").value("new-refresh-token"));
    }
}
