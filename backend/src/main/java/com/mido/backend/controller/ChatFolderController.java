package com.mido.backend.controller;

import com.mido.backend.chat.dto.ChatFolderCreateRequest;
import com.mido.backend.chat.dto.ChatFolderMoveRequest;
import com.mido.backend.chat.dto.ChatFolderRenameRequest;
import com.mido.backend.chat.dto.ChatFolderResponse;
import com.mido.backend.chat.dto.ChatFolderTreeResponse;
import com.mido.backend.chat.entity.ChatFolder;
import com.mido.backend.chat.service.ChatFolderService;
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
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/chat/folder")
@Validated
@RequiredArgsConstructor
public class ChatFolderController {

    private final ChatFolderService chatFolderService;
    private final UserService userService;

    @PostMapping("/create")
    public ResponseEntity<ChatFolderResponse> create(@Valid @RequestBody ChatFolderCreateRequest request) {
        Long userId = currentUserId();
        ChatFolder folder = new ChatFolder();
        folder.setUserId(userId);
        folder.setName(request.getName());
        folder.setParentId(request.getParentId());
        ChatFolder saved = chatFolderService.createFolder(folder);
        return ResponseEntity.status(HttpStatus.CREATED).body(ChatFolderResponse.from(saved));
    }

    @GetMapping("/tree")
    public List<ChatFolderTreeResponse> tree() {
        Long userId = currentUserId();
        return chatFolderService.getFolderTree(userId)
                .stream()
                .map(ChatFolderTreeResponse::from)
                .collect(Collectors.toList());
    }

    @PostMapping("/rename")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void rename(@Valid @RequestBody ChatFolderRenameRequest request) {
        Long userId = currentUserId();
        chatFolderService.renameFolder(request.getId(), userId, request.getName());
    }

    @PostMapping("/move")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void move(@Valid @RequestBody ChatFolderMoveRequest request) {
        Long userId = currentUserId();
        chatFolderService.moveFolder(request.getId(), userId, request.getNewParentId());
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        Long userId = currentUserId();
        chatFolderService.deleteFolder(id, userId);
    }

    private Long currentUserId() {
        String username = SecurityUtils.getCurrentUsername()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "未登录"));
        User user = userService.findByUsername(username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "用户不存在"));
        return user.getId();
    }
}

