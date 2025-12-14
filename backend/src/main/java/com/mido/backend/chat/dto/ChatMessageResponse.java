package com.mido.backend.chat.dto;

import com.mido.backend.chat.entity.ChatMessage;
import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ChatMessageResponse {

    private final Long id;
    private final Long chatId;
    private final String role;
    private final String content;
    private final LocalDateTime createdAt;

    public static ChatMessageResponse from(ChatMessage message) {
        return ChatMessageResponse.builder()
                .id(message.getId())
                .chatId(message.getChatId())
                .role(message.getRole())
                .content(message.getContent())
                .createdAt(message.getCreatedAt())
                .build();
    }
}

