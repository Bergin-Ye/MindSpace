package com.mindspace.vo;

import lombok.Data;

@Data
public class BatchUploadItemVO {

    private String originalName;
    private boolean success;
    private Long documentId;
    private Integer code;
    private String message;
}