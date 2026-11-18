package com.mindspace.vo;


import com.mindspace.entity.KnowledgeBase;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class KnowledgeBaseVO {

    private long id;
    private String name;
    private  String description;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    //创建对应的方法 用于将KnowledgeBase实体转换为KnowledgeBaseVO视图类
    public static KnowledgeBaseVO from(KnowledgeBase kb){
        //接受KnowledgeBase实体参数
        KnowledgeBaseVO vo = new KnowledgeBaseVO();
        vo.setId(kb.getId());
        vo.setName(kb.getName());
        vo.setDescription(kb.getDescription());
        vo.setCreateTime(kb.getCreateTime());
        vo.setUpdateTime(kb.getUpdateTime());
        return vo;
    }
}
