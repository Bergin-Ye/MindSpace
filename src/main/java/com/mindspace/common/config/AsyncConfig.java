package com.mindspace.common.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.ThreadPoolExecutor;

/**
 * 异步解析线程池。
 * 这里不写 @EnableAsync，因为我们直接用 ThreadPoolTaskExecutor.execute 提交任务。
 */
//这是一个配置类
@Configuration
public class AsyncConfig {

    //注册成一个bean 启动的时候交给spring管理 名字叫做documentParseExecutor
    @Bean("documentParseExecutor")
    public ThreadPoolTaskExecutor documentParseExecutor(
            //从 application.yaml 读取配置项，然后赋值过来
            @Value("${parse.pool.core-size:2}") int coreSize,
            @Value("${parse.pool.max-size:4}") int maxSize,
            @Value("${parse.pool.queue-capacity:50}") int queueCapacity) {

        //创建一个新的线程池
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        //上面applicating弄出来的配置赋值进去 线程池的核心数量
        executor.setCorePoolSize(coreSize);
        //最大线程数
        executor.setMaxPoolSize(maxSize);
        //队列容量
        executor.setQueueCapacity(queueCapacity);
        //线程名前缀
        executor.setThreadNamePrefix("parse-");
        // 拒绝策略：队列满 + 最大线程忙时直接拒绝，由 Service 捕获并置 FAILED
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.AbortPolicy());
        // 项目关闭时等解析任务执行完，最多等 60 秒
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(60);
        return executor;
    }
}