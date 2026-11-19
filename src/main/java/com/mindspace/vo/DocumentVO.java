package com.mindspace.vo;

import com.mindspace.entity.Document;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class DocumentVO {

    private Long id;
    private Long knowledgeBaseId;
    private Long folderId;
    private String originalName;
    private String fileType;
    private Long fileSize;
    private String processStatus;
    private LocalDateTime createTime;

    public static DocumentVO from(Document doc) {
        DocumentVO vo = new DocumentVO();
        vo.setId(doc.getId());
        vo.setKnowledgeBaseId(doc.getKnowledgeBaseId());
        vo.setFolderId(doc.getFolderId());
        vo.setOriginalName(doc.getOriginalName());
        vo.setFileType(doc.getFileType());
        vo.setFileSize(doc.getFileSize());
        vo.setProcessStatus(doc.getProcessStatus());
        vo.setCreateTime(doc.getCreateTime());
        return vo;
    }
}