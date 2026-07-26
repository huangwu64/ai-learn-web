package com.prompttraining.module.auth;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.prompttraining.common.BusinessException;
import com.prompttraining.module.auth.entity.dto.*;
import com.prompttraining.module.user.UserMapper;
import com.prompttraining.module.user.entity.User;
import com.prompttraining.security.JwtTokenProvider;
import com.prompttraining.security.RefreshToken;
import com.prompttraining.security.RefreshTokenMapper;
import com.prompttraining.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * 认证服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserMapper userMapper;
    private final RefreshTokenMapper refreshTokenMapper;
    private final JwtTokenProvider jwtTokenProvider;
    private final PasswordEncoder passwordEncoder;

    @Value("${jwt.access-token-expiration:7200}")
    private long accessTokenExpiration;

    @Value("${jwt.refresh-token-expiration:604800}")
    private long refreshTokenExpiration;

    /**
     * 用户注册
     */
    @Transactional
    public void register(RegisterRequest request) {
        // 检查用户名唯一性
        Long count = userMapper.selectCount(
                new LambdaQueryWrapper<User>().eq(User::getUsername, request.getUsername())
        );
        if (count > 0) {
            throw new BusinessException(400, "用户名已被占用");
        }

        User user = new User();
        user.setUsername(request.getUsername());
        user.setNickname(request.getNickname() != null && !request.getNickname().isBlank()
                ? request.getNickname() : request.getUsername());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setStatus(1);
        userMapper.insert(user);

        log.info("新用户注册成功: {}", user.getUsername());
    }

    /**
     * 用户登录
     */
    @Transactional
    public LoginResponse login(LoginRequest request) {
        User user = userMapper.selectOne(
                new LambdaQueryWrapper<User>().eq(User::getUsername, request.getUsername())
        );
        if (user == null) {
            throw new BusinessException(401, "用户名或密码错误");
        }

        // 检查账号状态
        if (user.getStatus() != null && user.getStatus() == 0) {
            throw new BusinessException(401, "账号已被禁用");
        }

        // 验证密码
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new BusinessException(401, "用户名或密码错误");
        }

        // 生成 Token
        String accessToken = jwtTokenProvider.generateAccessToken(user.getId(), user.getUsername());

        // 存储 Refresh Token
        RefreshToken refreshTokenEntity = new RefreshToken();
        refreshTokenEntity.setUserId(user.getId());
        refreshTokenEntity.setExpiresAt(LocalDateTime.now().plusSeconds(refreshTokenExpiration));
        refreshTokenEntity.setIsRevoked(0);
        refreshTokenMapper.insert(refreshTokenEntity);

        String refreshToken = jwtTokenProvider.generateRefreshToken(user.getId(), refreshTokenEntity.getId());
        refreshTokenEntity.setToken(refreshToken);
        refreshTokenMapper.updateById(refreshTokenEntity);

        // 更新最后登录时间
        user.setLastLoginAt(LocalDateTime.now());
        userMapper.updateById(user);

        log.info("用户登录成功: {}", user.getUsername());

        return LoginResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .expiresIn(accessTokenExpiration)
                .user(LoginResponse.UserInfo.builder()
                        .id(user.getId())
                        .username(user.getUsername())
                        .nickname(user.getNickname())
                        .avatarUrl(user.getAvatarUrl())
                        .build())
                .build();
    }

    /**
     * 刷新 Token
     */
    @Transactional
    public TokenRefreshResponse refreshToken(RefreshTokenRequest request) {
        String token = request.getRefreshToken();
        if (!jwtTokenProvider.validateToken(token)) {
            throw new BusinessException(401, "Refresh Token 无效或已过期，请重新登录");
        }

        Long tokenId = jwtTokenProvider.getTokenIdFromRefreshToken(token);
        RefreshToken refreshTokenEntity = refreshTokenMapper.selectById(tokenId);
        if (refreshTokenEntity == null || refreshTokenEntity.getIsRevoked() == 1) {
            throw new BusinessException(401, "Refresh Token 无效或已过期，请重新登录");
        }

        if (refreshTokenEntity.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new BusinessException(401, "Refresh Token 无效或已过期，请重新登录");
        }

        // 撤销旧的 Refresh Token
        refreshTokenEntity.setIsRevoked(1);
        refreshTokenMapper.updateById(refreshTokenEntity);

        // 生成新的 Token 对
        Long userId = refreshTokenEntity.getUserId();
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(401, "用户不存在");
        }

        String newAccessToken = jwtTokenProvider.generateAccessToken(userId, user.getUsername());

        // 创建新的 Refresh Token
        RefreshToken newRefreshTokenEntity = new RefreshToken();
        newRefreshTokenEntity.setUserId(userId);
        newRefreshTokenEntity.setExpiresAt(LocalDateTime.now().plusSeconds(refreshTokenExpiration));
        newRefreshTokenEntity.setIsRevoked(0);
        refreshTokenMapper.insert(newRefreshTokenEntity);

        String newRefreshToken = jwtTokenProvider.generateRefreshToken(userId, newRefreshTokenEntity.getId());
        newRefreshTokenEntity.setToken(newRefreshToken);
        refreshTokenMapper.updateById(newRefreshTokenEntity);

        return TokenRefreshResponse.builder()
                .accessToken(newAccessToken)
                .refreshToken(newRefreshToken)
                .expiresIn(accessTokenExpiration)
                .build();
    }

    /**
     * 退出登录 - 撤销该用户所有 Refresh Token
     */
    @Transactional
    public void logout() {
        Long userId = SecurityUtils.getCurrentUserId();
        if (userId == null) {
            return;
        }
        // 撤销该用户所有 Refresh Token
        refreshTokenMapper.selectList(
                new LambdaQueryWrapper<RefreshToken>()
                        .eq(RefreshToken::getUserId, userId)
                        .eq(RefreshToken::getIsRevoked, 0)
        ).forEach(rt -> {
            rt.setIsRevoked(1);
            refreshTokenMapper.updateById(rt);
        });
        log.info("用户退出登录，撤销所有 Refresh Token: userId={}", userId);
    }

    /**
     * 撤销某用户所有 Refresh Token（修改密码时调用）
     */
    public void revokeAllUserTokens(Long userId) {
        refreshTokenMapper.selectList(
                new LambdaQueryWrapper<RefreshToken>()
                        .eq(RefreshToken::getUserId, userId)
                        .eq(RefreshToken::getIsRevoked, 0)
        ).forEach(rt -> {
            rt.setIsRevoked(1);
            refreshTokenMapper.updateById(rt);
        });
    }
}
