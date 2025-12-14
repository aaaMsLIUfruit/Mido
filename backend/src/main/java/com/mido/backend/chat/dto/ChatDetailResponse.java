package com.mido.backend.chat.dto;

import com.mido.backend.chat.entity.Chat;
import java.time.LocalDateTime;
import java.util.List;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ChatDetailResponse {

    private final Long id;
    private final Long folderId;
    private final String title;
    private final LocalDateTime createdAt;
    private final LocalDateTime updatedAt;
    private final List<ChatMessageResponse> messages;

    public static ChatDetailResponse from(Chat chat, List<ChatMessageResponse> messages) {
        return ChatDetailResponse.builder()
                .id(chat.getId())
                .folderId(chat.getFolderId())
                .title(chat.getTitle())
                .createdAt(chat.getCreatedAt())
                .updatedAt(chat.getUpdatedAt())
                .messages(messages)
                .build();
    }
}

