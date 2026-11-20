package com.mindspace.service;

import com.mindspace.common.result.PageResult;
import com.mindspace.document.DownloadFile;
import com.mindspace.dto.DocumentMoveDTO;
import com.mindspace.dto.DocumentUpdateDTO;
import com.mindspace.vo.BatchUploadResultVO;
import com.mindspace.vo.DocumentDetailVO;
import com.mindspace.vo.DocumentVO;
import jakarta.validation.Valid;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface DocumentService {


    //单个文件上传
    DocumentVO upload(Long userId, Long knowledgeBaseId, Long folderId, MultipartFile file);

    //批量上传
    BatchUploadResultVO uploadBatch(Long userId, Long knowledgeBaseId, Long folderId, List<MultipartFile> files);

    //分页查询文档
    PageResult<DocumentVO> list(Long userId, Long knowledgeBaseId, Long folderId, String keyword, String processStatus, long page, long size);

    //查询文档详情
    DocumentDetailVO detail(Long userId, Long id);

    //重命名文档
    DocumentVO rename(Long userId, Long id, @Valid DocumentUpdateDTO dto);

    //移动文档
    DocumentVO move(Long userId, Long id, @Valid DocumentMoveDTO dto);

    //删除文档
    void delete(Long userId, Long id);

    //下载文档
    DownloadFile download(Long userId, Long id);

    //删除知识库时级联删除文件记录和磁盘文件
    void deleteByKnowledgeBase(Long userId, Long knowledgeBaseId);

    /*
    * 解析文档的接口
    * */
    DocumentDetailVO  parseDocument(Long userId, Long id);

}
