package com.mido.backend.chat.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ChatFolderMoveRequest {

    @NotNull(message = "ID不能为空")
    private Long id;

    private Long newParentId;
}

