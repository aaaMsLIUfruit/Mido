package com.mido.backend.note.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.mido.backend.note.entity.Note;
import com.mido.backend.note.entity.NoteFolder;
import com.mido.backend.note.mapper.NoteFolderMapper;
import com.mido.backend.note.mapper.NoteMapper;
import com.mido.backend.note.service.NoteService;
import java.time.LocalDateTime;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class NoteServiceImpl extends ServiceImpl<NoteMapper, Note> implements NoteService {

    private final NoteFolderMapper noteFolderMapper;

    @Override
    public Note createNote(Note note) {
        validateFolderOwnership(note.getFolderId(), note.getUserId());
        note.setIsDeleted(0);
        LocalDateTime now = LocalDateTime.now();
        note.setCreatedAt(now);
        note.setUpdatedAt(now);
        this.save(note);
        return note;
    }

    @Override
    public IPage<Note> listNotes(Long userId, long page, long size) {
        return this.lambdaQuery()
                .eq(Note::getUserId, userId)
                .eq(Note::getIsDeleted, 0)
                .orderByDesc(Note::getUpdatedAt)
                .page(new Page<>(page, size));
    }

    @Override
    public IPage<Note> listNotesByFolder(Long userId, Long folderId, long page, long size) {
        return this.lambdaQuery()
                .eq(Note::getUserId, userId)
                .eq(Note::getIsDeleted, 0)
                .eq(folderId != null, Note::getFolderId, folderId)
                .orderByDesc(Note::getUpdatedAt)
                .page(new Page<>(page, size));
    }

    @Override
    public Note updateNote(Note payload, Long userId) {
        Note existing = this.getById(payload.getId());
        if (existing == null || !existing.getUserId().equals(userId) || existing.getIsDeleted() == 1) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "笔记不存在");
        }

        if (payload.getFolderId() != null) {
            validateFolderOwnership(payload.getFolderId(), userId);
            existing.setFolderId(payload.getFolderId());
        }
        if (payload.getTitle() != null) {
            existing.setTitle(payload.getTitle());
        }
        if (payload.getContent() != null) {
            existing.setContent(payload.getContent());
        }
        if (payload.getCoverUrl() != null) {
            existing.setCoverUrl(payload.getCoverUrl());
        }
        existing.setUpdatedAt(LocalDateTime.now());
        this.updateById(existing);
        return existing;
    }

    @Override
    public void softDelete(Long noteId, Long userId) {
        Note existing = this.getById(noteId);
        if (existing == null || !existing.getUserId().equals(userId) || existing.getIsDeleted() == 1) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "笔记不存在");
        }
        existing.setIsDeleted(1);
        existing.setUpdatedAt(LocalDateTime.now());
        this.updateById(existing);
    }

    @Override
    public Optional<Note> findByIdAndUser(Long id, Long userId) {
        LambdaQueryWrapper<Note> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Note::getId, id)
                .eq(Note::getUserId, userId)
                .eq(Note::getIsDeleted, 0);
        return Optional.ofNullable(this.getOne(wrapper));
    }

    private void validateFolderOwnership(Long folderId, Long userId) {
        if (folderId == null) {
            return;
        }
        NoteFolder folder = noteFolderMapper.selectOne(new LambdaQueryWrapper<NoteFolder>()
                .eq(NoteFolder::getId, folderId)
                .eq(NoteFolder::getUserId, userId)
                .eq(NoteFolder::getIsDeleted, 0));
        if (folder == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "文件夹不存在");
        }
    }
}

