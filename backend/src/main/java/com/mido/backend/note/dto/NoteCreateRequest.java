package com.mido.backend.note.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class NoteCreateRequest {

    private Long folderId;

    @NotBlank(message = "标题不能为空")
    @Size(max = 255, message = "标题长度不能超过255个字符")
    private String title;

    private String content;

    private String coverUrl;
}

