package com.prompttraining.security;

import com.prompttraining.common.Constant;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

/**
 * 自定义 UserDetails，携带用户 ID 与角色（V3 新增角色）
 */
public class UserPrincipal implements UserDetails {

    @Getter
    private final Long userId;
    private final String username;
    private final String password;
    private final boolean enabled;
    @Getter
    private final String role;

    public UserPrincipal(Long userId, String username, String password, boolean enabled) {
        this(userId, username, password, enabled, Constant.ROLE_USER);
    }

    public UserPrincipal(Long userId, String username, String password, boolean enabled, String role) {
        this.userId = userId;
        this.username = username;
        this.password = password;
        this.enabled = enabled;
        this.role = role != null ? role : Constant.ROLE_USER;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return Constant.ROLE_ADMIN.equals(role)
                ? List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))
                : List.of(new SimpleGrantedAuthority("ROLE_USER"));
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getUsername() {
        return username;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }
}
