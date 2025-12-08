package com.mido.backend.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "ai.deepseek")
public class AiProperties {

    private String apiKey;
    private String baseUrl = "https://api.deepseek.com";
    private String chatPath = "/chat/completions";
    private String model = "deepseek-chat";
    private Double temperature = 0.6;
}

