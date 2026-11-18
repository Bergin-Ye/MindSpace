package com.mindspace.security;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Spring Security 总配置。
 *
 * 职责：
 * 1. 关闭与 JWT 无关的默认安全功能（CSRF、表单登录、HTTP Basic、Session）
 * 2. 定义哪些接口免登录（白名单），哪些接口必须登录
 * 3. 把自定义的 JwtAuthenticationFilter 插入安全过滤器链
 * 4. 把 RestAuthenticationEntryPoint 注册为"未认证时的响应处理器"
 *
 * 注意：本类只负责"装配"，不处理请求。
 * 真正的 token 解析在 JwtAuthenticationFilter，
 * 未认证时的 JSON 响应在 RestAuthenticationEntryPoint。
 */
@Configuration              // 声明配置类，Spring 启动时加载其中的 @Bean
@EnableWebSecurity          // 开启 Spring Security 的 Web 安全支持，漏了这个整套安全配置不生效
@RequiredArgsConstructor    // Lombok 为 final 字段生成构造器，用于注入下面两个 Bean
public class SecurityConfig {

    /** JWT 认证过滤器：每个请求验 token、设 SecurityContext */
    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    /** 未认证处理器：请求需要登录但未登录时，由它返回统一 JSON */
    private final RestAuthenticationEntryPoint restAuthenticationEntryPoint;

    /**
     * 构建安全过滤器链。
     * Spring Security 启动时会找到这个 Bean，用它作为安全体系的核心配置。
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // ① 关闭 CSRF
                //    CSRF 依赖 Cookie/Session，JWT 的 token 放在 Authorization 头里，
                //    浏览器不会自动携带，不存在跨站伪造风险，关闭即可。
                .csrf(AbstractHttpConfigurer::disable)

                // ② 关闭默认表单登录页
                //    Spring Security 默认会生成 /login 的 HTML 页面，
                //    我们用自己的 /api/auth/login 接口，不需要它。
                .formLogin(AbstractHttpConfigurer::disable)

                // ③ 关闭 HTTP Basic 认证
                //    避免未认证请求收到 WWW-Authenticate: Basic 导致浏览器弹框，
                //    我们的认证方案是 Bearer JWT，不是 Basic。
                .httpBasic(AbstractHttpConfigurer::disable)

                // ④ 无状态 Session
                //    STATELESS = 不创建、不使用 HttpSession。
                //    JWT 本身就是无状态认证，服务端不存登录状态，
                //    多实例部署也不会有 session 不共享的问题。
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                // ⑤ 授权规则
                //    匹配顺序：从上到下，先匹配到的生效。
                //    所以白名单必须写在 anyRequest 之前，否则白名单会失效。
                .authorizeHttpRequests(auth -> auth

                        // 白名单：注册、登录接口不需要 token
                        // （用户还没登录，不可能带 token）
                        .requestMatchers("/api/auth/register", "/api/auth/login").permitAll()

                        // 除白名单外，其他所有请求都必须已认证
                        // 包括 /api/auth/me、后续所有业务接口
                        .anyRequest().authenticated())

                // ⑥ 注册未认证处理器
                //    当请求需要认证但 SecurityContext 为空时，
                //    由 restAuthenticationEntryPoint 返回 { "code": 401, ... }。
                //    不配这行的话，Spring 默认返回 HTML 或空 401，前端拿不到统一 JSON。
                .exceptionHandling(exception ->
                        exception.authenticationEntryPoint(restAuthenticationEntryPoint))

                // ⑦ 把 JWT 过滤器插入安全链
                //    插在 UsernamePasswordAuthenticationFilter 之前，
                //    保证它在授权检查之前执行，先设置好 SecurityContext。
                //    如果插在授权检查之后，Security 检查时上下文是空的，所有请求都会被判未认证。
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        // ⑧ 构建并返回过滤器链，不调 build() 配置不生效
        return http.build();
    }
}