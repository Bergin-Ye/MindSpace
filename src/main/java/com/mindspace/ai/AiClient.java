package com.mindspace.ai;

/*
 * 这是一个接口
 * 他只干一件事情
 * 当你需要使用ai功能 请你依赖这个接口
 * 他会告诉你 你可以使用什么功能 需要传递什么参数 他自己不实现任何功能
 * 好处是当你需要换一个模型 或者 换一个ai服务 你不需要修改已有代码
 * 你只需要新建一个实现类 就可以了 不破坏老的代码
 * */

public interface AiClient {

    /**
     * 发一次对话请求，返回模型输出的原始文本。
     */
    String chat(String systemPrompt, String userPrompt);
}