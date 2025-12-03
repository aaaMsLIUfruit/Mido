package com.mido.backend.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.mido.backend.common.dto.PageResponse;
import com.mido.backend.note.dto.NoteCreateRequest;
import com.mido.backend.note.dto.NoteResponse;
import com.mido.backend.note.dto.NoteUpdateRequest;
import com.mido.backend.note.entity.Note;
import com.mido.backend.note.service.NoteService;
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
@RequestMapping("/api/note")
@Validated
@RequiredArgsConstructor
public class NoteController {

    private final NoteService noteService;
    private final UserService userService;

    @PostMapping("/create")
    public ResponseEntity<NoteResponse> create(@Valid @RequestBody NoteCreateRequest request) {
        Long userId = currentUserId();
        Note note = new Note();
        note.setUserId(userId);
        note.setFolderId(request.getFolderId());
        note.setTitle(request.getTitle());
        note.setContent(request.getContent());
        note.setCoverUrl(request.getCoverUrl());
        Note saved = noteService.createNote(note);
        return ResponseEntity.status(HttpStatus.CREATED).body(NoteResponse.from(saved));
    }

    @GetMapping("/list")
    public PageResponse<NoteResponse> list(
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "10") long size,
            @RequestParam(required = false) Long folderId) {
        Long userId = currentUserId();
        IPage<Note> notesPage = folderId == null
                ? noteService.listNotes(userId, page, size)
                : noteService.listNotesByFolder(userId, folderId, page, size);
        List<NoteResponse> records = notesPage.getRecords()
                .stream()
                .map(NoteResponse::from)
                .collect(Collectors.toList());
        return new PageResponse<>(notesPage.getCurrent(), notesPage.getSize(), notesPage.getTotal(), records);
    }

    @GetMapping("/{id}")
    public ResponseEntity<NoteResponse> detail(@PathVariable Long id) {
        Long userId = currentUserId();
        Note note = noteService.findByIdAndUser(id, userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "笔记不存在"));
        return ResponseEntity.ok(NoteResponse.from(note));
    }

    @PutMapping("/update")
    public ResponseEntity<NoteResponse> update(@Valid @RequestBody NoteUpdateRequest request) {
        Long userId = currentUserId();
        Note payload = new Note();
        payload.setId(request.getId());
        payload.setFolderId(request.getFolderId());
        payload.setTitle(request.getTitle());
        payload.setContent(request.getContent());
        payload.setCoverUrl(request.getCoverUrl());
        Note updated = noteService.updateNote(payload, userId);
        return ResponseEntity.ok(NoteResponse.from(updated));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        Long userId = currentUserId();
        noteService.softDelete(id, userId);
    }

    private Long currentUserId() {
        String username = SecurityUtils.getCurrentUsername()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "未登录"));
        User user = userService.findByUsername(username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "用户不存在"));
        return user.getId();
    }
}

