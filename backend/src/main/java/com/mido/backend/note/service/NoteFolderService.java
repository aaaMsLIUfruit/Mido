package com.mido.backend.note.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.mido.backend.note.entity.NoteFolder;
import com.mido.backend.note.service.dto.NoteFolderTreeNode;
import java.util.List;

public interface NoteFolderService extends IService<NoteFolder> {

    NoteFolder createFolder(NoteFolder folder);

    List<NoteFolderTreeNode> getFolderTree(Long userId);

    void renameFolder(Long folderId, Long userId, String name);

    void moveFolder(Long folderId, Long userId, Long newParentId);

    void deleteFolder(Long folderId, Long userId);
}

