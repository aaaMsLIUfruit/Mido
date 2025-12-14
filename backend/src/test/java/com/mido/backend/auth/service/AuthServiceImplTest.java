package com.mido.backend.auth.service;

import com.mido.backend.auth.dto.AuthResponse;
import com.mido.backend.auth.dto.LoginRequest;
import com.mido.backend.auth.dto.RegisterRequest;
import com.mido.backend.auth.service.impl.AuthServiceImpl;
import com.mido.backend.note.entity.NoteFolder;
import com.mido.backend.note.service.NoteFolderService;
import com.mido.backend.security.JwtUtil;
import com.mido.backend.user.entity.User;
import com.mido.backend.user.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("认证服务测试")
class AuthServiceImplTest {

    @Mock
    private UserService userService;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private JwtUtil jwtUtil;

    @Mock
    private NoteFolderService noteFolderService;

    @InjectMocks
    private AuthServiceImpl authService;

    private User testUser;
    private RegisterRequest validRegisterRequest;
    private LoginRequest validLoginRequest;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("testuser");
        testUser.setEmail("test@example.com");
        testUser.setPasswordHash("encoded_password");
        testUser.setCreatedAt(LocalDateTime.now());

        validRegisterRequest = new RegisterRequest();
        validRegisterRequest.setUsername("newuser");
        validRegisterRequest.setEmail("newuser@example.com");
        validRegisterRequest.setPassword("password123");
        validRegisterRequest.setConfirmPassword("password123");

        validLoginRequest = new LoginRequest();
        validLoginRequest.setIdentifier("testuser");
        validLoginRequest.setPassword("password123");
    }

    @Test
    @DisplayName("正常注册成功")
    void register_Success() {
        // Given
        when(userService.findByUsername("newuser")).thenReturn(Optional.empty());
        when(userService.findByEmail("newuser@example.com")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("password123")).thenReturn("encoded_password");
        when(userService.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId(2L);
            return true;
        });
        when(jwtUtil.generateToken(any(User.class))).thenReturn("test_token");

        // When
        AuthResponse response = authService.register(validRegisterRequest);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getMessage()).isEqualTo("注册成功");
        assertThat(response.getToken()).isEqualTo("test_token");

        // 验证用户保存
        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userService, times(1)).save(userCaptor.capture());
        User savedUser = userCaptor.getValue();
        assertThat(savedUser.getUsername()).isEqualTo("newuser");
        assertThat(savedUser.getEmail()).isEqualTo("newuser@example.com");
        assertThat(savedUser.getPasswordHash()).isEqualTo("encoded_password");

        // 验证自动创建默认文件夹
        ArgumentCaptor<NoteFolder> folderCaptor = ArgumentCaptor.forClass(NoteFolder.class);
        verify(noteFolderService, times(1)).save(folderCaptor.capture());
        NoteFolder defaultFolder = folderCaptor.getValue();
        assertThat(defaultFolder.getName()).isEqualTo("未分类");
        assertThat(defaultFolder.getParentId()).isNull();
        assertThat(defaultFolder.getUserId()).isEqualTo(2L);
        assertThat(defaultFolder.getIsDeleted()).isEqualTo(0);

        // 验证生成token
        verify(jwtUtil, times(1)).generateToken(any(User.class));
    }

    @Test
    @DisplayName("注册时用户名已存在")
    void register_UsernameExists_ThrowsException() {
        // Given
        when(userService.findByUsername("newuser")).thenReturn(Optional.of(testUser));

        // When & Then
        assertThatThrownBy(() -> authService.register(validRegisterRequest))
                .isInstanceOf(ResponseStatusException.class)
                .hasFieldOrPropertyWithValue("status", HttpStatus.BAD_REQUEST)
                .hasMessageContaining("用户名已存在");

        verify(userService, never()).save(any(User.class));
        verify(noteFolderService, never()).save(any(NoteFolder.class));
    }

    @Test
    @DisplayName("注册时邮箱已被注册")
    void register_EmailExists_ThrowsException() {
        // Given
        when(userService.findByUsername("newuser")).thenReturn(Optional.empty());
        when(userService.findByEmail("newuser@example.com")).thenReturn(Optional.of(testUser));

        // When & Then
        assertThatThrownBy(() -> authService.register(validRegisterRequest))
                .isInstanceOf(ResponseStatusException.class)
                .hasFieldOrPropertyWithValue("status", HttpStatus.BAD_REQUEST)
                .hasMessageContaining("邮箱已被注册");

        verify(userService, never()).save(any(User.class));
        verify(noteFolderService, never()).save(any(NoteFolder.class));
    }

    @Test
    @DisplayName("注册时两次密码不一致")
    void register_PasswordsDoNotMatch_ThrowsException() {
        // Given
        validRegisterRequest.setConfirmPassword("different_password");

        // When & Then
        assertThatThrownBy(() -> authService.register(validRegisterRequest))
                .isInstanceOf(ResponseStatusException.class)
                .hasFieldOrPropertyWithValue("status", HttpStatus.BAD_REQUEST)
                .hasMessageContaining("两次密码输入不一致");

        verify(userService, never()).save(any(User.class));
        verify(noteFolderService, never()).save(any(NoteFolder.class));
    }

    @Test
    @DisplayName("注册时用户名和邮箱会被trim")
    void register_TrimsUsernameAndEmail() {
        // Given
        validRegisterRequest.setUsername("  newuser  ");
        validRegisterRequest.setEmail("  NEWUSER@EXAMPLE.COM  ");
        // 注意：AuthServiceImpl中ensureUniqueUsername接收的是request.getUsername()（未trim）
        // 但实际业务逻辑应该检查trim后的值，这里按照现有代码逻辑mock
        when(userService.findByUsername("  newuser  ")).thenReturn(Optional.empty());
        when(userService.findByEmail("  NEWUSER@EXAMPLE.COM  ")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("password123")).thenReturn("encoded_password");
        when(userService.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId(2L);
            return true;
        });
        when(jwtUtil.generateToken(any(User.class))).thenReturn("test_token");

        // When
        authService.register(validRegisterRequest);

        // Then
        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userService).save(userCaptor.capture());
        User savedUser = userCaptor.getValue();
        assertThat(savedUser.getUsername()).isEqualTo("newuser");
        assertThat(savedUser.getEmail()).isEqualTo("newuser@example.com"); // 转小写
    }

    @Test
    @DisplayName("正常登录成功")
    void login_Success() {
        // Given
        Authentication authentication = mock(Authentication.class);
        when(authentication.getName()).thenReturn("testuser");
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(authentication);
        when(userService.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(jwtUtil.generateToken(testUser)).thenReturn("test_token");

        // When
        AuthResponse response = authService.login(validLoginRequest);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getMessage()).isEqualTo("登录成功");
        assertThat(response.getToken()).isEqualTo("test_token");

        verify(authenticationManager, times(1)).authenticate(any(UsernamePasswordAuthenticationToken.class));
        verify(userService, times(1)).findByUsername("testuser");
        verify(jwtUtil, times(1)).generateToken(testUser);
    }

    @Test
    @DisplayName("登录时密码错误")
    void login_WrongPassword_ThrowsException() {
        // Given
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new BadCredentialsException("Bad credentials"));

        // When & Then
        assertThatThrownBy(() -> authService.login(validLoginRequest))
                .isInstanceOf(BadCredentialsException.class);

        verify(userService, never()).findByUsername(anyString());
        verify(jwtUtil, never()).generateToken(any(User.class));
    }

    @Test
    @DisplayName("登录时用户不存在")
    void login_UserNotFound_ThrowsException() {
        // Given
        Authentication authentication = mock(Authentication.class);
        when(authentication.getName()).thenReturn("nonexistent");
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(authentication);
        when(userService.findByUsername("nonexistent")).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> authService.login(validLoginRequest))
                .isInstanceOf(ResponseStatusException.class)
                .hasFieldOrPropertyWithValue("status", HttpStatus.UNAUTHORIZED)
                .hasMessageContaining("用户不存在");

        verify(jwtUtil, never()).generateToken(any(User.class));
    }
}

