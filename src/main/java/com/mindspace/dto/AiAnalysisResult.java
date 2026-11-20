package com.mindspace.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * 这个玩意就是你把上面开发的tika解析出来的东西发给了ai
 * ai不是要返回东西给你吗
 * 你需要让他塞进这个json对象里面
 * 一句话 让ai 按照这个格式返回
 */
@Data
public class AiAnalysisResult {

    private String summary;
    private List<String> keywords;
    private String category;
    private BigDecimal confidence;
}