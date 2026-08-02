package com.prompttraining.security;

import com.prompttraining.common.Constant;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * JWT 认证过滤器 - 对每个请求校验 JWT Token
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider jwtTokenProvider;
    private final UserDetailsServiceImpl userDetailsService;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String token = extractToken(request);

        if (StringUtils.hasText(token) && jwtTokenProvider.validateToken(token)) {
            String role = jwtTokenProvider.getRoleFromToken(token);
            UserPrincipal userPrincipal;

            if (Constant.ROLE_ADMIN.equals(role)) {
                // 管理员：不在 user 表中，直接构建 ADMIN 认证信息，跳过数据库查询
                userPrincipal = new UserPrincipal(
                        Constant.ADMIN_USER_ID,
                        jwtTokenProvider.getUsernameFromToken(token),
                        "",
                        true,
                        Constant.ROLE_ADMIN
                );
            } else {
                Long userId = jwtTokenProvider.getUserIdFromToken(token);
                userPrincipal = userDetailsService.loadUserById(userId);
            }

            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(userPrincipal, null, userPrincipal.getAuthorities());
            authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

            SecurityContextHolder.getContext().setAuthentication(authentication);
        }

        filterChain.doFilter(request, response);
    }

    /**
     * 从请求头中提取 Bearer Token
     */
    private String extractToken(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith(Constant.TOKEN_PREFIX)) {
            return bearerToken.substring(Constant.TOKEN_PREFIX.length());
        }
        return null;
    }
}
