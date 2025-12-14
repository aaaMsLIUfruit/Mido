package com.mido.backend.chat.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ChatFolderCreateRequest {

    private Long parentId;

    @NotBlank(message = "文件夹名称不能为空")
    @Size(max = 100, message = "文件夹名称长度不能超过100个字符")
    private String name;
}

