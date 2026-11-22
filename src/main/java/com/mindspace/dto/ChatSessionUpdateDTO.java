package com.mindspace.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ChatSessionUpdateDTO {

    @NotBlank(message = "会话标题不能为空")
    @Size(max = 255, message = "会话标题不能超过255个字符")
    private String title;
}