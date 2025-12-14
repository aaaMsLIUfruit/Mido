package com.mido.backend.chat.service.dto;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ChatFolderTreeNode {

    private Long id;
    private String name;
    private Long parentId;
    private LocalDateTime createdAt;
    private List<ChatFolderTreeNode> children = new ArrayList<>();
}

