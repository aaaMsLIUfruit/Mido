package com.mido.backend.note.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class NoteFolderRenameRequest {

    @NotNull(message = "文件夹ID不能为空")
    private Long id;

    @NotBlank(message = "文件夹名称不能为空")
    private String name;
}

