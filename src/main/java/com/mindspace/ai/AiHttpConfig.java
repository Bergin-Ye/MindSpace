package com.mindspace.ai;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.net.http.HttpClient;
import java.time.Duration;

/**
 * 给 Spring AI 的 OpenAI starter 提供带超时的 RestClient.Builder。
 * 不定义这个 Bean 时，Spring AI 会退回默认 RestClient，读超时不明确。
 *
 * 它的使用不发生在你的代码里，而发生在 IoC 容器里。
 * 启动项目时被放入 IoC 容器里面
 * springAi 会自动使用这个配置
 */
@Configuration
public class AiHttpConfig {

    @Bean
    @Scope("prototype")
    //创建一个方法restClientBuilder 返回一个RestClient.Builder
    public RestClient.Builder restClientBuilder(
            //从application.yml去读取超时时间 没读到就是默认60s 然后存进timeoutSeconds变量
            @Value("${ai.timeout-seconds:60}") int timeoutSeconds) {

        //创建一个httpClient对象
        //然后用newBuilder()来配置这个httpClient对象
        //设置连接超时时间为10s 超过放弃放弃连接
        //把配置加入并且返回给 httpClient 这样别人就可以调用httpClient对象了
        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();

        //把jdk的请求包装成spring的请求工厂
        JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(httpClient);
        //给这个请求设置超时时间 timeoutSeconds秒 超过放弃放弃读取
        factory.setReadTimeout(Duration.ofSeconds(timeoutSeconds));

        //返回一个RestClient.Builder对象
        return RestClient.builder().requestFactory(factory);
    }
}