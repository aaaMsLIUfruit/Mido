package com.mido.backend.auth.service;

import com.mido.backend.auth.dto.AuthResponse;
import com.mido.backend.auth.dto.LoginRequest;
import com.mido.backend.auth.dto.RegisterRequest;

public interface AuthService {

    AuthResponse register(RegisterRequest request);

    AuthResponse login(LoginRequest request);
}

