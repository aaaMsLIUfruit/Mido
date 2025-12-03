package com.mido.backend.note.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mido.backend.note.entity.NoteFolder;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface NoteFolderMapper extends BaseMapper<NoteFolder> {
}

