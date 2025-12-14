package com.mido.backend.chat.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.mido.backend.chat.entity.Chat;
import com.mido.backend.chat.entity.ChatFolder;
import com.mido.backend.chat.mapper.ChatFolderMapper;
import com.mido.backend.chat.mapper.ChatMapper;
import com.mido.backend.chat.service.ChatFolderService;
import com.mido.backend.chat.service.dto.ChatFolderTreeNode;
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
public class ChatFolderServiceImpl extends ServiceImpl<ChatFolderMapper, ChatFolder> implements ChatFolderService {

    private static final String DEFAULT_FOLDER_NAME = "未分类";

    private final ChatMapper chatMapper;

    @Override
    public ChatFolder createFolder(ChatFolder folder) {
        validateParent(folder.getParentId(), folder.getUserId(), null);
        folder.setIsDeleted(0);
        folder.setCreatedAt(LocalDateTime.now());
        this.save(folder);
        return folder;
    }

    @Override
    public List<ChatFolderTreeNode> getFolderTree(Long userId) {
        List<ChatFolder> folders = this.lambdaQuery()
                .eq(ChatFolder::getUserId, userId)
                .eq(ChatFolder::getIsDeleted, 0)
                .orderByAsc(ChatFolder::getCreatedAt)
                .list();
        Map<Long, ChatFolderTreeNode> nodeMap = new HashMap<>();
        List<ChatFolderTreeNode> roots = new ArrayList<>();
        for (ChatFolder folder : folders) {
            ChatFolderTreeNode node = new ChatFolderTreeNode();
            node.setId(folder.getId());
            node.setName(folder.getName());
            node.setParentId(folder.getParentId());
            node.setCreatedAt(folder.getCreatedAt());
            nodeMap.put(folder.getId(), node);
        }
        for (ChatFolder folder : folders) {
            ChatFolderTreeNode node = nodeMap.get(folder.getId());
            if (folder.getParentId() == null) {
                roots.add(node);
            } else {
                ChatFolderTreeNode parent = nodeMap.get(folder.getParentId());
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
        ChatFolder folder = getOwnedFolder(folderId, userId);
        folder.setName(name);
        this.updateById(folder);
    }

    @Override
    public void moveFolder(Long folderId, Long userId, Long newParentId) {
        ChatFolder folder = getOwnedFolder(folderId, userId);
        validateParent(newParentId, userId, folderId);
        folder.setParentId(newParentId);
        this.updateById(folder);
    }

    @Override
    public void deleteFolder(Long folderId, Long userId) {
        ChatFolder folder = getOwnedFolder(folderId, userId);
        List<ChatFolder> allFolders = this.lambdaQuery()
                .eq(ChatFolder::getUserId, userId)
                .list();
        Set<Long> targetIds = collectDescendantIds(folder.getId(), allFolders);
        targetIds.add(folder.getId());

        this.update(new LambdaUpdateWrapper<ChatFolder>()
                .in(ChatFolder::getId, targetIds)
                .set(ChatFolder::getIsDeleted, 1));

        chatMapper.update(null, new LambdaUpdateWrapper<Chat>()
                .eq(Chat::getUserId, userId)
                .in(Chat::getFolderId, targetIds)
                .set(Chat::getIsDeleted, 1));
    }

    private ChatFolder getOwnedFolder(Long folderId, Long userId) {
        ChatFolder folder = this.getOne(new LambdaQueryWrapper<ChatFolder>()
                .eq(ChatFolder::getId, folderId)
                .eq(ChatFolder::getUserId, userId)
                .eq(ChatFolder::getIsDeleted, 0));
        if (folder == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "文件夹不存在");
        }
        return folder;
    }

    private void validateParent(Long parentId, Long userId, Long currentFolderId) {
        if (parentId == null) {
            return;
        }
        ChatFolder parent = this.getOne(new LambdaQueryWrapper<ChatFolder>()
                .eq(ChatFolder::getId, parentId)
                .eq(ChatFolder::getUserId, userId)
                .eq(ChatFolder::getIsDeleted, 0));
        if (parent == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "父文件夹不存在");
        }
        if (isDefaultRoot(parent)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "默认文件夹下不能创建子文件夹");
        }
        if (currentFolderId != null) {
            List<ChatFolder> allFolders = this.lambdaQuery()
                    .eq(ChatFolder::getUserId, userId)
                    .list();
            Set<Long> descendants = collectDescendantIds(currentFolderId, allFolders);
            if (descendants.contains(parentId) || currentFolderId.equals(parentId)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "不能移动到子文件夹");
            }
        }
    }

    private boolean isDefaultRoot(ChatFolder folder) {
        return folder.getParentId() == null && DEFAULT_FOLDER_NAME.equals(folder.getName());
    }

    private Set<Long> collectDescendantIds(Long rootId, List<ChatFolder> folders) {
        Map<Long, List<ChatFolder>> childrenMap = new HashMap<>();
        for (ChatFolder folder : folders) {
            childrenMap.computeIfAbsent(folder.getParentId(), k -> new ArrayList<>()).add(folder);
        }
        Set<Long> result = new HashSet<>();
        Queue<Long> queue = new ArrayDeque<>();
        queue.add(rootId);
        while (!queue.isEmpty()) {
            Long current = queue.poll();
            List<ChatFolder> children = childrenMap.get(current);
            if (children == null) {
                continue;
            }
            for (ChatFolder child : children) {
                result.add(child.getId());
                queue.add(child.getId());
            }
        }
        return result;
    }
}

