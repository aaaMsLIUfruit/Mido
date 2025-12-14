package com.mido.backend.chat.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mido.backend.chat.entity.Chat;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ChatMapper extends BaseMapper<Chat> {
}

