package com.mindspace.vo;


import com.mindspace.entity.Folder;
import lombok.Data;

import java.time.LocalDateTime;

/*
* 平铺列表VO
* 为什么我们需要平铺和树结构的VO？
* 一句话为了方便前端展示
* - 每条记录都知道自己的`parentId` ，但不知道子节点是谁。
- 前端如果想知道“集合的父级是谁”，直接按 id 找`parentId=2` 的记录即可。
- 适合做 下拉框、面包屑、按 id 查名字、统计数量 。
- 数据量再大也不会因为嵌套太深导致 JSON 层级爆炸。
本项目里它会被用在：

- 移动文件时选择目标文件夹的下拉框；
- 文件详情里显示“所属文件夹”；
- 面包屑：HashMap → 集合 → Java 基础；
- 后端按 id 校验父文件夹是否存在
* */
@Data
public class FolderVO {

    private Long id;
    private Long knowledgeBaseId;
    private Long parentId;
    private String name;
    private Integer isDefault;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    public static FolderVO from(Folder f) {
        FolderVO vo = new FolderVO();
        vo.setId(f.getId());
        vo.setKnowledgeBaseId(f.getKnowledgeBaseId());
        vo.setParentId(f.getParentId());
        vo.setName(f.getName());
        vo.setIsDefault(f.getIsDefault());
        vo.setCreateTime(f.getCreateTime());
        vo.setUpdateTime(f.getUpdateTime());
        return vo;
    }
}
