package com.mido.backend.chat.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mido.backend.chat.entity.ChatFolder;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ChatFolderMapper extends BaseMapper<ChatFolder> {
}

