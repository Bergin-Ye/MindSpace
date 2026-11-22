package com.mindspace.vo;

import lombok.Data;

@Data
public class VectorSearchResultVO {

    private Long documentId;
    private String originalName;
    private Long chunkId;
    private Integer chunkIndex;
    private String content;
    private Double score;
}