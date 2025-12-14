package com.mido.backend.chat.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.mido.backend.chat.entity.ChatFolder;
import com.mido.backend.chat.service.dto.ChatFolderTreeNode;
import java.util.List;

public interface ChatFolderService extends IService<ChatFolder> {

    ChatFolder createFolder(ChatFolder folder);

    List<ChatFolderTreeNode> getFolderTree(Long userId);

    void renameFolder(Long folderId, Long userId, String name);

    void moveFolder(Long folderId, Long userId, Long newParentId);

    void deleteFolder(Long folderId, Long userId);
}

