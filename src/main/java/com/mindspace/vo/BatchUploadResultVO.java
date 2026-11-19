package com.mindspace.vo;

import lombok.Data;

import java.util.List;

@Data
public class BatchUploadResultVO {

    private int total;
    private int successCount;
    private int failCount;
    private List<BatchUploadItemVO> items;
}