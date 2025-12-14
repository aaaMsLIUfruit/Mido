package com.mido.backend.chat.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ChatUpdateRequest {

    @NotNull(message = "ID不能为空")
    private Long id;

    private Long folderId;

    @Size(max = 200, message = "标题长度不能超过200个字符")
    private String title;
}

