package com.mido.backend.chat.dto;

import com.mido.backend.chat.entity.Chat;
import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ChatResponse {

    private final Long id;
    private final Long folderId;
    private final String title;
    private final LocalDateTime createdAt;
    private final LocalDateTime updatedAt;

    public static ChatResponse from(Chat chat) {
        return ChatResponse.builder()
                .id(chat.getId())
                .folderId(chat.getFolderId())
                .title(chat.getTitle())
                .createdAt(chat.getCreatedAt())
                .updatedAt(chat.getUpdatedAt())
                .build();
    }
}

