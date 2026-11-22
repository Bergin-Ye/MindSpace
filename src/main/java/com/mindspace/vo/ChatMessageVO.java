package com.mindspace.vo;

import com.mindspace.entity.ChatMessage;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class ChatMessageVO {

    private Long id;
    private Long sessionId;
    private String role;
    private String content;
    private List<VectorSearchResultVO> references;
    private LocalDateTime createTime;

    public static ChatMessageVO from(ChatMessage message) {
        ChatMessageVO vo = new ChatMessageVO();
        vo.setId(message.getId());
        vo.setSessionId(message.getSessionId());
        vo.setRole(message.getRole());
        vo.setContent(message.getContent());
        vo.setCreateTime(message.getCreateTime());
        return vo;
    }
}