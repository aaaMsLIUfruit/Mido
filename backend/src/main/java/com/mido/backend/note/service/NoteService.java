package com.mido.backend.note.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.mido.backend.note.entity.Note;
import java.util.Optional;

public interface NoteService extends IService<Note> {

    Note createNote(Note note);

    IPage<Note> listNotes(Long userId, long page, long size);

    IPage<Note> listNotesByFolder(Long userId, Long folderId, long page, long size);

    Note updateNote(Note note, Long userId);

    void softDelete(Long noteId, Long userId);

    Optional<Note> findByIdAndUser(Long id, Long userId);
}

