package com.mindspace.dto;


import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class KnowledgeBaseDTO {
    @NotBlank(message = "名称不能为空")
    @Size(min = 1, max = 100, message = "名称最多100个字符")
    private String name;

    @Size(max = 500, message = "描述最多500个字符")
    private String description;
}
