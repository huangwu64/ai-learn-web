package com.prompttraining.module.user;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.prompttraining.module.user.entity.User;
import org.apache.ibatis.annotations.Mapper;

/**
 * 用户 Mapper - V1 仅用于读取默认用户
 */
@Mapper
public interface UserMapper extends BaseMapper<User> {
}
