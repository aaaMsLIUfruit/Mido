package com.mido.backend.controller;

import com.mido.backend.ai.dto.AiChatRequest;
import com.mido.backend.ai.dto.AiChatResponse;
import com.mido.backend.ai.service.AiService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
public class AiController {

    private final AiService aiService;

    @PostMapping("/chat")
    public AiChatResponse chat(@Valid @RequestBody AiChatRequest request) {
        String answer = aiService.chat(request.getMessages());
        return new AiChatResponse(answer);
    }
}

