package com.mindspace.service;

import com.mindspace.vo.BatchUploadResultVO;
import com.mindspace.vo.DocumentVO;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface DocumentService {


    //单个文件上传
    DocumentVO upload(Long userId, Long knowledgeBaseId, Long folderId, MultipartFile file);

    //批量上传
    BatchUploadResultVO uploadBatch(Long userId, Long knowledgeBaseId, Long folderId, List<MultipartFile> files);
}
