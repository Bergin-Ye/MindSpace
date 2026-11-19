package com.mindspace.controller;


import com.mindspace.common.result.Result;
import com.mindspace.service.DocumentService;
import com.mindspace.vo.BatchUploadResultVO;
import com.mindspace.vo.DocumentVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/*
* 用于接收文档上传相关请求
* */
@RestController
@RequestMapping("/api/documents")
public class DocumentController {

    @Autowired
    private DocumentService documentService;

    //单个文件上传
    @PostMapping("/upload")
    public Result<DocumentVO> upload(@AuthenticationPrincipal Long userId,
                                     @RequestParam("knowledgeBaseId") Long knowledgeBaseId,
                                     @RequestParam(value = "folderId", required = false) Long folderId,
                                     @RequestPart("file") MultipartFile file) {
        return Result.success(documentService.upload(userId, knowledgeBaseId, folderId, file));
    }

    /*
    * 批量上传
    * 允许部分成功 每个文件返回自己的code 和 msg
    * */

    @PostMapping("/batch-upload")
    public Result<BatchUploadResultVO> batchUpload(@AuthenticationPrincipal Long userId,
                                                   @RequestParam("knowledgeBaseId") Long knowledgeBaseId,
                                                   @RequestParam(value = "folderId", required = false) Long folderId,
                                                   @RequestPart("files") List<MultipartFile> files){
        return Result.success(documentService.uploadBatch(userId, knowledgeBaseId, folderId, files));

    }

}
