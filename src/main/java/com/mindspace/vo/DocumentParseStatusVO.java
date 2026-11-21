package com.mindspace.vo;


import com.mindspace.entity.Document;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class DocumentParseStatusVO {

    private static final int PREVIEW_LENGTH = 200;

    private Long id;
    private String processStatus;
    private String errorMessage;
    private Long extractedTextLength;
    private String textPreview;
    private LocalDateTime updateTime;

    public static DocumentParseStatusVO from(Document doc) {
        DocumentParseStatusVO vo = new DocumentParseStatusVO();
        vo.setId(doc.getId());
        vo.setProcessStatus(doc.getProcessStatus());
        vo.setErrorMessage(doc.getErrorMessage());
        vo.setUpdateTime(doc.getUpdateTime());

        //定义一个text 用于存储提取的extractedTextLength
        String text = doc.getExtractedText();
        //判断文本是否为空
        if (text != null) {
            //存储提取的文本长度
            vo.setExtractedTextLength((long) text.length());
            //存储提取的文本预览
            vo.setTextPreview(text.length() > PREVIEW_LENGTH
                    ? text.substring(0, PREVIEW_LENGTH) + "..."
                    : text);
        }
        //返回vo
        return vo;
    }


}
