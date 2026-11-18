package com.mindspace.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class FolderCreateDTO {
    @NotNull(message = "知识库ID不能为空")
    private  Long knowledgeBaseId;

    private  Long parentId;

    @NotBlank(message = "名称不能为空")
    @Size(min = 1, max = 100, message = "名称最多100个字符")
    private  String name;

}
