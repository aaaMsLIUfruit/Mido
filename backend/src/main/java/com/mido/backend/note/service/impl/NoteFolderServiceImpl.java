package com.mido.backend.note.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.mido.backend.note.entity.NoteFolder;
import com.mido.backend.note.mapper.NoteFolderMapper;
import com.mido.backend.note.mapper.NoteMapper;
import com.mido.backend.note.service.NoteFolderService;
import com.mido.backend.note.service.dto.NoteFolderTreeNode;
import com.mido.backend.note.entity.Note;
import java.time.LocalDateTime;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class NoteFolderServiceImpl extends ServiceImpl<NoteFolderMapper, NoteFolder> implements NoteFolderService {

    private static final String DEFAULT_FOLDER_NAME = "未分类";

    private final NoteMapper noteMapper;

    @Override
    public NoteFolder createFolder(NoteFolder folder) {
        validateParent(folder.getParentId(), folder.getUserId(), null);
        folder.setIsDeleted(0);
        folder.setCreatedAt(LocalDateTime.now());
        this.save(folder);
        return folder;
    }

    @Override
    public List<NoteFolderTreeNode> getFolderTree(Long userId) {
        List<NoteFolder> folders = this.lambdaQuery()
                .eq(NoteFolder::getUserId, userId)
                .eq(NoteFolder::getIsDeleted, 0)
                .orderByAsc(NoteFolder::getCreatedAt)
                .list();
        Map<Long, NoteFolderTreeNode> nodeMap = new HashMap<>();
        List<NoteFolderTreeNode> roots = new ArrayList<>();
        for (NoteFolder folder : folders) {
            NoteFolderTreeNode node = new NoteFolderTreeNode();
            node.setId(folder.getId());
            node.setName(folder.getName());
            node.setParentId(folder.getParentId());
            node.setCreatedAt(folder.getCreatedAt());
            nodeMap.put(folder.getId(), node);
        }
        for (NoteFolder folder : folders) {
            NoteFolderTreeNode node = nodeMap.get(folder.getId());
            if (folder.getParentId() == null) {
                roots.add(node);
            } else {
                NoteFolderTreeNode parent = nodeMap.get(folder.getParentId());
                if (parent != null) {
                    parent.getChildren().add(node);
                } else {
                    roots.add(node);
                }
            }
        }
        return roots;
    }

    @Override
    public void renameFolder(Long folderId, Long userId, String name) {
        NoteFolder folder = getOwnedFolder(folderId, userId);
        folder.setName(name);
        this.updateById(folder);
    }

    @Override
    public void moveFolder(Long folderId, Long userId, Long newParentId) {
        NoteFolder folder = getOwnedFolder(folderId, userId);
        validateParent(newParentId, userId, folderId);
        folder.setParentId(newParentId);
        this.updateById(folder);
    }

    @Override
    public void deleteFolder(Long folderId, Long userId) {
        NoteFolder folder = getOwnedFolder(folderId, userId);
        List<NoteFolder> allFolders = this.lambdaQuery()
                .eq(NoteFolder::getUserId, userId)
                .list();
        Set<Long> targetIds = collectDescendantIds(folder.getId(), allFolders);
        targetIds.add(folder.getId());

        this.update(new LambdaUpdateWrapper<NoteFolder>()
                .in(NoteFolder::getId, targetIds)
                .set(NoteFolder::getIsDeleted, 1));

        noteMapper.update(null, new LambdaUpdateWrapper<Note>()
                .eq(Note::getUserId, userId)
                .in(Note::getFolderId, targetIds)
                .set(Note::getIsDeleted, 1));
    }

    private NoteFolder getOwnedFolder(Long folderId, Long userId) {
        NoteFolder folder = this.getOne(new LambdaQueryWrapper<NoteFolder>()
                .eq(NoteFolder::getId, folderId)
                .eq(NoteFolder::getUserId, userId)
                .eq(NoteFolder::getIsDeleted, 0));
        if (folder == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "文件夹不存在");
        }
        return folder;
    }

    private void validateParent(Long parentId, Long userId, Long currentFolderId) {
        if (parentId == null) {
            return;
        }
        NoteFolder parent = this.getOne(new LambdaQueryWrapper<NoteFolder>()
                .eq(NoteFolder::getId, parentId)
                .eq(NoteFolder::getUserId, userId)
                .eq(NoteFolder::getIsDeleted, 0));
        if (parent == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "父文件夹不存在");
        }
        if (isDefaultRoot(parent)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "默认文件夹下不能创建子文件夹");
        }
        if (currentFolderId != null) {
            List<NoteFolder> allFolders = this.lambdaQuery()
                    .eq(NoteFolder::getUserId, userId)
                    .list();
            Set<Long> descendants = collectDescendantIds(currentFolderId, allFolders);
            if (descendants.contains(parentId) || currentFolderId.equals(parentId)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "不能移动到子文件夹");
            }
        }
    }

    private boolean isDefaultRoot(NoteFolder folder) {
        return folder.getParentId() == null && DEFAULT_FOLDER_NAME.equals(folder.getName());
    }

    private Set<Long> collectDescendantIds(Long rootId, List<NoteFolder> folders) {
        Map<Long, List<NoteFolder>> childrenMap = new HashMap<>();
        for (NoteFolder folder : folders) {
            childrenMap.computeIfAbsent(folder.getParentId(), k -> new ArrayList<>()).add(folder);
        }
        Set<Long> result = new HashSet<>();
        Queue<Long> queue = new ArrayDeque<>();
        queue.add(rootId);
        while (!queue.isEmpty()) {
            Long current = queue.poll();
            List<NoteFolder> children = childrenMap.get(current);
            if (children == null) {
                continue;
            }
            for (NoteFolder child : children) {
                result.add(child.getId());
                queue.add(child.getId());
            }
        }
        return result;
    }
}

