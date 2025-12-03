package com.mido.backend.note.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@TableName("note_folder")
public class NoteFolder {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("user_id")
    private Long userId;

    private String name;

    @TableField("parent_id")
    private Long parentId;

    @TableField("is_deleted")
    private Integer isDeleted;

    @TableField("created_at")
    private LocalDateTime createdAt;
}

