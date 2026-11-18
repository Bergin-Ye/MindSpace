package com.mindspace.service;

import com.mindspace.dto.KnowledgeBaseDTO;
import com.mindspace.entity.KnowledgeBase;
import com.mindspace.vo.KnowledgeBaseVO;
import jakarta.validation.Valid;

import java.util.List;

public interface KnowledgeBaseService {
    //创建知识库
    KnowledgeBaseVO create(Long userId, @Valid KnowledgeBaseDTO dto);

    //查询知识库列表
    List<KnowledgeBaseVO> list(Long userId);

    //查询知识库详分区
    KnowledgeBaseVO detail(Long userId, long id);

    //修改知识库
    KnowledgeBaseVO update(Long userId, long id, @Valid KnowledgeBaseDTO dto);

    //删除知识库
    void delete(Long userId, long id);

    //注册时创建默认知识库
    void createDefault(Long userId);
}
