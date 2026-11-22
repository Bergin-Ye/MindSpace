package com.mindspace.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.mindspace.common.exception.BusinessException;
import com.mindspace.common.result.ResultCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

@Slf4j
@Component

/*
* 我们要把之前的Tika弄出来的文本 发给远端的ai服务 让他返回向量回来
* 这里是学习笔记 这里的ai和我之前所认为的LLMai完全不是一个东西
* 是这样的 我们本次做的是什么
* 就是当你把切好块的文本 按照需求的格式发给向量模型
* 然后向量模型会返回类似于这样的{ "model": "bge-m3", "input": ["苹果", "香蕉"] }东西
* 他是一个1024维度的向量
* 我们需要存储下来 用于后续的相似度计算
* 具体的计算过程是这样的 这个所谓的向量 你可以理解为是二维空间中的一个点
* 然后你后续搜索同样需要把你的输入提示词也变成向量
* 然后通过向量之间的夹角来计算相似度
* 然后去找最相似的 也就是你最需要的文案片段来返回这就是rag知识库
* 我们目前向量模型有两种阿里云百炼送我的300块调用或者咱就ollma本地部署一个小的向量模型
* */

public class OpenAiCompatibleEmbeddingClient implements EmbeddingClient {

    private final RestClient restClient;
    private final String apiKey;
    private final String model;

    //默认spring配置文件注入参数 冒号后面的是默认参数
    public OpenAiCompatibleEmbeddingClient(
            @Value("${embedding.base-url:http://localhost:11434/v1}") String baseUrl,
            @Value("${embedding.api-key:ollama}") String apiKey,
            @Value("${embedding.model:bge-m3}") String model,
            @Value("${embedding.timeout-seconds:60}") int timeoutSeconds) {
        this.apiKey = apiKey;
        this.model = model;

        //防御性处理：配置里尾部多写了个 / 会导致拼 URI 变成 //embeddings。这段把尾部斜杠去掉，保证拼出来的地址是干净的。
        String normalized = baseUrl.endsWith("/")
                ? baseUrl.substring(0, baseUrl.length() - 1)
                : baseUrl;

        /*
        *用了jdk自带的java.net.http.HttpClient 然后用了spring的JdkClientHttpRequestFactory
        *把他包装成RestClient 能用的"请求工厂
        * 因为默认的RestTemplate 超时时间是40s，我们想设置60s 默认工厂不给你设置时间
        * */
        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
        JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(httpClient);
        factory.setReadTimeout(Duration.ofSeconds(timeoutSeconds));

        this.restClient = RestClient.builder()
                .baseUrl(normalized)
                .requestFactory(factory)
                .build();
    }

    @Override
    //实现向量化接口
    public List<float[]> embed(List<String> texts) {
        //没配 key/model 直接抛"服务未配置"业务异常
        if (!StringUtils.hasText(apiKey) || !StringUtils.hasText(model)) {
            throw new BusinessException(ResultCode.EMBEDDING_NOT_CONFIGURED);
        }
        //空输入直接返回空列表 不浪费http请求
        if (texts == null || texts.isEmpty()) {
            return List.of();
        }
        //OpenAI 兼容接口的请求格式，就俩字段
        Map<String, Object> body = Map.of(
                "model", model,
                "input", texts
        );

        /*实际长这样
        * POST http://localhost:11434/v1/embeddings
        * Authorization: Bearer ollama
        * Content-Type: application/json
        * {"model":"bge-m3","input":["Spring Boot 自动配置","..."]}
        * */
        //发请求
        try {
            //发一个post请求
            JsonNode response = restClient.post()
                    //发url地址 把/embeddings拼接到baseUrl后面
                    .uri("/embeddings")
                    //加 Bearer token，OpenAI 协议要求的
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                    //告诉服务器我发的是 JSON
                    .contentType(MediaType.APPLICATION_JSON)
                    //把 Map 序列化成 JSON 发出去（Spring 自动用 Jackson）
                    .body(body)
                    //触发实际请求
                    .retrieve()
                    // 把响应 JSON 解析成 Jackson 的 JsonNode（树模型）
                    .body(JsonNode.class);

            /*
            * 这里的思路是不相信ai返回的响应
            * 必须自己校验 并且全部拦下来转换成自己的业务异常
            * */

            //如果响应的是空
            if (response == null) {
                //抛出业务异常
                throw new BusinessException(ResultCode.EMBEDDING_RESPONSE_INVALID, "向量化返回为空");
            }
            //如果服务端返回了error
            if (response.has("error")) {
                //打印错误信息
                log.error("向量化接口返回错误：{}", response.path("error"));
                //抛出业务异常
                throw new BusinessException(ResultCode.EMBEDDING_SERVICE_ERROR, "向量化服务返回错误");
            }

            //如果返回的data不是数组或者数量不一致 那么及时响应格式问题
            JsonNode data = response.path("data");
            if (!data.isArray() || data.size() != texts.size()) {
                //抛出业务异常
                throw new BusinessException(ResultCode.EMBEDDING_RESPONSE_INVALID,
                        "向量数量与输入数量不一致");
            }
            //这里是关键部分
            /*
            * 就是你把文本块发给你的向量模型
            * 他会返回索引和这个向量
            * 由于一些不可控原因 你发给他文本块的顺序可能是 abc 但是 返回可能是cba
            * 你需要index这个索引来匹配你的输入顺序（index由模型返回）
            * 然后呢index 0 也就是 a 需要存储他的向量 相面我们就在干这件事情
            * */
            //我们需要一个二维数组来存储每个文本块的向量
            //size就是我发给模型的文本块数量 也就是外层二维数组大小
            float[][] vectors = new float[texts.size()][];
            //我们遍历返回的data数组
            for (int i = 0; i < data.size(); i++) {
                //然后获取每一项ai返回的
                JsonNode item = data.get(i);
                //获取ai返回的当前项的索引
                int index = item.path("index").asInt(i);
                //如果索引超出范围 或者 索引超出了最大范围 或者 索引对应的向量已经存在了
                if (index < 0 || index >= texts.size() || vectors[index] != null) {
                    //抛出业务异常
                    throw new BusinessException(ResultCode.EMBEDDING_RESPONSE_INVALID,
                            "向量索引不合法");
                }

                //这里是在验货 看下你返回的向量是否为空
                JsonNode embedding = item.path("embedding");
                //如果向量不是数组 或者 向量是空的
                if (!embedding.isArray() || embedding.isEmpty()) {
                    //抛出业务异常
                    throw new BusinessException(ResultCode.EMBEDDING_RESPONSE_INVALID,
                            "向量数据为空");
                }

                //把向量转换成数组 new了1024个float
                float[] vector = new float[embedding.size()];
                //遍历所有向量元素 从0-1023
                for (int j = 0; j < embedding.size(); j++) {
                    //把对应的分量转换成float类型 然后赋值给vector数组的第j个元素
                    vector[j] = (float) embedding.get(j).asDouble();
                }
                //把存储好的向量（vector数组）赋值给二维数组的第index行
                vectors[index] = vector;
            }

            //遍历二维数组的vector
            for (float[] vector : vectors) {
                //如果向量为空
                if (vector == null) {
                    //抛出业务异常
                    throw new BusinessException(ResultCode.EMBEDDING_RESPONSE_INVALID,
                            "向量数量不足");
                }
            }
            //返回向量列表
            return Arrays.asList(vectors);

            //抛出业务异常
        } catch (BusinessException e) {
            throw e;
        } catch (RestClientResponseException e) {
            log.error("向量化接口返回异常：status={}, body={}",
                    e.getStatusCode(), e.getResponseBodyAsString());
            throw new BusinessException(ResultCode.EMBEDDING_SERVICE_ERROR,
                    "向量化服务返回 " + e.getStatusCode());
        } catch (Exception e) {
            log.error("调用向量化服务失败", e);
            throw new BusinessException(ResultCode.EMBEDDING_SERVICE_ERROR);
        }
    }
}