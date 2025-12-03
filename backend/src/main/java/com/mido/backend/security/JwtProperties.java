package com.mido.backend.security;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "security.jwt")
public class JwtProperties {

    @NotBlank(message = "JWT 密钥不能为空")
    @Size(min = 32, message = "JWT 密钥长度至少 32 位")
    private String secret;

    @Min(value = 1, message = "JWT 过期时间必须大于 0")
    private long expirationMinutes = 60;

    public String getSecret() {
        return secret;
    }

    public void setSecret(String secret) {
        this.secret = secret;
    }

    public long getExpirationMinutes() {
        return expirationMinutes;
    }

    public void setExpirationMinutes(long expirationMinutes) {
        this.expirationMinutes = expirationMinutes;
    }
}

