package com.prompttraining.module.user;

import com.prompttraining.common.BusinessException;
import com.prompttraining.module.auth.AuthService;
import com.prompttraining.module.user.entity.User;
import com.prompttraining.module.user.entity.dto.ChangePasswordRequest;
import com.prompttraining.module.user.entity.dto.UpdateUserRequest;
import com.prompttraining.module.user.entity.dto.UserInfoResponse;
import com.prompttraining.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 用户服务 - V2 完整实现
 * 支持注册、登录、JWT Token 管理、个人信息管理
 */
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final AuthService authService;

    /**
     * 获取当前登录用户信息
     */
    public UserInfoResponse getCurrentUser() {
        Long userId = SecurityUtils.getCurrentUserId();
        if (userId == null) {
            throw new BusinessException(401, "未登录");
        }
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(404, "用户不存在");
        }
        return toResponse(user);
    }

    /**
     * 更新当前用户信息（昵称、头像）
     */
    @Transactional
    public UserInfoResponse updateUser(UpdateUserRequest request) {
        if ((request.getNickname() == null || request.getNickname().isBlank())
                && (request.getAvatarUrl() == null || request.getAvatarUrl().isBlank())) {
            throw new BusinessException(400, "至少需要提供 nickname 或 avatarUrl 其中之一");
        }

        Long userId = SecurityUtils.getCurrentUserId();
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(404, "用户不存在");
        }

        if (request.getNickname() != null && !request.getNickname().isBlank()) {
            if (request.getNickname().length() > 64) {
                throw new BusinessException(400, "昵称最长64位");
            }
            user.setNickname(request.getNickname());
        }
        if (request.getAvatarUrl() != null && !request.getAvatarUrl().isBlank()) {
            if (request.getAvatarUrl().length() > 255) {
                throw new BusinessException(400, "头像URL最长255位");
            }
            user.setAvatarUrl(request.getAvatarUrl());
        }

        userMapper.updateById(user);
        return toResponse(user);
    }

    /**
     * 修改密码
     */
    @Transactional
    public void changePassword(ChangePasswordRequest request) {
        Long userId = SecurityUtils.getCurrentUserId();
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(404, "用户不存在");
        }

        // 验证旧密码
        if (!passwordEncoder.matches(request.getOldPassword(), user.getPassword())) {
            throw new BusinessException(400, "旧密码不正确");
        }

        // 更新密码
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userMapper.updateById(user);

        // 撤销所有 Refresh Token，强制重新登录
        authService.revokeAllUserTokens(userId);
    }

    /**
     * 根据用户名查找用户
     */
    public User findByUsername(String username) {
        return userMapper.selectOne(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<User>()
                        .eq(User::getUsername, username)
        );
    }

    private UserInfoResponse toResponse(User user) {
        return UserInfoResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .nickname(user.getNickname())
                .avatarUrl(user.getAvatarUrl())
                .createdAt(user.getCreatedAt())
                .lastLoginAt(user.getLastLoginAt())
                .build();
    }
}
