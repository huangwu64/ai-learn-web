package com.prompttraining.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 管理员配置 - 对应 application.yml 中 admin 配置段
 *
 * 管理员账号密码在配置文件中可修改，不硬编码在代码中。
 * 密码支持两种形式（自动识别）：
 *   - 明文密码（直接写配置文件，便于修改）
 *   - BCrypt 哈希（以 $2a$ / $2b$ / $2y$ 开头时按哈希校验）
 */
@Data
@Component
@ConfigurationProperties(prefix = "admin")
public class AdminConfig {

    /** 管理员账号 */
    private String username = "admin";

    /** 管理员密码（明文或 BCrypt 哈希） */
    private String password = "123456";

    /** 管理员前端入口路径（可配置替换的特殊 URL） */
    private String entryPath = "/admin";

    /** 管理员 Token 有效期（秒），默认 12 小时 */
    private long tokenExpiration = 43200;
}
