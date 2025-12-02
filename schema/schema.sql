-- 1. 用户
CREATE TABLE user (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    username VARCHAR(50) NOT NULL UNIQUE,
    email VARCHAR(100) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_user_email ON user(email);

-- 2. 笔记文件夹
CREATE TABLE note_folder (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    name VARCHAR(100) NOT NULL,
    parent_id BIGINT DEFAULT NULL,
    is_deleted TINYINT(1) DEFAULT 0,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES user(id),
    FOREIGN KEY (parent_id) REFERENCES note_folder(id)
);

CREATE INDEX idx_note_folder_user_id ON note_folder(user_id);

-- 3. 笔记
CREATE TABLE note (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    folder_id BIGINT NOT NULL,
    title VARCHAR(200),
    content MEDIUMTEXT,
    cover_url VARCHAR(255),
    is_deleted TINYINT(1) DEFAULT 0,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES user(id),
    FOREIGN KEY (folder_id) REFERENCES note_folder(id)
);

CREATE INDEX idx_note_user_id ON note(user_id);
CREATE INDEX idx_note_folder_id ON note(folder_id);

-- 4. 笔记评论
CREATE TABLE note_comment (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    note_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    anchor_offset INT,
    anchor_length INT,
    content TEXT NOT NULL,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (note_id) REFERENCES note(id),
    FOREIGN KEY (user_id) REFERENCES user(id)
);

CREATE INDEX idx_note_comment_note_id ON note_comment(note_id);
CREATE INDEX idx_note_comment_user_id ON note_comment(user_id);

-- 5. AI 对话会话（一个 chat 文件）
CREATE TABLE chat (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    title VARCHAR(200),
    is_deleted TINYINT(1) DEFAULT 0,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES user(id)
);

CREATE INDEX idx_chat_user_id ON chat(user_id);

-- 6. 对话文件夹
CREATE TABLE chat_folder (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    name VARCHAR(100),
    parent_id BIGINT DEFAULT NULL,
    is_deleted TINYINT(1) DEFAULT 0,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES user(id),
    FOREIGN KEY (parent_id) REFERENCES chat_folder(id)
);

CREATE INDEX idx_chat_folder_user_id ON chat_folder(user_id);

-- 7. 对话消息
CREATE TABLE chat_message (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    chat_id BIGINT NOT NULL,
    role ENUM('user', 'assistant') NOT NULL,
    content MEDIUMTEXT NOT NULL,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (chat_id) REFERENCES chat(id)
);

CREATE INDEX idx_chat_message_chat_id ON chat_message(chat_id);
