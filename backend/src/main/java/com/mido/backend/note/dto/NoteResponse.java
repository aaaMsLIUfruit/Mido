package com.mido.backend.note.dto;

import com.mido.backend.note.entity.Note;
import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class NoteResponse {

    private final Long id;
    private final Long folderId;
    private final String title;
    private final String content;
    private final String coverUrl;
    private final LocalDateTime createdAt;
    private final LocalDateTime updatedAt;

    public static NoteResponse from(Note note) {
        return NoteResponse.builder()
                .id(note.getId())
                .folderId(note.getFolderId())
                .title(note.getTitle())
                .content(note.getContent())
                .coverUrl(note.getCoverUrl())
                .createdAt(note.getCreatedAt())
                .updatedAt(note.getUpdatedAt())
                .build();
    }
}

