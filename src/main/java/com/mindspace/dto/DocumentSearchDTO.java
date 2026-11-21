package com.mindspace.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

@Data
public class DocumentSearchDTO {

    @NotNull(message = "知识库 id 不能为空")
    private Long knowledgeBaseId;

    // 全文关键词，搜索文件名、正文、AI 摘要
    private String keyword;

    // 文件夹筛选，0 或不传表示全部
    private Long folderId;

    // pdf / docx / txt / md
    private String fileType;

    // UPLOADED / PROCESSING / SUCCESS / FAILED
    private String processStatus;

    // 技术 / 学习 / 工作 / 生活 / 其他
    private String aiCategory;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private LocalDateTime startTime;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private LocalDateTime endTime;

    // relevance / createTime
    private String sort;

    private Long page = 1L;
    private Long size = 10L;
}