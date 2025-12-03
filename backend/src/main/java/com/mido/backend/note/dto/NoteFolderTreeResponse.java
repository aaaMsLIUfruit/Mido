package com.mido.backend.note.dto;

import com.mido.backend.note.service.dto.NoteFolderTreeNode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class NoteFolderTreeResponse {

    private Long id;
    private String name;
    private Long parentId;
    private LocalDateTime createdAt;
    private List<NoteFolderTreeResponse> children;

    public static NoteFolderTreeResponse from(NoteFolderTreeNode node) {
        NoteFolderTreeResponse response = new NoteFolderTreeResponse();
        response.setId(node.getId());
        response.setName(node.getName());
        response.setParentId(node.getParentId());
        response.setCreatedAt(node.getCreatedAt());
        response.setChildren(node.getChildren().stream()
                .map(NoteFolderTreeResponse::from)
                .collect(Collectors.toList()));
        return response;
    }
}

