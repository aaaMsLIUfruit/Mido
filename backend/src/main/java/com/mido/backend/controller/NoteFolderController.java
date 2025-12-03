package com.mido.backend.controller;

import com.mido.backend.note.dto.NoteFolderCreateRequest;
import com.mido.backend.note.dto.NoteFolderMoveRequest;
import com.mido.backend.note.dto.NoteFolderRenameRequest;
import com.mido.backend.note.dto.NoteFolderResponse;
import com.mido.backend.note.dto.NoteFolderTreeResponse;
import com.mido.backend.note.entity.NoteFolder;
import com.mido.backend.note.service.NoteFolderService;
import com.mido.backend.note.service.dto.NoteFolderTreeNode;
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
@RequestMapping("/api/note/folder")
@Validated
@RequiredArgsConstructor
public class NoteFolderController {

    private final NoteFolderService noteFolderService;
    private final UserService userService;

    @PostMapping("/create")
    public ResponseEntity<NoteFolderResponse> create(@Valid @RequestBody NoteFolderCreateRequest request) {
        Long userId = currentUserId();
        NoteFolder folder = new NoteFolder();
        folder.setUserId(userId);
        folder.setName(request.getName());
        folder.setParentId(request.getParentId());
        NoteFolder saved = noteFolderService.createFolder(folder);
        return ResponseEntity.status(HttpStatus.CREATED).body(NoteFolderResponse.from(saved));
    }

    @GetMapping("/tree")
    public List<NoteFolderTreeResponse> tree() {
        Long userId = currentUserId();
        List<NoteFolderTreeNode> nodes = noteFolderService.getFolderTree(userId);
        return nodes.stream().map(NoteFolderTreeResponse::from).collect(Collectors.toList());
    }

    @PostMapping("/rename")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void rename(@Valid @RequestBody NoteFolderRenameRequest request) {
        Long userId = currentUserId();
        noteFolderService.renameFolder(request.getId(), userId, request.getName());
    }

    @PostMapping("/move")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void move(@Valid @RequestBody NoteFolderMoveRequest request) {
        Long userId = currentUserId();
        noteFolderService.moveFolder(request.getId(), userId, request.getNewParentId());
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        Long userId = currentUserId();
        noteFolderService.deleteFolder(id, userId);
    }

    private Long currentUserId() {
        String username = SecurityUtils.getCurrentUsername()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "未登录"));
        User user = userService.findByUsername(username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "用户不存在"));
        return user.getId();
    }
}

