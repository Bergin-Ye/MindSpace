package com.mindspace.vo;

import lombok.Data;

@Data
public class EmbedResultVO {

    private Long documentId;
    private Integer chunkCount;
    private Integer embeddingDim;
    private Long elapsedMs;
}