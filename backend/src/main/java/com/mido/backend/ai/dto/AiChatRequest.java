package com.mido.backend.ai.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;
import lombok.Data;

@Data
public class AiChatRequest {

    @NotEmpty
    @Valid
    private List<AiMessageDto> messages;
}

