-- 为chat表添加folder_id字段以支持对话文件夹管理
-- 如果需要支持对话文件夹功能，请执行此SQL

ALTER TABLE chat 
ADD COLUMN folder_id BIGINT DEFAULT NULL AFTER user_id,
ADD INDEX idx_chat_folder_id (folder_id),
ADD FOREIGN KEY (folder_id) REFERENCES chat_folder(id);

-- 添加字段后，需要将Chat.java中的folderId字段的exist = false标记去掉
-- 修改 Chat.java:
-- @TableField(value = "folder_id", exist = false) 
-- 改为:
-- @TableField("folder_id")

