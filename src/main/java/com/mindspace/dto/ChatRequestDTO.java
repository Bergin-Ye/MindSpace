package com.mindspace.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ChatRequestDTO {

    @NotNull(message = "知识库 id 不能为空")
    private Long knowledgeBaseId;

    //不传就新建会话
    private Long sessionId;

    @NotBlank(message = "问题不能为空")
    @Size(max = 2000, message = "问题不能超过2000个字符")
    private String question;
}