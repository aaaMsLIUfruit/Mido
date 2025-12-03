package com.mido.backend.auth.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class LoginRequest {

    @NotBlank(message = "登录账号不能为空")
    private String identifier;

    @NotBlank(message = "密码不能为空")
    private String password;
}

