package com.mindspace.service;

import com.mindspace.dto.FolderCreateDTO;
import com.mindspace.dto.FolderMoveDTO;
import com.mindspace.dto.FolderUpdateDTO;
import com.mindspace.vo.FolderTreeVO;
import com.mindspace.vo.FolderVO;
import jakarta.validation.Valid;

import java.util.List;

public interface FolderService {
    //创建文件夹
    FolderVO create(Long userId, @Valid FolderCreateDTO dto);

    //查询文件夹列表
    List<FolderVO> list(Long userId, Long knowledgeBaseId);

    //查询文件夹树结构
    List<FolderTreeVO> tree(Long userId, Long knowledgeBaseId);

    //更新文件夹
    FolderVO update(Long userId, Long id, @Valid FolderUpdateDTO dto);

    //删除文件夹
    void delete(Long userId, Long id);

    //移动文件夹
    FolderVO move(Long userId, Long id, @Valid FolderMoveDTO dto);

    void createDefault(Long userId, Long knowledgeBaseId);

    void deleteByKnowledgeBase(Long userId, Long knowledgeBaseId);
}
