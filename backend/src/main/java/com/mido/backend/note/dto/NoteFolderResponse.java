package com.mido.backend.note.dto;

import com.mido.backend.note.entity.NoteFolder;
import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class NoteFolderResponse {

    private final Long id;
    private final String name;
    private final Long parentId;
    private final LocalDateTime createdAt;

    public static NoteFolderResponse from(NoteFolder folder) {
        return NoteFolderResponse.builder()
                .id(folder.getId())
                .name(folder.getName())
                .parentId(folder.getParentId())
                .createdAt(folder.getCreatedAt())
                .build();
    }
}

