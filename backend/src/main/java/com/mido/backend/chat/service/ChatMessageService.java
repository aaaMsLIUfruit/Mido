package com.mido.backend.chat.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.mido.backend.chat.entity.ChatMessage;
import java.util.List;

public interface ChatMessageService extends IService<ChatMessage> {

    ChatMessage createMessage(ChatMessage message);

    List<ChatMessage> listMessagesByChatId(Long chatId);

    void deleteMessagesByChatId(Long chatId);
}

