package com.mindspace.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mindspace.common.exception.BusinessException;
import com.mindspace.common.result.PageResult;
import com.mindspace.common.result.ResultCode;
import com.mindspace.dto.ChatSessionUpdateDTO;
import com.mindspace.entity.ChatMessage;
import com.mindspace.entity.ChatSession;
import com.mindspace.entity.KnowledgeBase;
import com.mindspace.mapper.ChatMessageMapper;
import com.mindspace.mapper.ChatSessionMapper;
import com.mindspace.mapper.KnowledgeBaseMapper;
import com.mindspace.service.ChatHistoryService;
import com.mindspace.vo.ChatMessageVO;
import com.mindspace.vo.ChatSessionVO;
import com.mindspace.vo.VectorSearchResultVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ChatHistoryServiceImpl implements ChatHistoryService {

    private final ChatSessionMapper chatSessionMapper;
    private final ChatMessageMapper chatMessageMapper;
    private final KnowledgeBaseMapper knowledgeBaseMapper;
    private final ObjectMapper objectMapper;

    /*
     * 会话列表
     * */
    @Override
    public PageResult<ChatSessionVO> listSessions(Long userId, Long knowledgeBaseId, long page, long size) {
       //如果知识库id不为空
        if (knowledgeBaseId != null) {
            //去检测知识库是否存在 是否属于当前用户
            checkKnowledgeBase(userId, knowledgeBaseId);
        }

        //分页查询会话
        long currentPage = page < 1 ? 1 : page;
        long pageSize = size < 1 ? 10 : Math.min(size, 100);

        //查询会话
        Page<ChatSession> mpPage = new Page<>(currentPage, pageSize);
        //根据userid 知识库id 还有分页参数查询会话 并且按照更新时间倒序排序
        chatSessionMapper.selectPage(mpPage, new LambdaQueryWrapper<ChatSession>()
                .eq(ChatSession::getUserId, userId)
                .eq(knowledgeBaseId != null, ChatSession::getKnowledgeBaseId, knowledgeBaseId)
                .orderByDesc(ChatSession::getUpdateTime)
                .orderByDesc(ChatSession::getId));

        //将查询结果转换为vo
        List<ChatSessionVO> records = mpPage.getRecords().stream()
                .map(ChatSessionVO::from)
                .toList();

        //返回分页结果
        return new PageResult<>(mpPage.getTotal(), mpPage.getCurrent(),
                mpPage.getSize(), mpPage.getPages(), records);
    }

    /*
     * 某个会话的消息列表
     * */
    @Override
    public PageResult<ChatMessageVO> listMessages(Long userId, Long sessionId,
                                                  long page, long size) {
        //检查会话是否存在 是否属于当前用户
        getOwnedSession(userId, sessionId);

        //分页查询消息
        long currentPage = page < 1 ? 1 : page;
        long pageSize = size < 1 ? 20 : Math.min(size, 100);
        //定义分页查询参数
        Page<ChatMessage> mpPage = new Page<>(currentPage, pageSize);
        //根据会话id 还有分页参数查询消息 并且按照创建时间升序排序
        chatMessageMapper.selectPage(mpPage, new LambdaQueryWrapper<ChatMessage>()
                .eq(ChatMessage::getSessionId, sessionId)
                .orderByAsc(ChatMessage::getCreateTime)
                .orderByAsc(ChatMessage::getId));

        //将查询结果转换为vo
        List<ChatMessageVO> records = mpPage.getRecords().stream()
                .map(this::toMessageVO)
                .toList();

        //返回分页结果
        return new PageResult<>(mpPage.getTotal(), mpPage.getCurrent(),
                mpPage.getSize(), mpPage.getPages(), records);
    }

    /*
    * 重命名会话
    * */
    @Override
    public ChatSessionVO renameSession(Long userId, Long sessionId, ChatSessionUpdateDTO dto) {
        ChatSession session = getOwnedSession(userId, sessionId);

        String title = dto.getTitle().trim();
        LocalDateTime now = LocalDateTime.now();

        ChatSession update = new ChatSession();
        update.setId(sessionId);
        update.setTitle(title);
        update.setUpdateTime(now);
        chatSessionMapper.updateById(update);

        session.setTitle(title);
        session.setUpdateTime(now);
        return ChatSessionVO.from(session);
    }

    /*
    * 删除会话
    * */
    @Override
    @Transactional
    public void deleteSession(Long userId, Long sessionId) {
        getOwnedSession(userId, sessionId);

        chatMessageMapper.delete(new LambdaQueryWrapper<ChatMessage>()
                .eq(ChatMessage::getSessionId, sessionId));
        chatSessionMapper.deleteById(sessionId);
    }

    /*
    * 工具类
    * */

    private ChatSession getOwnedSession(Long userId, Long sessionId) {
        ChatSession session = chatSessionMapper.selectOne(new LambdaQueryWrapper<ChatSession>()
                .eq(ChatSession::getId, sessionId)
                .eq(ChatSession::getUserId, userId));
        if (session == null) {
            throw new BusinessException(ResultCode.CHAT_SESSION_NOT_FOUND);
        }
        return session;
    }

    private void checkKnowledgeBase(Long userId, Long knowledgeBaseId) {
        Long count = knowledgeBaseMapper.selectCount(new LambdaQueryWrapper<KnowledgeBase>()
                .eq(KnowledgeBase::getId, knowledgeBaseId)
                .eq(KnowledgeBase::getUserId, userId));
        if (count == 0) {
            throw new BusinessException(ResultCode.KB_NOT_FOUND);
        }
    }

    private ChatMessageVO toMessageVO(ChatMessage message) {
        ChatMessageVO vo = ChatMessageVO.from(message);
        vo.setReferences(parseReferences(message.getReferenceJson()));
        return vo;
    }

    private List<VectorSearchResultVO> parseReferences(String referenceJson) {
        if (!StringUtils.hasText(referenceJson)) {
            return List.of();
        }
        try {
            return objectMapper.readValue(referenceJson,
                    new TypeReference<List<VectorSearchResultVO>>() {
                    });
        } catch (Exception e) {
            log.warn("解析消息引用失败：{}", referenceJson, e);
            return List.of();
        }
    }
}
