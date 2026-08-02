package com.prompttraining.module.admin;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.prompttraining.common.BusinessException;
import com.prompttraining.module.admin.dto.AdminCreateUserRequest;
import com.prompttraining.module.admin.dto.AdminResetPasswordRequest;
import com.prompttraining.module.admin.dto.AdminUpdateUserRequest;
import com.prompttraining.module.admin.dto.AdminUserResponse;
import com.prompttraining.module.message.MessageMapper;
import com.prompttraining.module.message.entity.Message;
import com.prompttraining.module.session.SessionMapper;
import com.prompttraining.module.session.entity.Session;
import com.prompttraining.module.user.UserMapper;
import com.prompttraining.module.user.entity.User;
import com.prompttraining.security.RefreshTokenMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 管理员用户管理服务（V3.2）
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserAdminService {

    /** 匿名用户 ID，禁止删除 */
    private static final long ANONYMOUS_USER_ID = 1L;

    private final UserMapper userMapper;
    private final SessionMapper sessionMapper;
    private final MessageMapper messageMapper;
    private final RefreshTokenMapper refreshTokenMapper;
    private final PasswordEncoder passwordEncoder;

    /**
     * 用户列表
     */
    public List<AdminUserResponse> listUsers() {
        return userMapper.selectList(
                new LambdaQueryWrapper<User>().orderByAsc(User::getId)
        ).stream().map(this::toResponse).collect(Collectors.toList());
    }

    /**
     * 管理员创建用户
     */
    @Transactional
    public AdminUserResponse createUser(AdminCreateUserRequest req) {
        if (req.getUsername() == null || req.getUsername().isBlank()) {
            throw new BusinessException(400, "用户名不能为空");
        }
        if (req.getPassword() == null || req.getPassword().isBlank()) {
            throw new BusinessException(400, "密码不能为空");
        }
        String username = req.getUsername().trim();
        Long count = userMapper.selectCount(
                new LambdaQueryWrapper<User>().eq(User::getUsername, username)
        );
        if (count > 0) {
            throw new BusinessException(400, "用户名已被占用");
        }

        User user = new User();
        user.setUsername(username);
        user.setNickname(req.getNickname() != null && !req.getNickname().isBlank()
                ? req.getNickname().trim() : username);
        user.setPassword(passwordEncoder.encode(req.getPassword()));
        user.setStatus(1);
        userMapper.insert(user);
        log.info("管理员创建用户: {}", username);
        return toResponse(user);
    }

    /**
     * 管理员删除用户（级联清理会话/消息/Refresh Token）
     */
    @Transactional
    public void deleteUser(Long id) {
        if (id == null || id == ANONYMOUS_USER_ID) {
            throw new BusinessException(400, "该用户不可删除");
        }
        User user = userMapper.selectById(id);
        if (user == null) {
            throw new BusinessException(404, "用户不存在");
        }

        // 软删除该用户所有会话
        LambdaQueryWrapper<Session> sessionWrapper = new LambdaQueryWrapper<>();
        sessionWrapper.eq(Session::getUserId, id);
        List<Session> sessions = sessionMapper.selectList(sessionWrapper);
        for (Session s : sessions) {
            sessionMapper.deleteById(s.getId());
            LambdaQueryWrapper<Message> msgWrapper = new LambdaQueryWrapper<>();
            msgWrapper.eq(Message::getSessionId, s.getId());
            messageMapper.delete(msgWrapper);
        }
        // 清理 Refresh Token
        refreshTokenMapper.delete(
                new LambdaQueryWrapper<com.prompttraining.security.RefreshToken>()
                        .eq(com.prompttraining.security.RefreshToken::getUserId, id)
        );
        // 删除用户
        userMapper.deleteById(id);
        log.info("管理员删除用户: id={}, username={}", id, user.getUsername());
    }

    /**
     * 管理员更新用户信息（直接生效，无需审核）
     */
    @Transactional
    public AdminUserResponse updateUser(Long id, AdminUpdateUserRequest req) {
        User user = getExisting(id);
        if (req.getNickname() != null && !req.getNickname().isBlank()) {
            if (req.getNickname().length() > 64) throw new BusinessException(400, "昵称最长64位");
            user.setNickname(req.getNickname().trim());
        }
        if (req.getAvatarUrl() != null && !req.getAvatarUrl().isBlank()) {
            if (req.getAvatarUrl().length() > 255) throw new BusinessException(400, "头像地址过长");
            user.setAvatarUrl(req.getAvatarUrl().trim());
        }
        if (req.getStatus() != null) {
            if (req.getStatus() != 0 && req.getStatus() != 1) throw new BusinessException(400, "状态值不合法");
            user.setStatus(req.getStatus());
        }
        userMapper.updateById(user);
        return toResponse(user);
    }

    /**
     * 管理员重置用户密码
     */
    @Transactional
    public void resetPassword(Long id, AdminResetPasswordRequest req) {
        if (req.getPassword() == null || req.getPassword().isBlank()) {
            throw new BusinessException(400, "新密码不能为空");
        }
        User user = getExisting(id);
        user.setPassword(passwordEncoder.encode(req.getPassword()));
        userMapper.updateById(user);
        // 撤销该用户所有 Refresh Token，强制重新登录
        refreshTokenMapper.delete(
                new LambdaQueryWrapper<com.prompttraining.security.RefreshToken>()
                        .eq(com.prompttraining.security.RefreshToken::getUserId, id)
        );
        log.info("管理员重置用户密码: id={}", id);
    }

    private User getExisting(Long id) {
        User user = userMapper.selectById(id);
        if (user == null) {
            throw new BusinessException(404, "用户不存在");
        }
        return user;
    }

    private AdminUserResponse toResponse(User user) {
        AdminUserResponse resp = new AdminUserResponse();
        resp.setId(user.getId());
        resp.setUsername(user.getUsername());
        resp.setNickname(user.getNickname());
        resp.setAvatarUrl(user.getAvatarUrl());
        resp.setStatus(user.getStatus());
        resp.setCreatedAt(user.getCreatedAt());
        resp.setLastLoginAt(user.getLastLoginAt());
        return resp;
    }
}
