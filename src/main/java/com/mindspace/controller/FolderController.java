package com.mindspace.controller;

import com.mindspace.common.result.Result;
import com.mindspace.dto.FolderCreateDTO;
import com.mindspace.dto.FolderMoveDTO;
import com.mindspace.dto.FolderUpdateDTO;
import com.mindspace.service.FolderService;
import com.mindspace.vo.FolderTreeVO;
import com.mindspace.vo.FolderVO;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 文件夹接口。
 * 所有接口都需要登录，userId 来自 token，前端不能自己传。
 */
@RestController
@RequestMapping("/api/folders")
public class FolderController {
    @Autowired
    private FolderService folderService;

    //创建文件夹
    @PostMapping
    public Result<FolderVO> create(@AuthenticationPrincipal Long userId,
            @RequestBody @Valid FolderCreateDTO dto){
        return Result.success(folderService.create(userId, dto));
    }

    // 平铺列表：适合下拉框、面包屑、按 id 查找父级
    @GetMapping
    public Result<List<FolderVO>> list(@AuthenticationPrincipal Long userId,
                                       @RequestParam("knowledgeBaseId") Long knowledgeBaseId){
        return Result.success(folderService.list(userId, knowledgeBaseId));
    }

    // 树结构：适合前端左侧文件夹导航，children 已经组装好
    @GetMapping("/tree")
    public Result<List<FolderTreeVO>> tree(@AuthenticationPrincipal Long userId,
                                           @RequestParam("knowledgeBaseId") Long knowledgeBaseId){
        return Result.success(folderService.tree(userId, knowledgeBaseId));
    }

    //重命名文件夹
    @PutMapping("/{id}")
    public Result<FolderVO> update(@AuthenticationPrincipal Long userId,
                                   @PathVariable("id") Long id,
                                   @Valid @RequestBody FolderUpdateDTO dto){
        return Result.success(folderService.update(userId, id, dto));
    }

    //删除文件夹
    @DeleteMapping("/{id}")
    public Result<Void> delete(@AuthenticationPrincipal Long userId,
                               @PathVariable("id") Long id){
        folderService.delete(userId, id);
        return Result.success();
    }

    //移动文件夹
    @PutMapping("/{id}/move")
    public Result<FolderVO> move(@AuthenticationPrincipal Long userId,
                                  @PathVariable("id") Long id,
                                  @Valid @RequestBody FolderMoveDTO dto){
        return Result.success(folderService.move(userId, id, dto));
    }




}
