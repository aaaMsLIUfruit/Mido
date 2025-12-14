package com.mido.backend.chat.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.mido.backend.chat.entity.ChatMessage;
import com.mido.backend.chat.mapper.ChatMessageMapper;
import com.mido.backend.chat.service.ChatMessageService;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ChatMessageServiceImpl extends ServiceImpl<ChatMessageMapper, ChatMessage> implements ChatMessageService {

    @Override
    public ChatMessage createMessage(ChatMessage message) {
        message.setCreatedAt(LocalDateTime.now());
        this.save(message);
        return message;
    }

    @Override
    public List<ChatMessage> listMessagesByChatId(Long chatId) {
        return this.lambdaQuery()
                .eq(ChatMessage::getChatId, chatId)
                .orderByAsc(ChatMessage::getCreatedAt)
                .list();
    }

    @Override
    public void deleteMessagesByChatId(Long chatId) {
        this.remove(new LambdaQueryWrapper<ChatMessage>()
                .eq(ChatMessage::getChatId, chatId));
    }
}

