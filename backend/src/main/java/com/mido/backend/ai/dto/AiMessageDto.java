package com.mido.backend.ai.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class AiMessageDto {

    @NotBlank
    private String role;

    @NotBlank
    private String content;
}

