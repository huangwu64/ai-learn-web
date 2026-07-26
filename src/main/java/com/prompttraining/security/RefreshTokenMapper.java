package com.prompttraining.security;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * Refresh Token Mapper
 */
@Mapper
public interface RefreshTokenMapper extends BaseMapper<RefreshToken> {
}
