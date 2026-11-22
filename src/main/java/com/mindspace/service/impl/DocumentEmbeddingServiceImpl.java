package com.mindspace.service.impl;


import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mindspace.ai.EmbeddingClient;
import com.mindspace.common.exception.BusinessException;
import com.mindspace.common.result.ResultCode;
import com.mindspace.document.TextChunker;
import com.mindspace.entity.Document;
import com.mindspace.entity.DocumentChunk;
import com.mindspace.entity.KnowledgeBase;
import com.mindspace.mapper.DocumentChunkMapper;
import com.mindspace.mapper.DocumentMapper;
import com.mindspace.mapper.KnowledgeBaseMapper;
import com.mindspace.service.DocumentEmbeddingService;
import com.mindspace.vo.EmbedResultVO;
import com.mindspace.vo.VectorSearchResultVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class DocumentEmbeddingServiceImpl implements DocumentEmbeddingService {

    //注入依赖
    private final DocumentMapper documentMapper;
    private final KnowledgeBaseMapper knowledgeBaseMapper;
    private final DocumentChunkMapper documentChunkMapper;
    private final EmbeddingClient embeddingClient;
    private final ObjectMapper objectMapper;

    //注入配置文件
    @Value("${embedding.chunk-size:500}")
    private int chunkSize;

    @Value("${embedding.chunk-overlap:100}")
    private int chunkOverlap;

    @Value("${embedding.max-chunks-per-document:200}")
    private int maxChunksPerDocument;

    @Value("${embedding.batch-size:16}")
    private int batchSize;

    @Value("${embedding.max-search-chunks:5000}")
    private int maxSearchChunks;


    @Override
    public EmbedResultVO embed(Long userId, Long id) {
        //获取当前时间戳
        long start = System.currentTimeMillis();
        //检验当前文档是否属于你 然后拿出来最新的文档内容
        Document doc = getOwned(userId, id);

        //如果当前文档解析状态不是成功那就抛出异常
        if (!"SUCCESS".equals(doc.getProcessStatus()) || !StringUtils.hasText(doc.getExtractedText())) {
            throw new BusinessException(ResultCode.DOCUMENT_NOT_PARSED);
        }

        //调用分块的那个方法 把tika解析出来的文本分块 分块大小 重叠大小 最大分块数 全部传过去
        //调用TextChunker类的split方法 分块返回一个字符串列表
        List<String> chunks = TextChunker.split(
                doc.getExtractedText(), chunkSize, chunkOverlap, maxChunksPerDocument
        );
        if (chunks.isEmpty()) {
            throw new BusinessException(ResultCode.DOCUMENT_NOT_PARSED, "文档没有可向量化的正文");
        }
        //定义一个List 里面是浮点型的数组 叫做vectors 然后先预分配分块的数量这么多的格子
        List<float[]> vectors = new ArrayList<>(chunks.size());

        //这里调用模型了 就是比如有100块 每次发10块给模型 然后存储下来
        //根据batchSize 来分批次 来遍历整体的分块列表
        for (int i = 0; i < chunks.size(); i += batchSize) {
            //算一下下一步位置 和 结束位置 不要超出最大范围
            int end = Math.min(i + batchSize, chunks.size());
            //调用embeddingClient接口 把当前批次丢给模型去向量化 然后这一批次结果存储到vectors列表中
            vectors.addAll(embeddingClient.embed(chunks.subList(i, end)));
        }
        //检验当前向量数量是否与分块数量一致
        if (vectors.size() != chunks.size()) {
            throw new BusinessException(ResultCode.EMBEDDING_RESPONSE_INVALID,
                    "向量数量与分块数量不一致");
        }
        //检验当前向量维度是否一致
        int dim = vectors.get(0).length;
        for (float[] vector : vectors) {
            if (vector.length != dim) {
                throw new BusinessException(ResultCode.EMBEDDING_RESPONSE_INVALID,
                        "不同分块的向量维度不一致");
            }
        }
        //根据文档id去数据库删掉对应id的向量
        documentChunkMapper.delete(new LambdaQueryWrapper<DocumentChunk>()
                .eq(DocumentChunk::getDocumentId, doc.getId()));

        //循环遍历 分块列表
        for (int i = 0; i < chunks.size(); i++) {
            //定义一个完整的DocumentChunk对象 里面包含文档id 用户id 知识库id 分块索引 内容 向量 向量维度 是需要存储进数据库的完全字段
            DocumentChunk chunk = new DocumentChunk();
            //前端传的
            chunk.setDocumentId(id);
            //前端传的
            chunk.setUserId(userId);
            //根据id userid 查出来的知识库id 也存进去
            chunk.setKnowledgeBaseId(doc.getKnowledgeBaseId());
            //分块索引
            chunk.setChunkIndex(i);
            //ai返回的分块内容 之前存在chunks列表中
            chunk.setContent(chunks.get(i));
            try {
                //把当前分块的向量 转换为字符串 存储到embedding字段中
                chunk.setEmbedding(objectMapper.writeValueAsString(vectors.get(i)));
            } catch (Exception e) {
                //如果序列化失败 则抛出异常
                throw new BusinessException(ResultCode.EMBEDDING_RESPONSE_INVALID,
                        "向量序列化失败");
            }
            //把处理好的向量 也存进数据库
            chunk.setEmbeddingDim(dim);
            //把封装好的chunk插入数据库
            documentChunkMapper.insert(chunk);
        }

        //返回一个EmbedResultVO对象
        EmbedResultVO vo = new EmbedResultVO();
        vo.setDocumentId(id);
        vo.setChunkCount(chunks.size());
        vo.setEmbeddingDim(dim);
        vo.setElapsedMs(System.currentTimeMillis() - start);
        //返回封装好的对象
        return vo;
    }

    /*
    *     /*
     * 向量语义检索
     * */
    @Override
    public List<VectorSearchResultVO> vectorSearch(Long userId, Long knowledgeBaseId, String query, int topK) {
        //检验这个查询的自然语言是不是空的
        if (!StringUtils.hasText(query)) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "查询内容不能为空");
        }
        //看下需要返回的相关文档数量是否在1-20之间
        if (topK < 1 || topK > 20) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "topK 必须在 1 到 20 之间");
        }
        //检验当前知识库是否存在
        checkKnowledgeBase(userId, knowledgeBaseId);
        //把用户输入的去掉首位然后调用embeddingClient接口中的embed方法 把用户输入的自然语言向量化
        //然后存进一个叫做queryVectors的列表中
        List<float[]> queryVectors = embeddingClient.embed(List.of(query.trim()));
        if (queryVectors.isEmpty()) {
            //如果返回的列表为空 则抛出异常 500 服务器内部错误
            throw new BusinessException(ResultCode.EMBEDDING_RESPONSE_INVALID, "查询向量为空");
        }
        //前端只传来一句话 所以转换成向量也只有一条啊 直接拿第零条存进queryVector数组中
        float[] queryVector = queryVectors.get(0);

        //初筛 只取你的 你的知识库 最多5000条文档的向量
        List<DocumentChunk> chunks = documentChunkMapper.selectForSearch(
                userId, knowledgeBaseId, maxSearchChunks);
        //向量空的 直接返回空列表
        if (chunks.isEmpty()) {
            return List.of();
        }
        //5000个分块 只需要知道他们出自哪些文档id就行 然后这些id存进documentIds
        Set<Long> documentIds = chunks.stream()
                //然后去查每一块的id
                .map(DocumentChunk::getDocumentId)
                //取出重复的id
                .collect(Collectors.toSet());
        //根据文档id去查他们原本的文档名字 然后存进nameMap中
        Map<Long, String> nameMap = documentMapper.selectByIds(documentIds).stream()
                .collect(Collectors.toMap(Document::getId, Document::getOriginalName));

        //定一个叫作result的列表 然后里面存的是数组
        List<VectorSearchResultVO> results = new ArrayList<>();
        //遍历所有的5000个分块
        for (DocumentChunk chunk : chunks) {
            try {
                //把json格式的向量 转换成数组 存进vector中
                float[] vector = objectMapper.readValue(chunk.getEmbedding(), float[].class);
                //如果向量的维度和查询向量的维度不一致 则跳过
                if (vector.length != queryVector.length) {
                    continue;
                }
                //计算一下余弦相似度 数学公式
                double score = cosine(queryVector, vector);
                //把计算好的余弦相似度 也存进vo中
                VectorSearchResultVO vo = new VectorSearchResultVO();
                //把当前分块的文档id 存进vo中
                vo.setDocumentId(chunk.getDocumentId());
                //把当前分块的文档名字 存进vo中
                vo.setOriginalName(nameMap.get(chunk.getDocumentId()));
                //把当前分块的id 存进vo中
                vo.setChunkId(chunk.getId());
                //把当前分块的索引 存进vo中
                vo.setChunkIndex(chunk.getChunkIndex());
                //把当前分块的内容 存进vo中
                vo.setContent(chunk.getContent());
                //把当前分块的余弦相似度四舍五入保留四位小数 存进vo中
                vo.setScore(Math.round(score * 10000.0) / 10000.0);
                results.add(vo);
                //如果解析失败 则跳过
            } catch (Exception e) {
                log.warn("解析 chunk 向量失败：chunkId={}", chunk.getId(), e);
            }
        }
        //
        //返回封装好的对象
        return results.stream()
                //现在有5000个vo 筛选根据Score从大到小排序 取前topK个
                .sorted(Comparator.comparing(VectorSearchResultVO::getScore).reversed())
                .limit(topK)
                .toList();

    }

    /*
     * ································工具类······························
     * */
    //检验当前文档是否属于你 然后拿出来最新的文档内容
    private Document getOwned(Long userId, Long id) {
        //根据userid id 去数据库查是否有匹配数据
        Document doc = documentMapper.selectOne(new LambdaQueryWrapper<Document>()
                .eq(Document::getId, id)
                .eq(Document::getUserId, userId));
        //如果是空的 则抛出异常 404 文件不存在
        if (doc == null) {
            throw new BusinessException(ResultCode.FILE_NOT_FOUND);
        }
        //查到了就返回
        return doc;
    }


    private void checkKnowledgeBase(Long userId, Long knowledgeBaseId) {
        Long count = knowledgeBaseMapper.selectCount(new LambdaQueryWrapper<KnowledgeBase>()
                .eq(KnowledgeBase::getId, knowledgeBaseId)
                .eq(KnowledgeBase::getUserId, userId));
        if (count == 0) {
            throw new BusinessException(ResultCode.KB_NOT_FOUND);
        }
    }

    //计算余弦相似度 数学公式
    private double cosine(float[] a, float[] b) {
        double dot = 0;
        double normA = 0;
        double normB = 0;
        for (int i = 0; i < a.length; i++) {
            dot += a[i] * b[i];
            normA += a[i] * a[i];
            normB += b[i] * b[i];
        }
        if (normA == 0 || normB == 0) {
            return 0;
        }
        return dot / (Math.sqrt(normA) * Math.sqrt(normB));
    }
}

