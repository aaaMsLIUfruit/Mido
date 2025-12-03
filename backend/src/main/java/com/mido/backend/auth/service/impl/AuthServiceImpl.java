package com.mido.backend.auth.service.impl;

import com.mido.backend.auth.dto.AuthResponse;
import com.mido.backend.auth.dto.LoginRequest;
import com.mido.backend.auth.dto.RegisterRequest;
import com.mido.backend.auth.service.AuthService;
import com.mido.backend.security.JwtUtil;
import com.mido.backend.user.entity.User;
import com.mido.backend.user.service.UserService;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserService userService;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtUtil jwtUtil;

    @Override
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        validatePasswords(request);
        ensureUniqueUsername(request.getUsername());
        ensureUniqueEmail(request.getEmail());

        User user = new User();
        user.setUsername(request.getUsername().trim());
        user.setEmail(request.getEmail().trim().toLowerCase());
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setCreatedAt(LocalDateTime.now());

        userService.save(user);
        String token = jwtUtil.generateToken(user);
        return AuthResponse.builder()
                .message("注册成功")
                .token(token)
                .build();
    }

    @Override
    public AuthResponse login(LoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getIdentifier(), request.getPassword()));
        String username = authentication.getName();
        User user = userService.findByUsername(username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "用户不存在"));
        String token = jwtUtil.generateToken(user);
        return AuthResponse.builder()
                .message("登录成功")
                .token(token)
                .build();
    }

    private void validatePasswords(RegisterRequest request) {
        if (!request.getPassword().equals(request.getConfirmPassword())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "两次密码输入不一致");
        }
    }

    private void ensureUniqueUsername(String username) {
        if (userService.findByUsername(username).isPresent()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "用户名已存在");
        }
    }

    private void ensureUniqueEmail(String email) {
        if (userService.findByEmail(email).isPresent()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "邮箱已被注册");
        }
    }
}

