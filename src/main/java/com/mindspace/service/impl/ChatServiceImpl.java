package com.mindspace.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mindspace.ai.AiClient;
import com.mindspace.common.exception.BusinessException;
import com.mindspace.common.result.ResultCode;
import com.mindspace.dto.ChatRequestDTO;
import com.mindspace.entity.ChatMessage;
import com.mindspace.entity.ChatSession;
import com.mindspace.entity.KnowledgeBase;
import com.mindspace.mapper.ChatMessageMapper;
import com.mindspace.mapper.ChatSessionMapper;
import com.mindspace.mapper.KnowledgeBaseMapper;
import com.mindspace.service.ChatService;
import com.mindspace.service.DocumentEmbeddingService;
import com.mindspace.vo.ChatAnswerVO;
import com.mindspace.vo.VectorSearchResultVO;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ChatServiceImpl implements ChatService {

    //定义常量
    private static final String ROLE_USER = "USER";
    private static final String ROLE_ASSISTANT = "ASSISTANT";

    private static final String NO_CONTEXT_ANSWER =
            "知识库中还没有可用资料，请先上传文档并完成解析和向量化。";

    private static final String SYSTEM_PROMPT = """
            你是 MindSpace 知识库问答助手。
            规则：
            1. 只能根据下面提供的资料回答，不要使用资料以外的知识；
            2. 如果资料不足，直接说“根据现有资料无法回答”，不要编造；
            3. 回答要简洁，引用资料时在句末加 [编号]；
            4. 不要输出 JSON、Markdown 代码块或与问题无关的内容。
            """;

    //注入所需依赖
    private final ChatSessionMapper chatSessionMapper;
    private final ChatMessageMapper chatMessageMapper;
    private final KnowledgeBaseMapper knowledgeBaseMapper;
    private final DocumentEmbeddingService documentEmbeddingService;
    private final AiClient aiClient;
    private final ObjectMapper objectMapper;

    //从配置文件注入参数
    @Value("${chat.top-k:5}")
    private int topK;

    @Value("${chat.history-size:6}")
    private int historySize;

    @Override
    public ChatAnswerVO ask(Long userId, ChatRequestDTO dto) {

        // 1. 检查知识库是否属于你
        checkKnowledgeBase(userId, dto.getKnowledgeBaseId());

        // 2. 看下前端发过来的dto是否有sessionId 没有就新建一个会话，有就查询会话
        ChatSession session = loadOrCreateSession(userId, dto);

        // 3. 最近几轮历史（新会话为空）
        //根据会话id 去数据库查询最近的N条历史聊天记录 然后存到history列表
        List<ChatMessage> history = loadHistory(session);

        // 4. 向量检索
        //之前引入了向量化的服务层依赖 然后你把id 知识库id 问题 topK 传递给向量检索服务
        //让他把你的问题 用向量检索服务 去查询知识库中 最相似的topK条记录
        //然后存储到VectorSearchResultVO格式叫做references的列表
        List<VectorSearchResultVO> references = documentEmbeddingService.vectorSearch(
                userId, dto.getKnowledgeBaseId(), dto.getQuestion(), topK);

        // 5. 没有资料时直接给固定回答，不调用大模型
        String answer;
        //如果向量化检索得到的列表是空
        if (references.isEmpty()) {
            //如果没有相似的记录 就直接给固定回答
            answer = NO_CONTEXT_ANSWER;
            //如果不是空的
        } else {
            //组装一个拥有完整信息的提示词字符串
            String userPrompt = buildUserPrompt(dto.getQuestion(), references, history);
            //调用ai接口把完整提示词 和 系统提示词发过去
            answer = aiClient.chat(SYSTEM_PROMPT, userPrompt);
        }

        // 6. 保存会话
        //如果会话id为空
        if (session.getId() == null) {
            //那就在数据库中新建一个会话
            chatSessionMapper.insert(session);
            //如果已经有了会话id
        } else {
            //
            //创建一个空的chatSession容器 取名叫做touch
            ChatSession touch = new ChatSession();
            //然后往容器里加入当前会话id
            touch.setId(session.getId());
            //然后往容器里加入当前时间
            touch.setUpdateTime(LocalDateTime.now());
            //然后根据会话id 更新会话信息
            chatSessionMapper.updateById(touch);
        }

        // 7. 保存两条消息
        //用户发的信息保存
        saveMessage(session.getId(), ROLE_USER, dto.getQuestion(), null);
        //ai返回的信息经过json序列化 保存到数据库
        ChatMessage assistantMessage = saveMessage(
                session.getId(), ROLE_ASSISTANT, answer, toJson(references));

        // 8. 返回结果 封装到合适的vo里面 然后直接返回给前端
        ChatAnswerVO vo = new ChatAnswerVO();
        vo.setSessionId(session.getId());
        vo.setMessageId(assistantMessage.getId());
        vo.setAnswer(answer);
        vo.setReferences(references);
        return vo;

    }

    /*````````````````````````````````````工具类··································*/


    //检查知识库是否属于你
    private void checkKnowledgeBase(Long userId, Long knowledgeBaseId) {
        Long count = knowledgeBaseMapper.selectCount(new LambdaQueryWrapper<KnowledgeBase>()
                .eq(KnowledgeBase::getId, knowledgeBaseId)
                .eq(KnowledgeBase::getUserId, userId));
        if (count == 0) {
            throw new BusinessException(ResultCode.KB_NOT_FOUND);
        }
    }

    //加载或者创建会话
    private ChatSession loadOrCreateSession(Long userId, ChatRequestDTO dto) {
        //拿到传过来的dto 取出来里的sessionId 看下是否为空
        if (dto.getSessionId() == null) {
            //如果没有sessionid 那我们就新建一个会话
            //创建一个空的chatSession容器 取名叫做session
            ChatSession session = new ChatSession();
            //然后往容器里加入当前userId
            session.setUserId(userId);
            //然后往容器里加入当前知识库id
            session.setKnowledgeBaseId(dto.getKnowledgeBaseId());
            //然后往容器里加入当前问题的标题
            session.setTitle(buildTitle(dto.getQuestion()));
            //返回当前的ChatSession容器
            return session;
        }

        //如果有sessionid 那就去数据库根据sessionid还有用户id 去查询会话数据
        ChatSession session = chatSessionMapper.selectOne(new LambdaQueryWrapper<ChatSession>()
                .eq(ChatSession::getId, dto.getSessionId())
                .eq(ChatSession::getUserId, userId));
        //如果没有查询到 抛出异常
        if (session == null) {
            throw new BusinessException(ResultCode.CHAT_SESSION_NOT_FOUND);
        }//如果有查询到 但是知识库id不一致 抛出异常
        if (!session.getKnowledgeBaseId().equals(dto.getKnowledgeBaseId())) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "会话不属于该知识库");
        }
        //正常情况 就返回session的数据
        return session;
    }

    //加载会话历史
    private List<ChatMessage> loadHistory(ChatSession session) {
        //如果没有会话id 或者 历史记录大小小于等于0
        if (session.getId() == null || historySize <= 0) {
            //就返回空列表
            return List.of();
        }
        //去数据库找最近的N条历史聊天记录 条件是会话id等于当前会话id 根据时间降序
        List<ChatMessage> messages = chatMessageMapper.selectList(
                new LambdaQueryWrapper<ChatMessage>()
                        .eq(ChatMessage::getSessionId, session.getId())
                        .orderByDesc(ChatMessage::getCreateTime)
                        .last("LIMIT " + historySize));
        //查询到 那就封装成一个列表
        List<ChatMessage> history = new ArrayList<>(messages);
        //反转列表 因为最近的在最后面（ai读记录从最早的读到现在 你得反转）
        Collections.reverse(history);
        //返回历史记录列表
        return history;
    }

    private String buildTitle(String question) {
        String title = question.trim();
        return title.length() > 30 ? title.substring(0, 30) + "..." : title;
    }


    //构建完整提示词 需要拿到用户问题 向量化返回的最相关列表 对话聊天记录
    private String buildUserPrompt(String question,
                                   List<VectorSearchResultVO> references,
                                   List<ChatMessage> history) {
        //定义一个StringBuilder容器
        StringBuilder sb = new StringBuilder();

        //如果有聊天记录
        if (!history.isEmpty()) {
            //那聊天记录加入提示词
            sb.append("对话历史：\n");
            //遍历聊天记录
            for (ChatMessage message : history) {
                //如果聊天记录 role 匹配的是 ROLE_USER 那就是用户发的消息 反之是ai回复的消息
                sb.append(ROLE_USER.equals(message.getRole()) ? "用户：" : "助手：")
                        //拿到当前的聊天记录然后加入提示词里
                        .append(message.getContent())
                        //上面就变成 用户：用户的问题
                        //这里是换行
                        .append("\n");
            }
            //换行
            sb.append("\n");
        }

        //给容器加入资料提示词
        sb.append("资料：\n");
        //遍历向量化列表
        for (int i = 0; i < references.size(); i++) {
            //把当前返回的tok列表 依次 放在一个临时变量ref里面
            VectorSearchResultVO ref = references.get(i);
            //拼接一下序号 文档名称 内容 再换行  得到的是 [1]文档 换行 内容
            sb.append("[").append(i + 1).append("] 文档：")
                    .append(ref.getOriginalName()).append("\n");
            //把topk列表里面对应索引的内容加入到提示词里面
            sb.append("内容：").append(ref.getContent()).append("\n\n");
        }
        //把用户的问题注入提示词
        sb.append("用户问题：").append(question);
        //返回一个完整的提示词字符串
        return sb.toString();
    }

    //保存聊天记录方法
    private ChatMessage saveMessage(Long sessionId, String role,
                                    String content, String referenceJson) {
        //创建一个空的chatMessage容器 取名叫做message
        ChatMessage message = new ChatMessage();
        //然后往容器里加入会话id
        message.setSessionId(sessionId);
        //然后往容器里加入角色
        message.setRole(role);
        //然后往容器里加入内容
        message.setContent(content);
        //然后往容器里加入引用json
        //引用json 是一个json字符串 里面包含了向量化返回的最相关列表
        message.setReferenceJson(referenceJson);
        //往数据库里插入记录
        chatMessageMapper.insert(message);
        return message;
    }

    private String toJson(List<VectorSearchResultVO> references) {
        try {
            return objectMapper.writeValueAsString(references);
        } catch (Exception e) {
            log.warn("引用序列化失败", e);
            return "[]";
        }
    }

}
