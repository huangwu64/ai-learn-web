package com.prompttraining.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.prompttraining.common.Result;
import com.prompttraining.security.JwtAuthenticationFilter;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * Spring Security 配置 - V2 JWT 认证模式
 *
 * V2 引入 JWT 认证体系：
 * - /api/v1/auth/** 放行（登录、注册、刷新 Token）
 * - /api/v1/** 其他接口需认证
 * - Swagger/Knife4j 文档放行
 */
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final ObjectMapper objectMapper;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            // 禁用 CSRF（前后端分离，使用 Token 认证）
            .csrf(AbstractHttpConfigurer::disable)
            // 无状态会话
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            // V2 JWT 认证模式
            .authorizeHttpRequests(auth -> auth
                // 认证接口放行（注册、登录、刷新 Token 无需认证）
                .requestMatchers(HttpMethod.POST, "/api/v1/auth/register").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/v1/auth/login").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/v1/auth/refresh").permitAll()
                // V3：管理员登录放行 + 公开接口放行
                .requestMatchers(HttpMethod.POST, "/api/v1/admin/auth/login").permitAll()
                .requestMatchers("/api/v1/public/**").permitAll()
                // V3：管理员接口需要 ADMIN 角色（需放在 /api/v1/** 之前）
                .requestMatchers("/api/v1/admin/**").hasRole("ADMIN")
                // Swagger/Knife4j 放行
                .requestMatchers("/swagger-ui/**", "/v3/api-docs/**", "/doc.html").permitAll()
                // 错误页面放行
                .requestMatchers("/error").permitAll()
                // OPTIONS 预检请求放行
                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                // 其他 API 需要认证（包括 /api/v1/auth/logout）
                .requestMatchers("/api/v1/**").authenticated()
                .anyRequest().permitAll()
            )
            // 添加 JWT 过滤器（在 UsernamePasswordAuthenticationFilter 之前）
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
            // 禁用表单登录
            .formLogin(AbstractHttpConfigurer::disable)
            .httpBasic(AbstractHttpConfigurer::disable)
            // 自定义 401/403 响应（返回 JSON）
            .exceptionHandling(ex -> ex
                .authenticationEntryPoint((request, response, authException) ->
                    writeJsonResponse(response, 401, "未登录或 Token 已过期，请重新登录"))
                .accessDeniedHandler((request, response, accessDeniedException) ->
                    writeJsonResponse(response, 403, "无权访问该资源"))
            );

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * 向响应写入 JSON 错误信息
     */
    private void writeJsonResponse(HttpServletResponse response, int code, String message) throws IOException {
        response.setStatus(HttpServletResponse.SC_OK); // 业务码用 200，通过 body 区分错误
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        Result<?> result = Result.fail(code, message);
        response.getWriter().write(objectMapper.writeValueAsString(result));
    }
}
