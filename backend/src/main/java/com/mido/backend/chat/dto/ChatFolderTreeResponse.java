package com.mido.backend.chat.dto;

import com.mido.backend.chat.service.dto.ChatFolderTreeNode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ChatFolderTreeResponse {

    private Long id;
    private String name;
    private Long parentId;
    private LocalDateTime createdAt;
    private List<ChatFolderTreeResponse> children;

    public static ChatFolderTreeResponse from(ChatFolderTreeNode node) {
        ChatFolderTreeResponse response = new ChatFolderTreeResponse();
        response.setId(node.getId());
        response.setName(node.getName());
        response.setParentId(node.getParentId());
        response.setCreatedAt(node.getCreatedAt());
        response.setChildren(node.getChildren().stream()
                .map(ChatFolderTreeResponse::from)
                .collect(Collectors.toList()));
        return response;
    }
}

