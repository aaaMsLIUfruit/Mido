package com.mido.backend.note.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class NoteUpdateRequest {

    @NotNull(message = "笔记ID不能为空")
    private Long id;

    private Long folderId;

    private String title;

    private String content;

    private String coverUrl;
}

