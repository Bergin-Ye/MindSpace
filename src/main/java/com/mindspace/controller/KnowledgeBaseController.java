package com.mindspace.controller;


import com.mindspace.common.result.Result;
import com.mindspace.dto.KnowledgeBaseDTO;
import com.mindspace.service.KnowledgeBaseService;
import com.mindspace.vo.KnowledgeBaseVO;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/knowledge-bases")
public class KnowledgeBaseController {
    @Autowired
    private KnowledgeBaseService knowledgeBaseService;

    //创建知识库
    @PostMapping
    public Result<KnowledgeBaseVO> create(@AuthenticationPrincipal Long userId,
                                          @RequestBody @Valid KnowledgeBaseDTO dto){
        return Result.success(knowledgeBaseService.create(userId, dto));
    }

    //查询知识库列表
    @GetMapping
    public Result<List<KnowledgeBaseVO>> list(@AuthenticationPrincipal Long userId) {
        //这里就是把userId传给service 然后service找到东西了就放到Result<List<KnowledgeBase>>中
        return Result.success(knowledgeBaseService.list(userId));
    }

    //查询知识库详分区
    @GetMapping("/{id}")
    public Result<KnowledgeBaseVO> detail(@AuthenticationPrincipal Long userId , @PathVariable long id) {
        return Result.success(knowledgeBaseService.detail(userId, id));
    }

    //修改知识库
    @PutMapping("/{id}")
    public Result<KnowledgeBaseVO> update(@AuthenticationPrincipal Long userId ,
                                              @PathVariable long id ,
                                              @RequestBody @Valid KnowledgeBaseDTO dto){
        return Result.success(knowledgeBaseService.update(userId, id, dto));
    }

    //删除知识库
    @DeleteMapping("/{id}")
    public Result<Void> delete(@AuthenticationPrincipal Long userId ,
                               @PathVariable long id){
        knowledgeBaseService.delete(userId, id);
        return Result.success();
    }
}
