package com.mido.backend.chat.dto;

import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ChatCreateRequest {

    private Long folderId;

    @Size(max = 200, message = "标题长度不能超过200个字符")
    private String title;
}

