package com.mindspace.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/*
* 和ai对话的会话记录session
* 指的是会话不是具体的聊天记录
* */

@Data
@TableName("chat_session")
public class ChatSession {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;
    private Long knowledgeBaseId;
    private String title;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    //逻辑删除 0 未删除 1 已删除
    @TableLogic
    private Integer deleted;
}