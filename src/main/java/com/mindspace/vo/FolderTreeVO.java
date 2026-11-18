package com.mindspace.vo;


import com.mindspace.entity.Folder;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/*
* 文件夹树VO
* 为什么我们需要树结构的VO？
* 一句话为了方便前端展示
* - 前端拿到就能直接递归渲染成左侧文件夹树，不需要自己再拼。
- `children` 为空数组，前端不用判 null。
- 适合做 文件管理页左侧导航 。
* */
@Data
public class FolderTreeVO {

    private Long id;
    private Long knowledgeBaseId;
    private Long parentId;
    private String name;
    private Integer isDefault;
    private List<FolderTreeVO> children = new ArrayList<>();

    public static FolderTreeVO from(Folder f) {
        FolderTreeVO vo = new FolderTreeVO();
        vo.setId(f.getId());
        vo.setKnowledgeBaseId(f.getKnowledgeBaseId());
        vo.setParentId(f.getParentId());
        vo.setName(f.getName());
        vo.setIsDefault(f.getIsDefault());
        return vo;
    }
}
