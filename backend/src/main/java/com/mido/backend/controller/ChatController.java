package com.mido.backend.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.mido.backend.chat.dto.ChatCreateRequest;
import com.mido.backend.chat.dto.ChatDetailResponse;
import com.mido.backend.chat.dto.ChatMessageCreateRequest;
import com.mido.backend.chat.dto.ChatMessageResponse;
import com.mido.backend.chat.dto.ChatResponse;
import com.mido.backend.chat.dto.ChatUpdateRequest;
import com.mido.backend.chat.entity.Chat;
import com.mido.backend.chat.entity.ChatMessage;
import com.mido.backend.chat.service.ChatMessageService;
import com.mido.backend.chat.service.ChatService;
import com.mido.backend.common.dto.PageResponse;
import com.mido.backend.security.SecurityUtils;
import com.mido.backend.user.entity.User;
import com.mido.backend.user.service.UserService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/chat")
@Validated
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;
    private final ChatMessageService chatMessageService;
    private final UserService userService;

    @PostMapping("/create")
    public ResponseEntity<ChatResponse> create(@Valid @RequestBody ChatCreateRequest request) {
        Long userId = currentUserId();
        Chat chat = new Chat();
        chat.setUserId(userId);
        chat.setFolderId(request.getFolderId());
        chat.setTitle(request.getTitle());
        Chat saved = chatService.createChat(chat);
        return ResponseEntity.status(HttpStatus.CREATED).body(ChatResponse.from(saved));
    }

    @GetMapping("/list")
    public PageResponse<ChatResponse> list(
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "100") long size,
            @RequestParam(required = false) Long folderId) {
        Long userId = currentUserId();
        IPage<Chat> chatsPage = folderId == null
                ? chatService.listChats(userId, page, size)
                : chatService.listChatsByFolder(userId, folderId, page, size);
        List<ChatResponse> records = chatsPage.getRecords()
                .stream()
                .map(ChatResponse::from)
                .collect(Collectors.toList());
        return new PageResponse<>(chatsPage.getCurrent(), chatsPage.getSize(), chatsPage.getTotal(), records);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ChatDetailResponse> detail(@PathVariable Long id) {
        Long userId = currentUserId();
        Chat chat = chatService.findByIdAndUser(id, userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "对话不存在"));
        List<ChatMessageResponse> messages = chatMessageService.listMessagesByChatId(id)
                .stream()
                .map(ChatMessageResponse::from)
                .collect(Collectors.toList());
        return ResponseEntity.ok(ChatDetailResponse.from(chat, messages));
    }

    @PutMapping("/update")
    public ResponseEntity<ChatResponse> update(@Valid @RequestBody ChatUpdateRequest request) {
        Long userId = currentUserId();
        Chat payload = new Chat();
        payload.setId(request.getId());
        payload.setFolderId(request.getFolderId());
        payload.setTitle(request.getTitle());
        Chat updated = chatService.updateChat(payload, userId);
        return ResponseEntity.ok(ChatResponse.from(updated));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        Long userId = currentUserId();
        chatService.softDelete(id, userId);
    }

    @PostMapping("/message/create")
    public ResponseEntity<ChatMessageResponse> createMessage(@Valid @RequestBody ChatMessageCreateRequest request) {
        Long userId = currentUserId();
        Chat chat = chatService.findByIdAndUser(request.getChatId(), userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "对话不存在"));
        
        ChatMessage message = new ChatMessage();
        message.setChatId(request.getChatId());
        message.setRole(request.getRole());
        message.setContent(request.getContent());
        ChatMessage saved = chatMessageService.createMessage(message);
        
        chat.setUpdatedAt(java.time.LocalDateTime.now());
        chatService.updateById(chat);
        
        return ResponseEntity.status(HttpStatus.CREATED).body(ChatMessageResponse.from(saved));
    }

    @GetMapping("/{id}/messages")
    public ResponseEntity<List<ChatMessageResponse>> listMessages(@PathVariable Long id) {
        Long userId = currentUserId();
        Chat chat = chatService.findByIdAndUser(id, userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "对话不存在"));
        List<ChatMessageResponse> messages = chatMessageService.listMessagesByChatId(id)
                .stream()
                .map(ChatMessageResponse::from)
                .collect(Collectors.toList());
        return ResponseEntity.ok(messages);
    }

    private Long currentUserId() {
        String username = SecurityUtils.getCurrentUsername()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "未登录"));
        User user = userService.findByUsername(username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "用户不存在"));
        return user.getId();
    }
}

