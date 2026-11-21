package com.mindspace.task;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.mindspace.entity.Document;
import com.mindspace.mapper.DocumentMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/**
 * 一句话应用重启 就把所有还在解析中的文档改成fail 重新解析
 *
 * 启动恢复：
 * 应用重启后，残留的 PROCESSING 文档不可能再有线程在解析，
 * 统一标记为 FAILED，让用户重新提交。
 *
 * 注意：当前按单实例设计；多实例部署时不能这样全量恢复。
 */
@Slf4j
@Component
@RequiredArgsConstructor

/*
* Spring 容器初始化
  -> Bean 创建、依赖注入
  -> 线程池 initialize
  -> ApplicationRunner.run()   <- 这里执行恢复
  -> 应用完全启动，开始接收请求
* */
public class ParseTaskRecoveryRunner implements ApplicationRunner {
    //注入mapper
    private final DocumentMapper documentMapper;

    @Override
    public void run(ApplicationArguments args) {
        int rows = documentMapper.update(null, new LambdaUpdateWrapper<Document>()
                /*UPDATE document
                SET process_status = 'FAILED',
                error_message = '服务重启，解析任务中断，请重新解析'
                WHERE process_status = 'PROCESSING';*/
                .eq(Document::getProcessStatus, "PROCESSING")
                .set(Document::getProcessStatus, "FAILED")
                .set(Document::getErrorMessage, "服务重启，解析任务中断，请重新解析"));

        if (rows > 0) {
            log.warn("启动恢复：{} 个 PROCESSING 文档被标记为 FAILED", rows);
        }
    }
}