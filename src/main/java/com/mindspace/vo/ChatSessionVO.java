package com.mindspace.vo;

import com.mindspace.entity.ChatSession;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ChatSessionVO {

    private Long id;
    private Long knowledgeBaseId;
    private String title;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    public static ChatSessionVO from(ChatSession session) {
        ChatSessionVO vo = new ChatSessionVO();
        vo.setId(session.getId());
        vo.setKnowledgeBaseId(session.getKnowledgeBaseId());
        vo.setTitle(session.getTitle());
        vo.setCreateTime(session.getCreateTime());
        vo.setUpdateTime(session.getUpdateTime());
        return vo;
    }
}