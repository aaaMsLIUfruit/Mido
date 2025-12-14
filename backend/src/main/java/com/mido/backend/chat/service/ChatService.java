package com.mido.backend.chat.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.mido.backend.chat.entity.Chat;
import java.util.Optional;

public interface ChatService extends IService<Chat> {

    Chat createChat(Chat chat);

    IPage<Chat> listChats(Long userId, long page, long size);

    IPage<Chat> listChatsByFolder(Long userId, Long folderId, long page, long size);

    Chat updateChat(Chat chat, Long userId);

    void softDelete(Long chatId, Long userId);

    Optional<Chat> findByIdAndUser(Long id, Long userId);
}

