package com.mido.backend.note.service.dto;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.Data;

@Data
public class NoteFolderTreeNode {

    private Long id;
    private String name;
    private Long parentId;
    private LocalDateTime createdAt;
    private List<NoteFolderTreeNode> children = new ArrayList<>();
}

