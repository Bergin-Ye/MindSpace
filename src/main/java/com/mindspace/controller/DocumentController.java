package com.mindspace.controller;

import com.mindspace.service.DocumentAiService;
import org.springframework.core.io.Resource;
import com.mindspace.common.result.PageResult;
import com.mindspace.common.result.Result;
import com.mindspace.document.DownloadFile;
import com.mindspace.dto.DocumentMoveDTO;
import com.mindspace.dto.DocumentUpdateDTO;
import com.mindspace.service.DocumentService;
import com.mindspace.vo.BatchUploadResultVO;
import com.mindspace.vo.DocumentDetailVO;
import com.mindspace.vo.DocumentVO;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.util.List;

/*
* 用于接收文档上传相关请求
* */
@RestController
@RequestMapping("/api/documents")
public class DocumentController {

    @Autowired
    private DocumentService documentService;

    @Autowired
    private DocumentAiService documentAiService;

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


    /*
     * 分页查询文档
     *参照接口文档返回指定格式 接受所需参数
     * */
    @GetMapping
    public Result<PageResult<DocumentVO>> list(@AuthenticationPrincipal Long userId,
                                                     @RequestParam("knowledgeBaseId") Long knowledgeBaseId,
                                                     @RequestParam(value = "folderId", required = false) Long folderId,
                                                     @RequestParam(value = "keyword", required = false) String keyword,
                                                     @RequestParam(value = "processStatus", required = false) String processStatus,
                                                     @RequestParam(value = "page", defaultValue = "1") long page,
                                                     @RequestParam(value = "size", defaultValue = "10") long size){
        return Result.success(documentService.list(userId, knowledgeBaseId, folderId, keyword, processStatus, page, size));

    }



    /*
     * 查询文档详情
     * */
    @GetMapping("/{id}")
    public Result<DocumentDetailVO> detail(@AuthenticationPrincipal Long userId,
                                           @PathVariable("id") Long id){
        return Result.success(documentService.detail(userId, id));
    }
    /*
     * 重命名文件
     * */
    @PutMapping("/{id}")
    public Result<DocumentVO> rename(@AuthenticationPrincipal Long userId,
                                     @PathVariable("id") Long id,
                                     @Valid @RequestBody DocumentUpdateDTO dto){
        return Result.success(documentService.rename(userId, id, dto));
    }

    /*
     * 移动文件
     * */
    @PutMapping("/{id}/move")
    public Result<DocumentVO> move(@AuthenticationPrincipal Long userId,
                                   @PathVariable("id") Long id,
                                   @Valid @RequestBody DocumentMoveDTO dto){
        return Result.success(documentService.move(userId, id, dto));
    }

    /*
     * 删除文件
     * */
    @DeleteMapping("/{id}")
    public Result<Void> delete(@AuthenticationPrincipal Long userId,
                               @PathVariable("id") Long id){
        documentService.delete(userId, id);
        return Result.success();
    }

    /*
     * 下载文件
     * */

    /*传的resource是文件流
    * Result = 给 JS 讲业务故事（code/message）；
    * ResponseEntity = 给浏览器下指令（要不要下载、文件名是什么、字节在哪）。
    * */
    @GetMapping("/{id}/download")
    public ResponseEntity<Resource> download(@AuthenticationPrincipal Long userId,
                                             @PathVariable("id") Long id) {
        DownloadFile file = documentService.download(userId, id);

        /*
        ContentDisposition 构建下载头
        * attachment() = "作为附件下载"，另一个选项是 inline() = "浏览器内预览"
.filename(原始文件名, UTF_8) = 告诉浏览器保存时叫啥名字。
* 关键细节：中文文件名必须显式指定 UTF-8，否则浏览器解出来的文件名是乱码
* （它内部会生成 filename*=UTF-8''%E5%91%BD... 这种 RFC 5987 编码）
        * */
        ContentDisposition disposition = ContentDisposition.attachment()
                .filename(file.originalName(), StandardCharsets.UTF_8)
                .build();

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .contentLength(file.size())
                .body(file.resource());
    }


    /*
    * 这里是文档解析的控制层接口
    * 我在思考 前端会给我发过来要解析的文档的id
    * 我们需要userId来判断是否有权限解析该文档
    * */
    @PostMapping("/{id}/parse")
    public Result<DocumentDetailVO > parse(@AuthenticationPrincipal Long userId,
                                   @PathVariable("id") Long id){
        return Result.success(documentService.parseDocument(userId,id));
    }

    /*
    * 这里是与ai对话的接口
    * */
    @PostMapping("/{id}/analyze")
    public Result<DocumentDetailVO> analyze(@AuthenticationPrincipal Long userId,
                                            @PathVariable("id") Long id){
        return Result.success(documentAiService.analyze(userId,id));
    }




    }

