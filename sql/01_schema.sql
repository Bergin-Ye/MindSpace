CREATE DATABASE IF NOT EXISTS mindspace
    DEFAULT CHARACTER SET utf8mb4
    DEFAULT COLLATE utf8mb4_0900_ai_ci;

USE mindspace;

-- 1. 用户表
CREATE TABLE sys_user (
    id          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    username    VARCHAR(50)  NOT NULL COMMENT '用户名',
    email       VARCHAR(100) NOT NULL COMMENT '邮箱',
    password    VARCHAR(255) NOT NULL COMMENT 'BCrypt 加密后的密码',
    avatar      VARCHAR(500) NULL COMMENT '头像地址',
    create_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted     TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除：0 未删除，1 已删除',
    PRIMARY KEY (id),
    UNIQUE KEY uk_username (username),
    UNIQUE KEY uk_email (email)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '用户表';

-- 2. 知识库表
CREATE TABLE knowledge_base (
    id          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    user_id     BIGINT       NOT NULL COMMENT '所属用户',
    name        VARCHAR(100) NOT NULL COMMENT '知识库名称',
    description VARCHAR(500) NULL COMMENT '描述',
    create_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted     TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除：0 未删除，1 已删除',
    PRIMARY KEY (id),
    KEY idx_user_id (user_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '知识库表';

-- 3. 文件夹表
CREATE TABLE folder (
    id                BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    user_id           BIGINT       NOT NULL COMMENT '所属用户',
    knowledge_base_id BIGINT       NOT NULL COMMENT '所属知识库',
    parent_id         BIGINT       NOT NULL DEFAULT 0 COMMENT '父文件夹 ID，0 表示根目录',
    name              VARCHAR(100) NOT NULL COMMENT '文件夹名称',
    is_default        TINYINT      NOT NULL DEFAULT 0 COMMENT '是否默认文件夹：0 否，1 是（未分类）',
    create_time       DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time       DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted           TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除：0 未删除，1 已删除',
    PRIMARY KEY (id),
    KEY idx_kb_parent (knowledge_base_id, parent_id),
    KEY idx_user_id (user_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '文件夹表';

-- 4. 文件表
CREATE TABLE document (
    id                BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    user_id           BIGINT       NOT NULL COMMENT '所属用户',
    knowledge_base_id BIGINT       NOT NULL COMMENT '所属知识库',
    folder_id         BIGINT       NOT NULL COMMENT '所属文件夹',
    original_name     VARCHAR(255) NOT NULL COMMENT '原始文件名',
    file_name         VARCHAR(255) NOT NULL COMMENT '服务端保存文件名（UUID）',
    file_path         VARCHAR(500) NOT NULL COMMENT '本地存储路径',
    file_type         VARCHAR(30)  NOT NULL COMMENT 'pdf/docx/txt/md',
    file_size         BIGINT       NOT NULL COMMENT '文件大小（字节）',
    file_hash         VARCHAR(64)  NOT NULL COMMENT 'SHA-256',
    extracted_text    LONGTEXT     NULL COMMENT '提取的文本',
    summary           TEXT         NULL COMMENT 'AI 摘要',
    keywords          JSON         NULL COMMENT 'AI 关键词数组',
    ai_category       VARCHAR(100) NULL COMMENT 'AI 分类结果',
    confidence        DECIMAL(5,4) NULL COMMENT '分类置信度 0-1',
    process_status    VARCHAR(30)  NOT NULL DEFAULT 'UPLOADED' COMMENT 'UPLOADED/PROCESSING/SUCCESS/FAILED',
    error_message     TEXT         NULL COMMENT '失败原因',
    create_time       DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '上传时间',
    update_time       DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted           TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除：0 未删除，1 已删除',
    PRIMARY KEY (id),
    KEY idx_user_kb (user_id, knowledge_base_id),
    KEY idx_folder (folder_id),
    KEY idx_hash (user_id, knowledge_base_id, file_hash),
    KEY idx_status (process_status),
    FULLTEXT KEY ft_document_search (original_name, extracted_text, summary) WITH PARSER ngram
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '文件表';

-- 5. 聊天会话表
CREATE TABLE chat_session (
    id                BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    user_id           BIGINT       NOT NULL COMMENT '用户 ID',
    knowledge_base_id BIGINT       NOT NULL COMMENT '知识库 ID',
    title             VARCHAR(255) NOT NULL DEFAULT '新会话' COMMENT '会话标题',
    create_time       DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time       DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted           TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除：0 未删除，1 已删除',
    PRIMARY KEY (id),
    KEY idx_user_kb (user_id, knowledge_base_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '聊天会话表';

-- 6. 聊天消息表
CREATE TABLE chat_message (
    id             BIGINT      NOT NULL AUTO_INCREMENT COMMENT '主键',
    session_id     BIGINT      NOT NULL COMMENT '会话 ID',
    role           VARCHAR(20) NOT NULL COMMENT 'USER / ASSISTANT',
    content        TEXT        NOT NULL COMMENT '消息内容',
    reference_json JSON        NULL COMMENT '来源引用（文档、片段、页码）',
    create_time    DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (id),
    KEY idx_session_id (session_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '聊天消息表';

USE mindspace;

CREATE TABLE IF NOT EXISTS document_chunk (
                                              id                BIGINT   NOT NULL AUTO_INCREMENT COMMENT '主键',
                                              document_id       BIGINT   NOT NULL COMMENT '所属文档',
                                              user_id           BIGINT   NOT NULL COMMENT '所属用户',
                                              knowledge_base_id BIGINT   NOT NULL COMMENT '所属知识库',
                                              chunk_index       INT      NOT NULL COMMENT '文档内分块序号，从 0 开始',
                                              content           TEXT     NOT NULL COMMENT 'chunk 文本',
                                              embedding         LONGTEXT NOT NULL COMMENT '向量 JSON 数组',
                                              embedding_dim     INT      NOT NULL COMMENT '向量维度',
                                              create_time       DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                                              PRIMARY KEY (id),
    KEY idx_document (document_id),
    KEY idx_user_kb (user_id, knowledge_base_id)
    ) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '文档向量块表';