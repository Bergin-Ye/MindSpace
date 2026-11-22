package com.mindspace.vo;

import lombok.Data;

import java.util.List;

@Data
public class ChatAnswerVO {

    private Long sessionId;
    private Long messageId;
    private String answer;
    private List<VectorSearchResultVO> references;
}