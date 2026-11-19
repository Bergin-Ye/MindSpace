package com.mindspace.entity;


import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/*
* java代码只需要对着驼峰操作
* mybatis会翻译成数据库的下划线
* 这样我们可以无痛的使用数据库的字段
* */
@Data
@TableName("document")
public class Document {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private Long knowledgeBaseId;
    private Long folderId;
    private String originalName;
    private String fileName;
    private String filePath;
    private String fileType;
    private Long fileSize;
    private String fileHash;
    private String extractedText;
    private String summary;
    // 当前先存 JSON 字符串，AI 功能完成后写入关键词数组
    private String keywords;
    private String aiCategory;
    private BigDecimal confidence;
    private String processStatus;
    private String errorMessage;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    @TableLogic
    private Integer deleted;
}
