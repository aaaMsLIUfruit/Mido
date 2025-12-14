package com.mido.backend.chat.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ChatMessageCreateRequest {

    @NotNull(message = "聊天ID不能为空")
    private Long chatId;

    @NotBlank(message = "角色不能为空")
    private String role;

    @NotBlank(message = "内容不能为空")
    private String content;
}

