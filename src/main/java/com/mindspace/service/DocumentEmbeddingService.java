package com.mindspace.service;

import com.mindspace.vo.EmbedResultVO;
import com.mindspace.vo.VectorSearchResultVO;

import java.util.List;

public interface DocumentEmbeddingService {

    /*
    * 文本分块+向量化接口
    * */
    EmbedResultVO embed(Long userId, Long id);

    /*
    * 向量语义检索
    * */
    List<VectorSearchResultVO> vectorSearch(Long userId, Long knowledgeBaseId, String query, int topK);
}
