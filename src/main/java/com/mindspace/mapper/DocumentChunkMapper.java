package com.mindspace.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mindspace.entity.DocumentChunk;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

public interface DocumentChunkMapper extends BaseMapper<DocumentChunk> {

    @Select("""
            SELECT c.*
            FROM document_chunk c
            JOIN document d ON c.document_id = d.id
            WHERE c.user_id = #{userId}
              AND c.knowledge_base_id = #{knowledgeBaseId}
              AND d.deleted = 0
              AND d.process_status = 'SUCCESS'
            ORDER BY c.id
            LIMIT #{limit}
            """)
    List<DocumentChunk> selectForSearch(@Param("userId") Long userId,
                                        @Param("knowledgeBaseId") Long knowledgeBaseId,
                                        @Param("limit") int limit);
}