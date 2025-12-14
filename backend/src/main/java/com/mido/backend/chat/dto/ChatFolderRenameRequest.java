package com.mido.backend.chat.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ChatFolderRenameRequest {

    @NotNull(message = "ID不能为空")
    private Long id;

    @Size(max = 100, message = "文件夹名称长度不能超过100个字符")
    private String name;
}

