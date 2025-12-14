package com.mido.backend.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("JWT工具类测试")
class JwtUtilTest {

    private JwtUtil jwtUtil;
    private JwtProperties jwtProperties;
    private com.mido.backend.user.entity.User testUser;
    private UserDetails userDetails;

    @BeforeEach
    void setUp() {
        jwtProperties = new JwtProperties();
        jwtProperties.setSecret("test_secret_key_must_be_at_least_32_characters_long");
        jwtProperties.setExpirationMinutes(24 * 60); // 24小时

        jwtUtil = new JwtUtil(jwtProperties);

        testUser = new com.mido.backend.user.entity.User();
        testUser.setId(1L);
        testUser.setUsername("testuser");
        testUser.setEmail("test@example.com");
        testUser.setPasswordHash("encoded_password");
        testUser.setCreatedAt(LocalDateTime.now());

        userDetails = org.springframework.security.core.userdetails.User.withUsername("testuser")
                .password("password")
                .authorities("ROLE_USER")
                .build();
    }

    @Test
    @DisplayName("生成token包含正确的claims")
    void generateToken_ContainsCorrectClaims() {
        // When
        String token = jwtUtil.generateToken(testUser);

        // Then
        assertThat(token).isNotNull();
        assertThat(token).isNotEmpty();

        // 验证可以提取username
        String username = jwtUtil.extractUsername(token);
        assertThat(username).isEqualTo("testuser");
    }

    @Test
    @DisplayName("生成token包含uid和email")
    void generateToken_ContainsUidAndEmail() {
        // When
        String token = jwtUtil.generateToken(testUser);

        // Then
        assertThat(token).isNotNull();
        
        // Token应该包含正确的subject（username）
        String username = jwtUtil.extractUsername(token);
        assertThat(username).isEqualTo("testuser");
    }

    @Test
    @DisplayName("Token过期时间正确（24小时）")
    void generateToken_ExpirationTimeIsCorrect() {
        // When
        String token = jwtUtil.generateToken(testUser);

        // Then
        assertThat(token).isNotNull();
        
        // 验证token在过期前是有效的
        boolean isValid = jwtUtil.isTokenValid(token, userDetails);
        assertThat(isValid).isTrue();
    }

    @Test
    @DisplayName("有效token能正确提取username")
    void extractUsername_ValidToken_ReturnsUsername() {
        // Given
        String token = jwtUtil.generateToken(testUser);

        // When
        String username = jwtUtil.extractUsername(token);

        // Then
        assertThat(username).isEqualTo("testuser");
    }

    @Test
    @DisplayName("无效token返回null")
    void extractUsername_InvalidToken_ReturnsNull() {
        // Given
        String invalidToken = "invalid.token.here";

        // When
        String username = jwtUtil.extractUsername(invalidToken);

        // Then
        assertThat(username).isNull();
    }

    @Test
    @DisplayName("有效token与UserDetails匹配")
    void isTokenValid_ValidTokenAndMatchingUserDetails_ReturnsTrue() {
        // Given
        String token = jwtUtil.generateToken(testUser);

        // When
        boolean isValid = jwtUtil.isTokenValid(token, userDetails);

        // Then
        assertThat(isValid).isTrue();
    }

    @Test
    @DisplayName("token与UserDetails不匹配时返回false")
    void isTokenValid_TokenAndUserDetailsMismatch_ReturnsFalse() {
        // Given
        String token = jwtUtil.generateToken(testUser);
        UserDetails otherUserDetails = org.springframework.security.core.userdetails.User.withUsername("otheruser")
                .password("password")
                .authorities("ROLE_USER")
                .build();

        // When
        boolean isValid = jwtUtil.isTokenValid(token, otherUserDetails);

        // Then
        assertThat(isValid).isFalse();
    }

    @Test
    @DisplayName("空token返回null")
    void extractUsername_NullToken_ReturnsNull() {
        // When
        String username = jwtUtil.extractUsername(null);

        // Then
        assertThat(username).isNull();
    }

    @Test
    @DisplayName("不同用户生成不同的token")
    void generateToken_DifferentUsers_GenerateDifferentTokens() {
        // Given
        com.mido.backend.user.entity.User user1 = new com.mido.backend.user.entity.User();
        user1.setId(1L);
        user1.setUsername("user1");
        user1.setEmail("user1@example.com");

        com.mido.backend.user.entity.User user2 = new com.mido.backend.user.entity.User();
        user2.setId(2L);
        user2.setUsername("user2");
        user2.setEmail("user2@example.com");

        // When
        String token1 = jwtUtil.generateToken(user1);
        String token2 = jwtUtil.generateToken(user2);

        // Then
        assertThat(token1).isNotEqualTo(token2);
        assertThat(jwtUtil.extractUsername(token1)).isEqualTo("user1");
        assertThat(jwtUtil.extractUsername(token2)).isEqualTo("user2");
    }
}

