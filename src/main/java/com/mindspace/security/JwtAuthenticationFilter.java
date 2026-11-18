package com.mindspace.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;

/*
* 改文件为过滤器 每一次进页都会去检查一次Authorization头 看看有没有token 有没有过期
* */


@Component
@RequiredArgsConstructor
//OncePerRequestFilter 保证每个请求只执行一次 声明过滤器
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    //注入jwt工具
    private final JwtUtil jwtUtil;

    @Override
    //拿到请求 然后拆分请求每一个部分 然后进行两个错误检查ServletException, IOException
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String header = request.getHeader("Authorization");
        //检查是否有token
        if (header != null && header.startsWith("Bearer ")) {
            //截取出token
            String token = header.substring(7);
            try {
                //解析token 然后取出来userId
                Long userId = jwtUtil.getUserId(token);
                UsernamePasswordAuthenticationToken authentication =
                        //创建一个认证对象 包含 id 凭证 和 权限列表
                        new UsernamePasswordAuthenticationToken(userId, null, Collections.emptyList());
                //加入客户端ip seesSionid
                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                //将认证对象 放入安全上下文
                SecurityContextHolder.getContext().setAuthentication(authentication);
                //清空上下文
            } catch (Exception e) {
                SecurityContextHolder.clearContext();
            }
        }

        //放行
        filterChain.doFilter(request, response);
    }
}