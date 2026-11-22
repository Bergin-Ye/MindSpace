package com.mindspace.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("document_chunk")
public class DocumentChunk {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long documentId;
    private Long userId;
    private Long knowledgeBaseId;
    private Integer chunkIndex;
    private String content;
    private String embedding;
    private Integer embeddingDim;
    private LocalDateTime createTime;
}