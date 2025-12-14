package com.mido.backend.chat.dto;

import com.mido.backend.chat.entity.ChatFolder;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ChatFolderResponse {

    private Long id;
    private String name;
    private Long parentId;
    private LocalDateTime createdAt;

    public static ChatFolderResponse from(ChatFolder folder) {
        ChatFolderResponse response = new ChatFolderResponse();
        response.setId(folder.getId());
        response.setName(folder.getName());
        response.setParentId(folder.getParentId());
        response.setCreatedAt(folder.getCreatedAt());
        return response;
    }
}

