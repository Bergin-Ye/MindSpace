package com.mindspace.service;

import com.mindspace.common.result.PageResult;
import com.mindspace.dto.ChatSessionUpdateDTO;
import com.mindspace.vo.ChatMessageVO;
import com.mindspace.vo.ChatSessionVO;

public interface ChatHistoryService {

    PageResult<ChatSessionVO> listSessions(Long userId, Long knowledgeBaseId,
                                           long page, long size);

    PageResult<ChatMessageVO> listMessages(Long userId, Long sessionId,
                                           long page, long size);

    ChatSessionVO renameSession(Long userId, Long sessionId, ChatSessionUpdateDTO dto);

    void deleteSession(Long userId, Long sessionId);
}