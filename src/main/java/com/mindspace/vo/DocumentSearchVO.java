package com.mindspace.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class DocumentSearchVO {

    private Long id;
    private Long knowledgeBaseId;
    private Long folderId;
    private String originalName;
    private String fileType;
    private Long fileSize;
    private String processStatus;
    private String aiCategory;
    private LocalDateTime createTime;

    // 正文中关键词附近的一段
    private String textSnippet;
}