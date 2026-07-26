package com.prompttraining.security;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.prompttraining.module.user.UserMapper;
import com.prompttraining.module.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

/**
 * Spring Security UserDetailsService 实现
 */
@Service
@RequiredArgsConstructor
public class UserDetailsServiceImpl implements UserDetailsService {

    private final UserMapper userMapper;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userMapper.selectOne(
                new LambdaQueryWrapper<User>().eq(User::getUsername, username)
        );
        if (user == null) {
            throw new UsernameNotFoundException("用户不存在: " + username);
        }
        return toUserPrincipal(user);
    }

    /**
     * 根据用户 ID 加载 UserDetails
     */
    public UserPrincipal loadUserById(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new UsernameNotFoundException("用户不存在: " + userId);
        }
        return toUserPrincipal(user);
    }

    private UserPrincipal toUserPrincipal(User user) {
        return new UserPrincipal(
                user.getId(),
                user.getUsername(),
                user.getPassword() != null ? user.getPassword() : "",
                user.getStatus() == null || user.getStatus() == 1
        );
    }
}
