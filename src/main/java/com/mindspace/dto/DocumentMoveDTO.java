package com.mindspace.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class DocumentMoveDTO {

    @NotNull(message = "文件夹ID不能为空")
    private Long folderId;
}