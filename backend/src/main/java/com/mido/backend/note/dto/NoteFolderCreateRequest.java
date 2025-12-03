package com.mido.backend.note.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class NoteFolderCreateRequest {

    @NotBlank(message = "文件夹名称不能为空")
    private String name;

    private Long parentId;
}

