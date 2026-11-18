package com.mindspace.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("folder")
public class Folder {
    //主键 自增
    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;
    private Long knowledgeBaseId;
    private Long parentId;
    private String name;
    private Integer isDefault;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    @TableLogic
    private Integer deleted;

}
