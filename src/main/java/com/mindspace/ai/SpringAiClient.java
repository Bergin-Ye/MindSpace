package com.mindspace.ai;

import com.mindspace.common.exception.BusinessException;
import com.mindspace.common.result.ResultCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;



@Slf4j
@Component
public class SpringAiClient implements AiClient {

    //这里只是占位符dummy
    private static final String DUMMY_API_KEY = "dummy";
    // 真正干活的底层组件（由 Spring AI 提供），用来组装和发送大模型请求
    private final ChatClient chatClient;
    // 保存从配置文件里读出来的 API Key，用于在真正发请求前做一次安全校验
    private final String apiKey;

    /**
     * 构造方法（依赖注入）
     *
     * 【为什么要写构造方法？】
     * 1. ChatClient.Builder builder：这是 Spring AI 自动配置提供的构建器 他就是自动接收参数。
     *    还记得你前面写的 AiHttpConfig 吗？那里配置的超时时间（RestClient.Builder），
     *    最终就是通过这个 builder 传递到当前这个 chatClient 里的！这就是 Bean 的传递。
     * 2. @Value("${spring.ai.openai.api-key:}")：从 application.yml 里读取 API Key。
     *    冒号 ": " 的意思是：如果配置文件里没写，默认给个空字符串，防止启动报错。
     */

    public SpringAiClient(ChatClient.Builder builder,
                          //去application.yml读apikey读不到就是空
                          @Value("${spring.ai.openai.api-key:}") String apiKey) {
        // 构建出真正的对话客户端。builder 里已经包含了配置好的超时 HTTP 工厂。
        this.chatClient = builder.build();
        this.apiKey = apiKey;
    }

    /**
     * 真正执行对话的核心方法（重写了 AiClient 接口的方法）
     */
    @Override
    public String chat(String systemPrompt, String userPrompt) {
        // 如果你没配置apikey 是空串 null 或者全是空格 或者等于dummy占位符 抛出异常
        if (!StringUtils.hasText(apiKey) || DUMMY_API_KEY.equals(apiKey)) {
            throw new BusinessException(ResultCode.AI_NOT_CONFIGURED);
        }

        try {
            // 【核心调用链：Spring AI 的精髓】
            // chatClient.prompt()：开启一次提示词构建
            // .system(systemPrompt)：设置“系统人设”（比如：你是一个文档分析助手，请只输出JSON）
            // .user(userPrompt)：设置“用户输入”（比如：Tika 提取出来的那一大段文本）
            // .call()：真正发起 HTTP 网络请求，这个阶段会消耗时间（也就是为什么需要前面配置超时）
            // .content()：从大模型返回的复杂 JSON 结构里，直接把“纯文本内容”抠出来给你
            String content = chatClient.prompt()
                    .system(systemPrompt)
                    .user(userPrompt)
                    //这里发过去了！！！！！！！！
                    .call()
                    //返回的
                    .content();

            // 【第二道防线：防空返回】
            // 有时候 AI 接口通了，但由于触发了内容审查、或者模型抽风，会返回空白内容。
            // 如果你不拦截，后面把它当做 JSON 去解析成 AiAnalysisResult 时，代码必崩。
            if (!StringUtils.hasText(content)) {
                throw new BusinessException(ResultCode.AI_RESPONSE_INVALID, "AI 返回内容为空");
            }
            return content;

        } catch (BusinessException e) {
            // 【异常处理第一层】
            // 如果抛出的是我们自己业务层定义的异常（比如上面防空返回抛出的），直接原样抛出。
            // 不要被下面的 Exception 捕获了，否则前端收到的错误码就不对了。
            throw e;
        } catch (Exception e) {
            // 【异常处理第二层：兜底防线】
            // 如果网络断了、超时了、或者 AI 官方接口报 502 了，会走到这里。
            // log.error：把真实的、详细的报错信息（包含堆栈）打印到你的后台日志里，方便你排查 bug。
            log.error("Spring AI 调用失败", e);
            // throw：对外（前端）则统一抛出 3001 AI 服务调用失败。
            // 这样就不会把底层的网络错误暴露给前端用户。
            throw new BusinessException(ResultCode.AI_SERVICE_ERROR);
        }
    }
}