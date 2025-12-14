package com.mido.backend.auth.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mido.backend.auth.dto.AuthResponse;
import com.mido.backend.auth.dto.LoginRequest;
import com.mido.backend.auth.dto.RegisterRequest;
import com.mido.backend.auth.service.AuthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@DisplayName("认证控制器测试")
class AuthControllerTest {

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;
    private AuthService authService;

    @BeforeEach
    void setUp() {
        authService = mock(AuthService.class);
        objectMapper = new ObjectMapper();
        mockMvc = MockMvcBuilders.standaloneSetup(new AuthController(authService))
                .build();
    }

    @Test
    @DisplayName("POST /api/auth/register 成功返回201和token")
    void register_Success_Returns201AndToken() throws Exception {
        // Given
        RegisterRequest request = new RegisterRequest();
        request.setUsername("newuser");
        request.setEmail("newuser@example.com");
        request.setPassword("password123");
        request.setConfirmPassword("password123");

        AuthResponse response = AuthResponse.builder()
                .message("注册成功")
                .token("test_token")
                .build();

        when(authService.register(any(RegisterRequest.class))).thenReturn(response);

        // When & Then
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.message").value("注册成功"))
                .andExpect(jsonPath("$.token").value("test_token"));
    }

    @Test
    @DisplayName("POST /api/auth/register 用户名重复返回400")
    void register_UsernameExists_Returns400() throws Exception {
        // Given
        RegisterRequest request = new RegisterRequest();
        request.setUsername("existinguser");
        request.setEmail("newuser@example.com");
        request.setPassword("password123");
        request.setConfirmPassword("password123");

        when(authService.register(any(RegisterRequest.class)))
                .thenThrow(new org.springframework.web.server.ResponseStatusException(
                        org.springframework.http.HttpStatus.BAD_REQUEST, "用户名已存在"));

        // When & Then
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /api/auth/register 邮箱重复返回400")
    void register_EmailExists_Returns400() throws Exception {
        // Given
        RegisterRequest request = new RegisterRequest();
        request.setUsername("newuser");
        request.setEmail("existing@example.com");
        request.setPassword("password123");
        request.setConfirmPassword("password123");

        when(authService.register(any(RegisterRequest.class)))
                .thenThrow(new org.springframework.web.server.ResponseStatusException(
                        org.springframework.http.HttpStatus.BAD_REQUEST, "邮箱已被注册"));

        // When & Then
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /api/auth/register 请求体验证失败返回400")
    void register_ValidationFailed_Returns400() throws Exception {
        // Given - 缺少必填字段
        RegisterRequest request = new RegisterRequest();
        request.setUsername(""); // 空用户名
        request.setEmail("invalid-email"); // 无效邮箱
        request.setPassword("123"); // 密码太短
        request.setConfirmPassword("123");

        // When & Then
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /api/auth/login 成功返回200和token")
    void login_Success_Returns200AndToken() throws Exception {
        // Given
        LoginRequest request = new LoginRequest();
        request.setIdentifier("testuser");
        request.setPassword("password123");

        AuthResponse response = AuthResponse.builder()
                .message("登录成功")
                .token("test_token")
                .build();

        when(authService.login(any(LoginRequest.class))).thenReturn(response);

        // When & Then
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("登录成功"))
                .andExpect(jsonPath("$.token").value("test_token"));
    }

    @Test
    @DisplayName("POST /api/auth/login 密码错误返回401")
    void login_WrongPassword_Returns401() throws Exception {
        // Given
        LoginRequest request = new LoginRequest();
        request.setIdentifier("testuser");
        request.setPassword("wrong_password");

        when(authService.login(any(LoginRequest.class)))
                .thenThrow(new org.springframework.web.server.ResponseStatusException(
                        org.springframework.http.HttpStatus.UNAUTHORIZED, "密码错误"));

        // When & Then
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("POST /api/auth/login 请求体验证失败返回400")
    void login_ValidationFailed_Returns400() throws Exception {
        // Given - 缺少必填字段
        LoginRequest request = new LoginRequest();
        request.setIdentifier(""); // 空标识符
        request.setPassword(""); // 空密码

        // When & Then
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }
}

