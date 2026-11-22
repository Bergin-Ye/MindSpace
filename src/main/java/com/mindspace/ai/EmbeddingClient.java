package com.mindspace.ai;

import java.util.List;

public interface EmbeddingClient {

    /**
     * 一批文本转成向量，返回顺序和输入一致。
     * 文本转换成向量接口
     */
    List<float[]> embed(List<String> texts);
}