package com.mido.backend.chat.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.mido.backend.chat.entity.Chat;
import com.mido.backend.chat.entity.ChatFolder;
import com.mido.backend.chat.mapper.ChatFolderMapper;
import com.mido.backend.chat.mapper.ChatMapper;
import com.mido.backend.chat.service.ChatService;
import java.time.LocalDateTime;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class ChatServiceImpl extends ServiceImpl<ChatMapper, Chat> implements ChatService {

    private final ChatFolderMapper chatFolderMapper;

    @Override
    public Chat createChat(Chat chat) {
        Long folderId = chat.getFolderId();
        if (folderId != null) {
            validateFolderOwnership(folderId, chat.getUserId());
        }
        chat.setIsDeleted(0);
        LocalDateTime now = LocalDateTime.now();
        chat.setCreatedAt(now);
        chat.setUpdatedAt(now);
        this.save(chat);
        return chat;
    }

    @Override
    public IPage<Chat> listChats(Long userId, long page, long size) {
        return this.lambdaQuery()
                .eq(Chat::getUserId, userId)
                .eq(Chat::getIsDeleted, 0)
                .orderByDesc(Chat::getUpdatedAt)
                .page(new Page<>(page, size));
    }

    @Override
    public IPage<Chat> listChatsByFolder(Long userId, Long folderId, long page, long size) {
        LambdaQueryWrapper<Chat> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Chat::getUserId, userId)
                .eq(Chat::getIsDeleted, 0)
                .orderByDesc(Chat::getUpdatedAt);
        
        // 注意：如果数据库表中没有folder_id字段，需要先添加该字段
        // 或者暂时注释掉这部分逻辑
        if (folderId != null) {
            wrapper.eq(Chat::getFolderId, folderId);
        } else {
            // 查询folder_id为null的记录（未分类的对话）
            wrapper.isNull(Chat::getFolderId);
        }
        
        return this.page(new Page<>(page, size), wrapper);
    }

    @Override
    public Chat updateChat(Chat payload, Long userId) {
        Chat existing = this.getById(payload.getId());
        if (existing == null || !existing.getUserId().equals(userId) || existing.getIsDeleted() == 1) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "对话不存在");
        }

        if (payload.getFolderId() != null) {
            validateFolderOwnership(payload.getFolderId(), userId);
            existing.setFolderId(payload.getFolderId());
        }
        if (payload.getTitle() != null) {
            existing.setTitle(payload.getTitle());
        }
        existing.setUpdatedAt(LocalDateTime.now());
        this.updateById(existing);
        return existing;
    }

    @Override
    public void softDelete(Long chatId, Long userId) {
        Chat existing = this.getById(chatId);
        if (existing == null || !existing.getUserId().equals(userId) || existing.getIsDeleted() == 1) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "对话不存在");
        }
        existing.setIsDeleted(1);
        existing.setUpdatedAt(LocalDateTime.now());
        this.updateById(existing);
    }

    @Override
    public Optional<Chat> findByIdAndUser(Long id, Long userId) {
        LambdaQueryWrapper<Chat> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Chat::getId, id)
                .eq(Chat::getUserId, userId)
                .eq(Chat::getIsDeleted, 0);
        return Optional.ofNullable(this.getOne(wrapper));
    }

    private void validateFolderOwnership(Long folderId, Long userId) {
        if (folderId == null) {
            return;
        }
        ChatFolder folder = chatFolderMapper.selectOne(new LambdaQueryWrapper<ChatFolder>()
                .eq(ChatFolder::getId, folderId)
                .eq(ChatFolder::getUserId, userId)
                .eq(ChatFolder::getIsDeleted, 0));
        if (folder == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "文件夹不存在");
        }
    }
}

