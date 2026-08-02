package com.prompttraining.module.admin;

import com.prompttraining.common.BusinessException;
import com.prompttraining.config.AdminConfig;
import com.prompttraining.module.admin.dto.AdminLoginRequest;
import com.prompttraining.module.admin.dto.AdminLoginResponse;
import com.prompttraining.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

/**
 * 管理员认证服务（V3）
 *
 * 账号密码从配置文件（admin.username / admin.password）读取，可修改。
 * 密码支持明文或 BCrypt 哈希（自动识别）。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AdminAuthService {

    private final AdminConfig adminConfig;
    private final JwtTokenProvider jwtTokenProvider;
    private final PasswordEncoder passwordEncoder;

    /**
     * 管理员登录，成功返回管理员 Token
     */
    public AdminLoginResponse login(AdminLoginRequest request) {
        if (request == null || !adminConfig.getUsername().equals(request.getUsername())) {
            throw new BusinessException(401, "管理员账号或密码错误");
        }
        if (!matchesPassword(request.getPassword(), adminConfig.getPassword())) {
            throw new BusinessException(401, "管理员账号或密码错误");
        }

        String token = jwtTokenProvider.generateAdminToken(adminConfig.getUsername(), adminConfig.getTokenExpiration());
        log.info("管理员登录成功: {}", adminConfig.getUsername());
        return new AdminLoginResponse(token, adminConfig.getTokenExpiration());
    }

    /**
     * 密码校验：BCrypt 哈希（$2a$/$2b$/$2y$ 开头）按哈希校验，否则按明文比较
     */
    private boolean matchesPassword(String rawPassword, String storedPassword) {
        if (rawPassword == null) return false;
        if (storedPassword == null) return false;
        if (storedPassword.startsWith("$2a$") || storedPassword.startsWith("$2b$") || storedPassword.startsWith("$2y$")) {
            return passwordEncoder.matches(rawPassword, storedPassword);
        }
        return rawPassword.equals(storedPassword);
    }
}
