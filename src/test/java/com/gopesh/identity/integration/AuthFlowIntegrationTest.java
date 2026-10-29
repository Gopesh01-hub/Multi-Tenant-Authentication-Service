package com.gopesh.identity.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gopesh.identity.auth.dto.LoginRequest;
import com.gopesh.identity.auth.dto.RefreshRequest;
import com.gopesh.identity.auth.dto.TokenResponse;
import com.gopesh.identity.user.dto.CreateUserRequest;
import com.gopesh.identity.user.dto.UserResponse;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * End-to-end integration test that exercises the full auth flow:
 * register -> login -> access protected resource -> refresh -> logout.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class AuthFlowIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private static String accessToken;
    private static String refreshToken;
    private static Long userId;

    @Test
    @Order(1)
    @DisplayName("Step 1: Register a new user")
    void shouldRegisterUser() throws Exception {
        CreateUserRequest request = CreateUserRequest.builder()
                .email("integration@test.com")
                .password("IntegrationTest123!")
                .firstName("Integration")
                .lastName("Test")
                .build();

        MvcResult result = mockMvc.perform(post("/v1/users")
                        .header("X-Tenant-ID", "test-tenant")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value("integration@test.com"))
                .andExpect(jsonPath("$.tenantId").value("test-tenant"))
                .andReturn();

        UserResponse userResponse = objectMapper.readValue(
                result.getResponse().getContentAsString(), UserResponse.class);
        userId = userResponse.getId();
        assertThat(userId).isNotNull();
    }

    @Test
    @Order(2)
    @DisplayName("Step 2: Login with registered credentials")
    void shouldLoginSuccessfully() throws Exception {
        LoginRequest request = LoginRequest.builder()
                .email("integration@test.com")
                .password("IntegrationTest123!")
                .deviceInfo("integration-test")
                .build();

        MvcResult result = mockMvc.perform(post("/v1/auth/login")
                        .header("X-Tenant-ID", "test-tenant")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.refreshToken").isNotEmpty())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andReturn();

        TokenResponse tokenResponse = objectMapper.readValue(
                result.getResponse().getContentAsString(), TokenResponse.class);
        accessToken = tokenResponse.getAccessToken();
        refreshToken = tokenResponse.getRefreshToken();
    }

    @Test
    @Order(3)
    @DisplayName("Step 3: Access protected endpoint with JWT")
    void shouldAccessProtectedEndpoint() throws Exception {
        mockMvc.perform(get("/v1/users/" + userId)
                        .header("X-Tenant-ID", "test-tenant")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("integration@test.com"));
    }

    @Test
    @Order(4)
    @DisplayName("Step 4: Refresh the access token")
    void shouldRefreshToken() throws Exception {
        RefreshRequest request = new RefreshRequest(refreshToken);

        MvcResult result = mockMvc.perform(post("/v1/auth/refresh")
                        .header("X-Tenant-ID", "test-tenant")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andReturn();

        TokenResponse tokenResponse = objectMapper.readValue(
                result.getResponse().getContentAsString(), TokenResponse.class);
        accessToken = tokenResponse.getAccessToken();
        refreshToken = tokenResponse.getRefreshToken();
    }

    @Test
    @Order(5)
    @DisplayName("Step 5: Logout (revoke refresh token)")
    void shouldLogout() throws Exception {
        RefreshRequest request = new RefreshRequest(refreshToken);

        mockMvc.perform(post("/v1/auth/logout")
                        .header("X-Tenant-ID", "test-tenant")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNoContent());
    }

    @Test
    @Order(6)
    @DisplayName("Step 6: Reject access without token")
    void shouldRejectUnauthenticatedAccess() throws Exception {
        mockMvc.perform(get("/v1/users/" + userId)
                        .header("X-Tenant-ID", "test-tenant"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @Order(7)
    @DisplayName("Step 7: Reject login with wrong password")
    void shouldRejectBadPassword() throws Exception {
        LoginRequest request = LoginRequest.builder()
                .email("integration@test.com")
                .password("WrongPassword!")
                .build();

        mockMvc.perform(post("/v1/auth/login")
                        .header("X-Tenant-ID", "test-tenant")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }
}
