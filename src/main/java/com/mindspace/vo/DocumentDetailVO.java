package com.mindspace.vo;

import com.mindspace.entity.Document;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class DocumentDetailVO {

    //正文预览的最大字符数
    private static final int PREVIEW_LENGTH = 200;

    private Long id;
    private Long knowledgeBaseId;
    private Long folderId;
    private String originalName;
    private String fileType;
    private Long fileSize;
    private String processStatus;
    private String summary;
    private String keywords;
    private String aiCategory;
    private BigDecimal confidence;
    private String errorMessage;
    private Long extractedTextLength;
    private String textPreview;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    public static DocumentDetailVO from(Document doc) {
        DocumentDetailVO vo = new DocumentDetailVO();
        vo.setId(doc.getId());
        vo.setKnowledgeBaseId(doc.getKnowledgeBaseId());
        vo.setFolderId(doc.getFolderId());
        vo.setOriginalName(doc.getOriginalName());
        vo.setFileType(doc.getFileType());
        vo.setFileSize(doc.getFileSize());
        vo.setProcessStatus(doc.getProcessStatus());
        vo.setSummary(doc.getSummary());
        vo.setKeywords(doc.getKeywords());
        vo.setAiCategory(doc.getAiCategory());
        vo.setConfidence(doc.getConfidence());
        vo.setErrorMessage(doc.getErrorMessage());

        //详情接口不返回全文，避免响应过大；只给长度和预览
        String text = doc.getExtractedText();
        if (text != null) {
            vo.setExtractedTextLength((long) text.length());
            vo.setTextPreview(text.length() > PREVIEW_LENGTH
                    ? text.substring(0, PREVIEW_LENGTH) + "..."
                    : text);
        }

        vo.setCreateTime(doc.getCreateTime());
        vo.setUpdateTime(doc.getUpdateTime());
        return vo;
    }
}