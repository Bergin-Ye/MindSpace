package com.mindspace.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mindspace.common.result.Result;
import com.mindspace.common.result.ResultCode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;

/*
* 这里就是当你被过滤器拦截了 验不过 我需要返回什么就从这里走
* */

@Component
@RequiredArgsConstructor
//声明自己是未验证处理器
public class RestAuthenticationEntryPoint implements AuthenticationEntryPoint {
    //注入objectMapper
    private final ObjectMapper objectMapper;

    @Override
    //验证失败时调用 去拿三个参数 然后抛一次异常
    public void commence(HttpServletRequest request,
                         HttpServletResponse response,
                         AuthenticationException authException) throws IOException {
        //设置状态码为200
        response.setStatus(HttpServletResponse.SC_OK);
        //告诉前端是json格式 utf-8
        response.setContentType("application/json;charset=UTF-8");
        //返回的字符串 包含错误码和错误信息
        objectMapper.writeValue(response.getWriter(), Result.error(ResultCode.UNAUTHORIZED));
    }
}