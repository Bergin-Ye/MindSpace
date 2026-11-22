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