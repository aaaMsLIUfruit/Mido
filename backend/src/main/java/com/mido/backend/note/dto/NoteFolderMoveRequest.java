package com.mido.backend.note.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class NoteFolderMoveRequest {

    @NotNull(message = "文件夹ID不能为空")
    private Long id;

    private Long newParentId;
}

