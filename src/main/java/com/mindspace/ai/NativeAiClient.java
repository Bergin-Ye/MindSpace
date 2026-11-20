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
import java.util.List;
import java.util.Map;

/**
 * 原生 RestClient 版本，保留对照学习。
 *
 * 当前不注册为 Bean。
 * 想切回原生版本：
 * 1. 取消下面 @Component 的注释；
 * 2. 注释掉 SpringAiClient 的 @Component；
 * 3. 保证 ai.base-url / ai.api-key / ai.model 配置存在。
 */
// @Component
@Slf4j
public class NativeAiClient implements AiClient {

    private final RestClient restClient;
    private final String apiKey;
    private final String model;

    public NativeAiClient(@Value("${ai.base-url}") String baseUrl,
                          @Value("${ai.api-key:}") String apiKey,
                          @Value("${ai.model}") String model,
                          @Value("${ai.timeout-seconds:60}") int timeoutSeconds) {
        this.apiKey = apiKey;
        this.model = model;

        // 原生版本的 base-url 带 /v1，这里不再拼 /v1
        String normalizedBaseUrl = baseUrl.endsWith("/")
                ? baseUrl.substring(0, baseUrl.length() - 1)
                : baseUrl;

        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
        JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(httpClient);
        factory.setReadTimeout(Duration.ofSeconds(timeoutSeconds));

        this.restClient = RestClient.builder()
                .baseUrl(normalizedBaseUrl)
                .requestFactory(factory)
                .build();
    }

    @Override
    public String chat(String systemPrompt, String userPrompt) {
        if (!StringUtils.hasText(apiKey) || !StringUtils.hasText(model)) {
            throw new BusinessException(ResultCode.AI_NOT_CONFIGURED);
        }

        Map<String, Object> body = Map.of(
                "model", model,
                "messages", List.of(
                        Map.of("role", "system", "content", systemPrompt),
                        Map.of("role", "user", "content", userPrompt)
                ),
                "temperature", 0.2
        );

        try {
            JsonNode response = restClient.post()
                    .uri("/chat/completions")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(JsonNode.class);

            if (response == null) {
                throw new BusinessException(ResultCode.AI_RESPONSE_INVALID, "AI 返回为空");
            }
            if (response.has("error")) {
                log.error("AI 接口返回错误：{}", response.path("error"));
                throw new BusinessException(ResultCode.AI_SERVICE_ERROR, "AI 服务返回错误");
            }

            String content = response.path("choices")
                    .path(0)
                    .path("message")
                    .path("content")
                    .asText("");

            if (!StringUtils.hasText(content)) {
                throw new BusinessException(ResultCode.AI_RESPONSE_INVALID, "AI 返回内容为空");
            }
            return content;
        } catch (BusinessException e) {
            throw e;
        } catch (RestClientResponseException e) {
            log.error("AI 接口返回异常：status={}, body={}",
                    e.getStatusCode(), e.getResponseBodyAsString());
            throw new BusinessException(ResultCode.AI_SERVICE_ERROR,
                    "AI 服务返回 " + e.getStatusCode());
        } catch (Exception e) {
            log.error("调用 AI 服务失败", e);
            throw new BusinessException(ResultCode.AI_SERVICE_ERROR);
        }
    }
}